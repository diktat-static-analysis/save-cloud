package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.setenv
import com.saveourtool.save.agent.utils.updateFromEnv
import com.saveourtool.save.core.config.LogType
import com.saveourtool.save.core.logging.logType
import com.saveourtool.save.reporter.Report
import com.saveourtool.common.utils.fs
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES")
open class SaveAgentTest {
    init {
        setenv(com.saveourtool.common.agent.AgentEnvName.CONTAINER_ID.name, "agent-for-test")
        setenv(com.saveourtool.common.agent.AgentEnvName.CONTAINER_NAME.name, "save-agent-for-test")
        setenv(com.saveourtool.common.agent.AgentEnvName.HEARTBEAT_URL.name, HEARTBEAT_ENDPOINT.toLocalhostUrl())
        setenv(com.saveourtool.common.agent.AgentEnvName.CLI_COMMAND.name, "echo Doing nothing it test mode")
        setenv(com.saveourtool.common.agent.AgentEnvName.EXECUTION_ID.name, "1")
    }

    @Suppress("MAGIC_NUMBER", "MagicNumber")
    private val tmpDir: okio.Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
        .resolve("save-agent-test-${Random.nextInt(100, 999)}")
    private val reportFile = tmpDir / "save.out.json"
    private val logsFile = tmpDir / "logs.txt"

    private val configuration: AgentConfiguration by lazy {
        AgentConfiguration.initializeFromEnv()
            .updateFromEnv()
            .let {
                it.copy(
                    logFilePath = logsFile.toString(),
                    save = it.save.copy(
                        reportDir = tmpDir.toString()
                    )
                )
            }
    }

    private val saveAgentForTest: SaveAgent by lazy {
        SaveAgent(configuration, httpClient = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
            engine {
                addHandler { request ->
                    when (request.url.encodedPath) {
                        HEARTBEAT_ENDPOINT -> respond(
                            json.encodeToString(PolymorphicSerializer(com.saveourtool.common.agent.HeartbeatResponse::class),
                                com.saveourtool.common.agent.ContinueResponse
                            ),
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                        EXECUTION_DATA_ENDPOINT -> respond(
                            "executionData",
                            HttpStatusCode.OK,
                        )
                        DEBUG_INFO_ENDPOINT -> respond(
                            "-1",
                            HttpStatusCode.OK,
                        )
                        else -> error("Unhandled ${request.url}")
                    }
                }
            }
        }, coroutineScope = CoroutineScope(Dispatchers.Default))
    }

    @BeforeTest
    fun setUp() {
        logType.set(LogType.ALL)
        val report = Report(
            testSuite = "Doing nothing it test mode",
            pluginExecutions = emptyList(),
        )
        fs.createDirectory(tmpDir)
        fs.write(reportFile, true) {
            writeUtf8(Json.encodeToString(listOf(report)))
        }
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }

    @Test
    fun `agent should send heartbeats`() {
        runBlocking {
            saveAgentForTest.sendHeartbeat(com.saveourtool.common.agent.ExecutionProgress(0, -1L))
        }
    }

    @Test
    fun `should change state to FINISHED after SAVE CLI returns`() = runBlocking {
        assertEquals(com.saveourtool.common.agent.AgentState.BUSY, saveAgentForTest.state.get())
        runBlocking {
            saveAgentForTest.run {
                startSaveProcess(
                    com.saveourtool.common.agent.AgentRunConfig(
                        cliArgs = "",
                        executionDataUploadUrl = EXECUTION_DATA_ENDPOINT.toLocalhostUrl(),
                        debugInfoUploadUrl = DEBUG_INFO_ENDPOINT.toLocalhostUrl()
                    )
                )
            }
        }
        assertEquals(com.saveourtool.common.agent.AgentState.FINISHED, saveAgentForTest.state.get())
    }

    private fun String.toLocalhostUrl() = "http://localhost$this"

    companion object {
        private const val HEARTBEAT_ENDPOINT = "/heartbeat"
        private const val EXECUTION_DATA_ENDPOINT = "/executionData"
        private const val DEBUG_INFO_ENDPOINT = "/debugInfo"
    }
}
