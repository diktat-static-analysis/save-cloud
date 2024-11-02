@file:OptIn(ExperimentalCoroutinesApi::class)

package com.saveourtool.save.agent

import com.saveourtool.common.agent.*
import com.saveourtool.common.domain.TestResultDebugInfo
import com.saveourtool.common.utils.*
import com.saveourtool.save.agent.utils.*
import com.saveourtool.save.agent.utils.processRequestToBackend
import com.saveourtool.save.core.config.resolveSaveOverridesTomlConfig
import com.saveourtool.save.core.files.getWorkingDirectory
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.logging.logTrace
import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.result.CountWarnings
import com.saveourtool.save.core.utils.ExecutionResult
import com.saveourtool.save.core.utils.ProcessBuilder
import com.saveourtool.save.core.utils.runIf
import com.saveourtool.save.plugins.fix.FixPlugin
import com.saveourtool.save.reporter.Report

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * A main class for SAVE Agent
 *
 * @param config
 * @param coroutineScope a [CoroutineScope] to launch other jobs
 * @property httpClient
 */
@Suppress("AVOID_NULL_CHECKS")
class SaveAgent(
    private val config: AgentConfiguration,
    internal val httpClient: HttpClient,
    private val coroutineScope: CoroutineScope,
) {
    /**
     * The current [AgentState] of this agent. Initial value corresponds to the period when agent needs to finish its configuration.
     */
    val state = createGenericAtomicReference(AgentState.BUSY)

    // fixme (limitation of old MM): can't use atomic reference to Instant here, because when using `Clock.System.now()` as an assigned value
    // Kotlin throws `kotlin.native.concurrent.InvalidMutabilityException: mutation attempt of frozen kotlinx.datetime.Instant...`
    private val executionStartSeconds = createAtomicLong(0L)
    private val saveProcessJob: GenericAtomicReference<Job?> = createGenericAtomicReference(null)
    private val backgroundContext = newSingleThreadContext("background")
    private val saveProcessContext = newSingleThreadContext("save-process")
    private val reportFormat = Json {
        serializersModule = SerializersModule {
            polymorphic(Plugin.TestFiles::class) {
                subclass(Plugin.Test::class)
                subclass(FixPlugin.FixTestFiles::class)
            }
        }
    }

    /**
     * Starts save-agent and required jobs in the background and then immediately returns
     *
     * @return a descriptor of the main coroutine job
     */
    fun start(): Job {
        logInfoCustom("Starting agent")
        state.set(AgentState.STARTING)
        return coroutineScope.launch { startHeartbeats(this) }
    }

    /**
     * Stop this agent by cancelling all jobs on [coroutineScope].
     * [coroutineScope] is the topmost scope for all jobs, so by cancelling it
     * we can gracefully shut down the whole application.
     */
    internal fun shutdown() {
        coroutineScope.cancel()
    }

    // a temporary workaround for python integration
    private fun executeAdditionallySetup(
        additionalFileNames: Collection<String>,
        setupShTimeoutMillis: Long,
    ) = runCatching {
        logDebugCustom("Will execute additionally setup of evaluated tool if it's required")
        additionalFileNames
            .singleOrNull { it == "setup.sh" }
            ?.let { fileName ->
                logDebugCustom("Additionally setup of evaluated tool by $fileName")
                val setupResult = ProcessBuilder(true, fs)
                    .exec(
                        "./$fileName",
                        // setup.sh should always be run from test-suites dir
                        TEST_SUITES_DIR_NAME,  // <- cd test-suites/
                        null,
                        setupShTimeoutMillis,
                    )  // < - cd test-suites && ./setup.sh
                if (setupResult.code != 0) {
                    throw IllegalStateException("$fileName} is failed with error: ${setupResult.stderr}")
                }
                logTrace("$fileName is executed successfully. Output: ${setupResult.stdout}")
            }
        logInfoCustom("Additionally setup has completed")
    }

    // prepare save-overrides.toml based on config.save.*
    private fun prepareSaveOverridesToml(saveCliOverrides: SaveCliOverrides, targetDirectory: Path) = runCatching {
        logDebugCustom("Will create `save-overrides.toml` if it's required")
        with(saveCliOverrides) {
            val generalConfig = buildMap {
                overrideExecCmd?.let { put("execCmd", it) }
                batchSize?.let { put("batchSize", it) }
                batchSeparator?.let { put("batchSeparator", it) }
            }.map { it.toTomlLine() }
            val fixAndWarnConfigs = buildMap {
                overrideExecFlags?.let { put("execFlags", it) }
            }.map { it.toTomlLine() }
            val saveOverridesTomlContent = buildString {
                if (generalConfig.isNotEmpty()) {
                    appendLine("[general]")
                    generalConfig.forEach { appendLine(it) }
                }
                if (fixAndWarnConfigs.isNotEmpty()) {
                    appendLine("[fix]")
                    fixAndWarnConfigs.forEach { appendLine(it) }
                    appendLine("[warn]")
                    fixAndWarnConfigs.forEach { appendLine(it) }
                }
            }
            if (saveOverridesTomlContent.isNotEmpty()) {
                fs.write(targetDirectory.resolveSaveOverridesTomlConfig(), true) {
                    writeUtf8(saveOverridesTomlContent)
                }
            }
        }
        logInfoCustom("Created `save-overrides.toml` based on configuration provided by orchestrator")
    }

    private fun Map.Entry<String, Any>.toTomlLine() = when (value) {
        is String -> "$key = \"$value\""
        else -> "$key = $value"
    }

    @Suppress("WHEN_WITHOUT_ELSE")  // when with sealed class
    private suspend fun startHeartbeats(coroutineScope: CoroutineScope) {
        logInfoCustom("Scheduling heartbeats")
        while (true) {
            val response = runCatching {
                // TODO: get execution progress here. However, with current implementation JSON report won't be valid until all tests are finished.
                sendHeartbeat(ExecutionProgress(executionId = requiredEnv(AgentEnvName.EXECUTION_ID.name).toLong(), percentCompletion = 0))
            }
            if (response.isSuccess) {
                when (val heartbeatResponse = response.getOrThrow().also {
                    logDebugCustom("Got heartbeat response $it")
                }) {
                    is InitResponse -> coroutineScope.launch(backgroundContext) {
                        initAgent(heartbeatResponse.config)
                    }
                    is NewJobResponse -> coroutineScope.launch {
                        maybeStartSaveProcess(heartbeatResponse.config)
                    }
                    is WaitResponse -> state.set(AgentState.IDLE)
                    is ContinueResponse -> Unit  // do nothing
                    is TerminateResponse -> shutdown()
                }
            } else {
                logErrorCustom("Exception during heartbeat: ${response.exceptionOrNull()?.message}")
                response.exceptionOrNull()?.printStackTrace()
            }
            // todo: start waiting after request was sent, not after response?
            logInfoCustom("Waiting for ${config.heartbeat.intervalMillis} ms")
            delay(config.heartbeat.intervalMillis)
        }
    }

    private suspend fun initAgent(agentInitConfig: AgentInitConfig) {
        state.set(AgentState.BUSY)

        downloadSaveCli(agentInitConfig.saveCliUrl)

        val targetDirectory = config.testSuitesDir.toPath()
        downloadTestResources(agentInitConfig.testSuitesSourceSnapshotUrl, targetDirectory)
            .runIf(failureResultPredicate) {
                logErrorAndSetCrashed {
                    "Unable to download tests from ${agentInitConfig.testSuitesSourceSnapshotUrl}"
                }
                return@initAgent
            }

        downloadAdditionalResources(targetDirectory, agentInitConfig.additionalFileNameToUrl)
            .runIf(
                failureResultPredicate
            ) {
                logErrorAndSetCrashed {
                    "Unable to download resources based on map [${agentInitConfig.additionalFileNameToUrl}]"
                }
                return@initAgent
            }

        // a temporary workaround for python integration
        executeAdditionallySetup(agentInitConfig.additionalFileNameToUrl.keys, agentInitConfig.setupShTimeoutMillis)
            .runIf(
                failureResultPredicate
            ) {
                logErrorAndSetCrashed {
                    "Unable to execute additionally setup"
                }
                return@initAgent
            }

        prepareSaveOverridesToml(agentInitConfig.saveCliOverrides, targetDirectory)
            .runIf(failureResultPredicate) {
                logErrorAndSetCrashed {
                    "Unable to prepare `save-overrides.toml`"
                }
                return@initAgent
            }
        state.set(AgentState.IDLE)
    }

    private fun CoroutineScope.maybeStartSaveProcess(config: AgentRunConfig) {
        if (saveProcessJob.get()?.isCompleted == false) {
            logErrorCustom("Shouldn't start new process when there is the previous running")
        } else {
            saveProcessJob.set(launch(saveProcessContext) {
                runCatching {
                    // new job received from Orchestrator, spawning SAVE CLI process
                    startSaveProcess(config)
                }
                    .exceptionOrNull()
                    ?.let {
                        state.set(AgentState.CLI_FAILED)
                        logErrorCustom("Error executing SAVE: ${it.describe()}\n${it.stackTraceToString()}")
                    }
            })
        }
    }

    /**
     * @param runConfig configuration to run SAVE process
     */
    internal fun CoroutineScope.startSaveProcess(runConfig: AgentRunConfig) {
        // blocking execution of OS process
        state.set(AgentState.BUSY)
        executionStartSeconds.set(Clock.System.now().epochSeconds)
        logInfoCustom("Starting SAVE in ${getWorkingDirectory()} with provided args ${runConfig.cliArgs}")
        val executionResult = runSave(runConfig.cliArgs)
        logInfoCustom("SAVE has completed execution with status ${executionResult.code}")

        val saveCliLogFilePath = config.logFilePath
        val saveCliLogData = fs.source(saveCliLogFilePath.toPath())
            .buffer()
            .use {
                String(it.readByteArray()).split("\n")
            }
        logDebugCustom("SAVE has completed execution, execution logs:")
        saveCliLogData.forEach {
            logDebugCustom("[SAVE] $it")
        }

        when (executionResult.code) {
            0 -> if (saveCliLogData.isEmpty()) {
                state.set(AgentState.CLI_FAILED)
            } else {
                handleSuccessfulExit(runConfig).invokeOnCompletion { cause ->
                    state.set(if (cause == null) AgentState.FINISHED else AgentState.CRASHED)
                }
            }
            else -> {
                logErrorCustom("SAVE has exited abnormally with status ${executionResult.code}")
                state.set(AgentState.CLI_FAILED)
            }
        }
    }

    private fun runSave(cliArgs: String): ExecutionResult {
        val fullCliCommand = buildString {
            append(config.cliCommand)
            append(" ${config.testSuitesDir}")
            append(" $cliArgs")
            with(config.save) {
                append(" --report-type ${reportType.name.lowercase()}")
                append(" --result-output ${resultOutput.name.lowercase()}")
                append(" --report-dir $reportDir")
                append(" --log ${logType.name.lowercase()}")
            }
        }
        return ProcessBuilder(true, fs)
            .exec(
                fullCliCommand,
                "",
                config.logFilePath.toPath(),
                SAVE_CLI_TIMEOUT
            )
    }

    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "TYPE_ALIAS")
    private fun readExecutionResults(jsonFile: String): Pair<List<TestResultDebugInfo>, List<TestExecutionResult>> {
        val currentTime = Clock.System.now()
        val reports: List<Report> = readExecutionReportFromFile(jsonFile)
        return reports.flatMap { report ->
            report.pluginExecutions.flatMap { pluginExecution ->
                pluginExecution.testResults.map { tr ->
                    val debugInfo = tr.toTestResultDebugInfo(report.testSuite, pluginExecution.plugin)
                    val testResultStatus = tr.status.toTestResultStatus()
                    debugInfo to TestExecutionResult(
                        filePath = tr.resources.test.toString(),
                        pluginName = pluginExecution.plugin,
                        agentContainerId = config.info.containerId,
                        agentContainerName = config.info.containerName,
                        status = testResultStatus,
                        startTimeSeconds = executionStartSeconds.get(),
                        endTimeSeconds = currentTime.epochSeconds,
                        unmatched = debugInfo.getCountWarningsAsLong { it.unmatched },
                        matched = debugInfo.getCountWarningsAsLong { it.matched },
                        expected = debugInfo.getCountWarningsAsLong { it.expected },
                        unexpected = debugInfo.getCountWarningsAsLong { it.unexpected },
                    )
                }
            }
        }
            .unzip()
    }

    private fun TestResultDebugInfo.getCountWarningsAsLong(getter: (CountWarnings) -> Int?) = this.debugInfo
        ?.countWarnings
        ?.let { getter(it) }
        ?.toLong()

    private fun readExecutionReportFromFile(jsonFile: String): List<Report> {
        val jsonFileContent = readFile(jsonFile).joinToString(separator = "")
        return if (jsonFileContent.isEmpty()) {
            throw IllegalStateException("Reading results file $jsonFile has returned empty")
        } else {
            reportFormat.decodeFromString(
                jsonFileContent
            )
        }
    }

    private fun CoroutineScope.handleSuccessfulExit(runConfig: AgentRunConfig): Job {
        val jsonReport = "${config.save.reportDir}/save.out.json"
        val result = runCatching {
            readExecutionResults(jsonReport)
        }
        return launch(backgroundContext) {
            if (result.isFailure) {
                val cause = result.exceptionOrNull()
                logErrorCustom(
                    "Couldn't read execution results from JSON report, reason: ${cause?.describe()}" +
                            "\n${cause?.stackTraceToString()}"
                )
            } else {
                val (debugInfos, testExecutionDtos) = result.getOrThrow()
                processRequestToBackend {
                    postExecutionData(runConfig.executionDataUploadUrl, testExecutionDtos)
                }
                debugInfos.forEach { debugInfo ->
                    logDebugCustom("Posting debug info for test ${debugInfo.testResultLocation}")
                    processRequestToBackend {
                        sendReport(runConfig.debugInfoUploadUrl, debugInfo)
                    }
                }
            }
        }
    }

    /**
     * @param executionProgress execution progress that will be sent in a heartbeat message
     * @return a [HeartbeatResponse] from Orchestrator
     */
    internal suspend fun sendHeartbeat(executionProgress: ExecutionProgress): HeartbeatResponse {
        logDebugCustom("Sending heartbeat to ${config.heartbeat.url}")
        // if current state is IDLE or FINISHED, should accept new jobs as a response
        return httpClient.post {
            url(config.heartbeat.url)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(Heartbeat(config.info, state.get(), executionProgress))
        }
            .body()
    }

    private suspend fun postExecutionData(executionDataUploadUrl: String, testExecutionResults: List<TestExecutionResult>) = httpClient.post {
        logInfoCustom("Posting execution data to backend, ${testExecutionResults.size} test executions")
        url(executionDataUploadUrl)
        contentType(ContentType.Application.Json)
        setBody(testExecutionResults)
    }

    private suspend fun sendReport(debugInfoUploadUrl: String, testResultDebugInfo: TestResultDebugInfo) = httpClient.post {
        url(debugInfoUploadUrl)
        contentType(ContentType.Application.Json)
        setBody(testResultDebugInfo)
    }

    private fun Result<*>.logErrorAndSetCrashed(errorMessage: () -> String) {
        logErrorCustom("${errorMessage()}: ${exceptionOrNull()?.describe()}")
        state.set(AgentState.CRASHED)
    }

    companion object {
        private const val SAVE_CLI_TIMEOUT = 1_000_000L
        private val failureResultPredicate: Result<*>.() -> Boolean = { isFailure }
    }
}
