package org.cqfn.save.orchestrator.controller.agents

import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.orchestrator.config.Beans
import org.cqfn.save.orchestrator.controller.AgentsController
import org.cqfn.save.orchestrator.service.AgentService
import org.cqfn.save.orchestrator.service.DockerService

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

import java.nio.charset.Charset
import java.time.LocalDateTime

@WebFluxTest(controllers = [AgentsController::class])
@Import(AgentService::class, Beans::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentsControllerTest {
    private val stubTime = LocalDateTime.now()

    @Autowired
    lateinit var webClient: WebTestClient
    @MockBean private lateinit var dockerService: DockerService

    @AfterEach
    fun tearDown() {
        mockServer.dispatcher.peek().let { mockResponse ->
            Assertions.assertTrue(mockResponse.getBody().let { it == null || it.size == 0L }) {
                "There is an enqueued response in the MockServer after a test has completed. Enqueued body: ${mockResponse.getBody()?.readString(Charset.defaultCharset())}"
            }
        }
    }

    @Test
    fun checkPostResponseIsOk() {
        val project = Project("Huawei", "huaweiName", "manual", "huaweiUrl", "description")
        val execution = Execution(project, stubTime, stubTime, ExecutionStatus.PENDING, "stub", "stub", 0, 20).apply {
            id = 42L
        }
        whenever(dockerService.buildAndCreateContainers(any())).thenReturn(listOf("test-agent-id-1", "test-agent-id-2"))
        // /addAgents
        mockServer.enqueue(MockResponse().setResponseCode(200))
        // /updateAgentStatuses
        mockServer.enqueue(MockResponse().setResponseCode(200))
        // /updateExecution
        mockServer.enqueue(MockResponse().setResponseCode(200))

        webClient
            .post()
            .uri("/initializeAgents")
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .isOk
        Thread.sleep(1_000)  // wait for background task to complete on mocks
        verify(dockerService).buildAndCreateContainers(any())
        verify(dockerService).startContainersAndUpdateExecution(any(), anyList())
    }

    @Test
    fun checkPostResponseIsNotOk() {
        val project = Project("Huawei", "huaweiName", "manual", "huaweiUrl", "description")
        val execution = Execution(project, stubTime, stubTime, ExecutionStatus.RUNNING, "stub", "stub", 0, 20)
        webClient
            .post()
            .uri("/initializeAgents")
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .is4xxClientError
    }

    @Test
    fun `should stop agents by id`() {
        webClient
            .post()
            .uri("/stopAgents")
            .body(BodyInserters.fromValue(listOf("id-of-agent")))
            .exchange()
            .expectStatus()
            .isOk
        verify(dockerService).stopAgents(anyList())
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
            mockServer.start()
            registry.add("orchestrator.backendUrl") { "http://localhost:${mockServer.port}" }
        }
    }
}
