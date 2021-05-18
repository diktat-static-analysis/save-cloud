@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

import org.cqfn.save.agent.utils.readFile
import org.cqfn.save.core.utils.ExecutionResult
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.domain.TestResultStatus

import generated.SAVE_CORE_VERSION
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import okio.ExperimentalFileSystem
import okio.Path.Companion.toPath

import kotlin.native.concurrent.AtomicReference
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

typealias requestExecutionData = suspend (testExecutionDto: TestExecutionDto) -> HttpResponse
typealias requestAdditionalData = suspend () -> HttpResponse

/**
 * A main class for SAVE Agent
 */
@OptIn(KtorExperimentalAPI::class, ExperimentalFileSystem::class)
class SaveAgent(private val config: AgentConfiguration,
                private val httpClient: HttpClient = HttpClient {
                    install(JsonFeature) {
                        serializer = KotlinxSerializer(Json {
                            serializersModule = SerializersModule {
                                // for some reason for K/N it's needed explicitly, at least for ktor 1.5.1, kotlin 1.4.21
                                contextual(HeartbeatResponse::class, HeartbeatResponse.serializer())
                            }
                        })
                    }
                    install(HttpTimeout) {
                        requestTimeoutMillis = config.requestTimeoutMillis
                    }
                }
) {
    /**
     * The current [AgentState] of this agent
     */
    val state = AtomicReference(AgentState.IDLE)
    private var saveProcessJob: Job? = null

    /**
     * @return Unit
     */
    suspend fun start() = coroutineScope {
        println("Starting agent")
        val heartbeatsJob = launch { startHeartbeats() }
        heartbeatsJob.join()
    }

    @Suppress("WHEN_WITHOUT_ELSE")  // when with sealed class
    private suspend fun startHeartbeats() = coroutineScope {
        println("Scheduling heartbeats")
        runCatching {
            saveAdditionalData()
        }
        while (true) {
            val response = runCatching {
                // TODO: get execution progress here
                sendHeartbeat(ExecutionProgress(0))
            }
            if (response.isSuccess) {
                when (response.getOrNull()) {
                    is NewJobResponse -> maybeStartSaveProcess()
                    WaitResponse -> state.value = AgentState.IDLE
                    ContinueResponse -> Unit  // do nothing
                }
            } else {
                println("Exception during heartbeat: ${response.exceptionOrNull()?.message}")
            }
            // todo: start waiting after request was sent, not after response?
            println("Waiting for ${config.heartbeat.interval} sec")
            delay(config.heartbeat.interval)
        }
    }

    private suspend fun maybeStartSaveProcess() = coroutineScope {
        if (saveProcessJob?.isCompleted == false) {
            println("Shouldn't start new process when there is the previous running")
        } else {
            saveProcessJob = launch {
                // new job received from Orchestrator, spawning SAVE CLI process
                startSaveProcess()
            }
        }
    }

    /**
     * @return Unit
     */
    @Suppress("MAGIC_NUMBER")  // todo: unsuppress when mocked data is substituted by actual
    internal suspend fun startSaveProcess() = coroutineScope {
        // blocking execution of OS process
        state.value = AgentState.BUSY
        val executionResult = runSave(emptyList())
        when (executionResult.code) {
            0 -> {
                val executionLogs = ExecutionLogs(config.id, readFile("logs.txt"))
                if (executionLogs.cliLogs.isEmpty()) {
                    state.value = AgentState.CLI_FAILED
                    return@coroutineScope
                }
                // todo: parse test executions from files
                val currentTime = Clock.System.now().toEpochMilliseconds()
                val testExecutionDtoExample = TestExecutionDto(0L, 0L, TestResultStatus.PASSED, currentTime, currentTime)
                sendAdditionalDataToBackend(::postExecutionData, ::saveAdditionalData, testExecutionDtoExample)
                runCatching {
                    sendLogs(executionLogs)
                }
                    .exceptionOrNull()
                    ?.let {
                        println("Couldn't send logs, reason: ${it.message}")
                    }
                state.value = AgentState.FINISHED
            }
            else -> {
                println("SAVE has exited abnormally with status ${executionResult.code}")
                state.value = AgentState.CLI_FAILED
            }
        }
    }

    private fun runSave(cliArgs: List<String>): ExecutionResult =
            ProcessBuilder().exec(config.cliCommand, "logs.txt".toPath())

    /**
     * @param executionLogs logs of CLI execution progress that will be sent in a message
     */
    private suspend fun sendLogs(executionLogs: ExecutionLogs) = httpClient.post<HttpResponse> {
        url("${config.orchestratorUrl}/executionLogs")
        contentType(ContentType.Application.Json)
        body = executionLogs
    }

    /**
     * @param executionProgress execution progress that will be sent in a heartbeat message
     * @return a [HeartbeatResponse] from Orchestrator
     */
    internal suspend fun sendHeartbeat(executionProgress: ExecutionProgress): HeartbeatResponse {
        // log.trace("Sending heartbeat to $orchestratorUrl")
        // if current state is IDLE or FINISHED, should accept new jobs as a response
        return httpClient.post("${config.orchestratorUrl}/heartbeat") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            body = Heartbeat(config.id, state.value, executionProgress)
        }
    }

    /**
     * Attempt to send execution data to backend, will retry several times, while increasing delay 2 times on each iteration.
     */
    private suspend fun sendAdditionalDataToBackend(
        sendExecution: requestExecutionData,
        sendAdditional: requestAdditionalData,
        testExecutionDto: TestExecutionDto
    ) = coroutineScope {
        var retryInterval = config.executionDataInitialRetryMillis
        repeat(config.executionDataRetryAttempts) { attempt ->
            val result = runCatching {
                sendExecution(testExecutionDto)
                sendAdditional()
            }
            if (result.isSuccess && result.getOrNull()?.status == HttpStatusCode.OK) {
                return@repeat
            } else {
                val reason = if (result.isSuccess && result.getOrNull()?.status != HttpStatusCode.OK) {
                    state.value = AgentState.BACKEND_FAILURE
                    "Backend returned status ${result.getOrNull()?.status}"
                } else {
                    state.value = AgentState.BACKEND_UNREACHABLE
                    "Backend is unreachable, ${result.exceptionOrNull()?.message}"
                }
                println("Cannot post execution data (x$attempt), will retry in $retryInterval second. Reason: $reason")
                delay(retryInterval)
                retryInterval *= 2
            }
        }
    }

    private suspend fun postExecutionData(testExecutionDto: TestExecutionDto) = httpClient.post<HttpResponse> {
        url("${config.backendUrl}/executionData")
        contentType(ContentType.Application.Json)
        body = testExecutionDto
    }

    private suspend fun saveAdditionalData() = httpClient.post<HttpResponse> {
        url("${config.backendUrl}/saveAgentVersion")
        contentType(ContentType.Application.Json)
        body = AgentVersion(config.id, SAVE_CORE_VERSION)
    }
}
