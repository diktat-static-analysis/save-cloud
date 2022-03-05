package org.cqfn.save.orchestrator.controller.heartbeat

import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.ExecutionProgress
import org.cqfn.save.agent.Heartbeat
import org.cqfn.save.agent.NewJobResponse
import org.cqfn.save.entities.AgentStatusDto
import org.cqfn.save.entities.AgentStatusesForExecution
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.orchestrator.config.Beans
import org.cqfn.save.orchestrator.service.AgentService
import org.cqfn.save.orchestrator.service.DockerService
import org.cqfn.save.test.TestBatch
import org.cqfn.save.test.TestDto
import org.cqfn.save.testsuite.TestSuiteType

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono

import java.nio.charset.Charset
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@WebFluxTest
@Import(Beans::class, AgentService::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HeartbeatControllerTest {
    @Autowired lateinit var webClient: WebTestClient
    @Autowired private lateinit var agentService: AgentService
    @MockBean private lateinit var dockerService: DockerService
    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun webClientSetUp() {
        webClient.mutate().responseTimeout(Duration.ofSeconds(2)).build()
    }

    @AfterEach
    fun tearDown() {
        mockServer.dispatcher.peek().let { mockResponse ->
            assertTrue(mockResponse.getBody().let { it == null || it.size == 0L }) {
                "There is an enqueued response in the MockServer after a test has completed. Enqueued body: ${mockResponse.getBody()?.readString(Charset.defaultCharset())}"
            }
        }
    }

    @Test
    fun checkAcceptingHeartbeat() {
        val heartBeatBusy = Heartbeat("test", AgentState.BUSY, ExecutionProgress(0))

        webClient.post()
            .uri("/heartbeat")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Mono.just(heartBeatBusy), Heartbeat::class.java)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun checkNewJobResponse() {
        val list = listOf(TestDto("qwe", "WarnPlugin", 0, "hash", listOf("tag")))
        // /getTestBatches
        mockServer.enqueue(
            MockResponse()
                .setBody(Json.encodeToString(TestBatch(list, mapOf(0L to ""))))
                .addHeader("Content-Type", "application/json")
        )

        val testSuite = TestSuite(TestSuiteType.PROJECT, "", null, null, LocalDateTime.now(), ".", ".").apply {
            id = 0
        }

        // /testSuite/{id}
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(testSuite))
        )

        val monoResponse = agentService.getNewTestsIds("container-1").block() as NewJobResponse

        assertTrue(monoResponse.tests.isNotEmpty() && monoResponse.tests.first().filePath == "qwe")
    }

    @Test
    fun `should not shutdown any agents when not all of them are IDLE`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.BUSY, "test-2"),
            ),
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100))),
            testBatch = TestBatch(emptyList(), emptyMap()),
            testSuite = null,
            mockAgentStatuses = true,
        ) {
            verify(dockerService, times(0)).stopAgents(any())
        }
    }

    @Test
    fun `should not shutdown any agents when all agents are IDLE but there are more tests left`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-2"),
            ),
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100))),
            testBatch = TestBatch(
                listOf(
                    TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                    TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                    TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
                ),
                mapOf(1L to "")
            ),
            testSuite = TestSuite(TestSuiteType.PROJECT, "", null, null, LocalDateTime.now(), ".", ".").apply {
                id = 0
            },
            mockAgentStatuses = false,
        ) {
            verify(dockerService, times(0)).stopAgents(any())
        }
    }

    @Test
    fun `should shutdown idle agents when there are no tests left`() {
        whenever(dockerService.stopAgents(any())).thenReturn(true)
        val agentStatusDtos = listOf(
            AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
            AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-2"),
        )
        testHeartbeat(
            agentStatusDtos = agentStatusDtos,
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100))),
            heartBeatInterval = 0,
            testBatch = TestBatch(emptyList(), emptyMap()),
            testSuite = null,
            mockAgentStatuses = true,
            {
                // /getAgentsStatusesForSameExecution after shutdownIntervalMillis
                mockServer.enqueue(
                    MockResponse()
                        .setBody(
                            objectMapper.writeValueAsString(
                                AgentStatusesForExecution(0, agentStatusDtos)
                            )
                        )
                        .addHeader("Content-Type", "application/json")
                )
                // additional setup for marking stuff as FINISHED
                // /updateExecutionByDto
                mockServer.enqueue(
                    MockResponse().setResponseCode(200)
                )
            }
        ) {
            verify(dockerService, times(1)).stopAgents(any())
        }
    }

    @Test
    fun `should not shutdown any agents when they are STARTING`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.STARTING, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.STARTING, "test-2"),
            ),
            heartbeats = listOf(Heartbeat("test-1", AgentState.STARTING, ExecutionProgress(0))),
            testBatch = TestBatch(
                listOf(
                    TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                    TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                    TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
                ),
                mapOf(1L to "")
            ),
            testSuite = TestSuite(TestSuiteType.PROJECT, "", null, null, LocalDateTime.now(), ".", ".").apply {
                id = 0
            },
            mockAgentStatuses = false,
        ) {
            verify(dockerService, times(0)).stopAgents(any())
        }
    }

    @Test
    fun `should shutdown agent, which don't sent heartbeat for some time`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.STARTING, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.BUSY, "test-2"),
            ),
            heartbeats = listOf(
                Heartbeat("test-1", AgentState.STARTING, ExecutionProgress(0)),
                Heartbeat("test-2", AgentState.BUSY, ExecutionProgress(0)),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0)),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0)),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0)),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0)),
            ),
            heartBeatInterval = 1_000,
            testBatch = TestBatch(
                listOf(
                    TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                    TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                    TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
                ),
                mapOf(1L to "")
            ),
            testSuite = TestSuite(TestSuiteType.PROJECT, "", null, null, LocalDateTime.now(), ".", ".").apply {
                id = 0
            },
            mockAgentStatuses = false,
        ) {
            // FixMe: we actually need to check the size of crashed agents list somehow
            verify(dockerService, atLeast(1)).stopAgents(any())
        }
    }

    @Test
    fun `should shutdown all agents, since all of them don't sent heartbeats for some time`() {
        val agentStatusDtos = listOf(
            AgentStatusDto(LocalDateTime.now(), AgentState.STARTING, "test-1"),
            AgentStatusDto(LocalDateTime.now(), AgentState.BUSY, "test-2"),
        )
        testHeartbeat(
            agentStatusDtos = agentStatusDtos,
            heartbeats = listOf(
                Heartbeat("test-1", AgentState.STARTING, ExecutionProgress(0)),
                Heartbeat("test-2", AgentState.BUSY, ExecutionProgress(0)),
            ),
            heartBeatInterval = 3000,
            testBatch = TestBatch(
                listOf(
                    TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                    TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                    TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
                ),
                mapOf(1L to "")
            ),
            testSuite = TestSuite(TestSuiteType.PROJECT, "", null, null, LocalDateTime.now(), ".", ".").apply {
                id = 0
            },
            mockAgentStatuses = false,
        ) {
            // FixMe: we actually need to check the size of crashed agents list somehow
            verify(dockerService, atLeast(2)).stopAgents(any())
        }
    }

    @Test
    fun `should shutdown agents even if there are some already FINISHED`() {
        val agentStatusDtos = listOf(
            AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
            AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-2"),
            AgentStatusDto(LocalDateTime.parse("2021-01-01T00:00:00"), AgentState.FINISHED, "test-1"),
            AgentStatusDto(LocalDateTime.parse("2021-01-01T00:00:00"), AgentState.FINISHED, "test-2"),
        )
        testHeartbeat(
            agentStatusDtos = agentStatusDtos,
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100))),
            heartBeatInterval = 0,
            testBatch = TestBatch(emptyList(), emptyMap()),
            testSuite = null,
            mockAgentStatuses = true,
            {
                // /getAgentsStatusesForSameExecution after shutdownIntervalMillis
                mockServer.enqueue(
                    MockResponse()
                        .setBody(
                            objectMapper.writeValueAsString(
                                AgentStatusesForExecution(0, agentStatusDtos)
                            )
                        )
                        .addHeader("Content-Type", "application/json")
                )
            }
        ) {
            verify(dockerService, times(1)).stopAgents(any())
        }
    }

    /**
     * Test logic triggered by a heartbeat.
     *
     * @param agentStatusDtos agent statuses that are returned from backend (mocked response)
     * @param heartbeat a [Heartbeat] that is received by orchestrator
     * @param testBatch a batch of tests returned from backend (mocked response)
     * @param mockAgentStatuses whether a mocked response for `/getAgentsStatusesForSameExecution` should be added to queue
     * @param additionalSetup is executed before the request is performed
     * @param verification a lambda for test assertions
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("TOO_LONG_FUNCTION", "TOO_MANY_PARAMETERS", "LongParameterList")
    private fun testHeartbeat(
        agentStatusDtos: List<AgentStatusDto>,
        heartbeats: List<Heartbeat>,
        heartBeatInterval: Long = 0,
        testBatch: TestBatch,
        testSuite: TestSuite?,
        mockAgentStatuses: Boolean = false,
        additionalSetup: () -> Unit = {},
        verification: () -> Unit,
    ) {
        // /getTestBatches
        mockServer.enqueue(
            MockResponse()
                .setBody(Json.encodeToString(testBatch))
                .addHeader("Content-Type", "application/json")
        )

        // /testSuite/{id}
        testSuite?.let {
            mockServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(objectMapper.writeValueAsString(testSuite))
            )
        }

        if (mockAgentStatuses) {
            // /getAgentsStatusesForSameExecution
            mockServer.enqueue(
                MockResponse()
                    .setBody(
                        objectMapper.writeValueAsString(
                            AgentStatusesForExecution(0, agentStatusDtos)
                        )
                    )
                    .addHeader("Content-Type", "application/json")
            )
        }
        additionalSetup()
        val assertions = CompletableFuture.supplyAsync {
            buildList<RecordedRequest?> {
                mockServer.takeRequest(60, TimeUnit.SECONDS)
                mockServer.takeRequest(60, TimeUnit.SECONDS)
                if (mockAgentStatuses) {
                    mockServer.takeRequest(60, TimeUnit.SECONDS)
                }
            }
        }

        heartbeats.forEach { heartbeat ->
            webClient.post()
                .uri("/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(heartbeat))
                .exchange()
                .expectStatus().isOk
            Thread.sleep(heartBeatInterval)
        }

        // wait for background tasks
        Thread.sleep(5_000)

        assertions.orTimeout(60, TimeUnit.SECONDS).join().forEach { Assertions.assertNotNull(it) }
        verification.invoke()
    }

    companion object {
        @JvmStatic
        private lateinit var mockServer: MockWebServer

        @AfterAll
        fun tearDown() {
            mockServer.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            // todo: should be initialized in @BeforeAll, but it gets called after @DynamicPropertySource
            mockServer = MockWebServer()
            // todo: extract request-based dispatcher to save-cloud-common
            mockServer.dispatcher = object : QueueDispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path
                    if (path != null && (path.contains("/testExecution/assignAgent") || path.contains("/updateAgentStatusesWithDto"))) {
                        return MockResponse().setResponseCode(200)
                    }
                    return super.dispatch(request)
                }
            }
            (mockServer.dispatcher as QueueDispatcher).setFailFast(true)
            mockServer.start()
            registry.add("orchestrator.backendUrl") { "http://localhost:${mockServer.port}" }
        }
    }
}
