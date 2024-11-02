package com.saveourtool.save.orchestrator.controller.agents

import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.Project
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.common.execution.TestingType
import com.saveourtool.save.orchestrator.controller.AgentsController
import com.saveourtool.save.orchestrator.runner.ContainerRunner
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.service.OrchestratorAgentService
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.ContainerService
import com.saveourtool.save.orchestrator.utils.emptyResponseAsMono

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient

import reactor.core.publisher.Mono
import java.net.URL

@WebFluxTest(controllers = [AgentsController::class])
@Import(AgentService::class)
@MockBeans(MockBean(ContainerRunner::class))
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentsControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @MockBean private lateinit var containerService: ContainerService
    @MockBean private lateinit var orchestratorAgentService: OrchestratorAgentService
    @MockBean private lateinit var containerRunner: ContainerRunner

    @Test
    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "UnsafeCallOnNullableType")
    fun `should build image, query backend and start containers`() {
        val project = Project.stub(null)
        val execution = Execution.stub(project).apply {
            type = TestingType.PUBLIC_TESTS
            status = ExecutionStatus.PENDING
            id = 42L
        }
        whenever(containerService.prepareConfiguration(any())).thenReturn(
            ContainerService.RunConfiguration(
                imageTag = "test-image-id",
                runCmd = listOf("sh", "-c", "test-exec-cmd"),
                workingDir = EXECUTION_DIR,
                env = emptyMap(),
            )
        )

        whenever(containerRunner.getContainerIdentifier(any())).thenReturn("save-test-agent-id-1")

        whenever(containerService.validateContainersAreStarted(any()))
            .thenReturn(Mono.just(Unit).then())
        whenever(orchestratorAgentService.addAgent(anyLong(), any()))
            .thenReturn(emptyResponseAsMono)
        whenever(orchestratorAgentService.updateAgentStatus(any()))
            .thenReturn(emptyResponseAsMono)
        whenever(orchestratorAgentService.updateExecutionStatus(anyLong(), any(), anyOrNull()))
            .thenReturn(emptyResponseAsMono)
        // /updateExecutionByDto is not mocked, because it's performed by DockerService, and it's mocked in these tests

        webClient
            .post()
            .uri("/initializeAgents")
            .bodyValue(execution.toRunRequest(URL("https://someUrl")))
            .exchange()
            .expectStatus()
            .isAccepted
        Thread.sleep(2_500)  // wait for background task to complete on mocks
        verify(containerService).prepareConfiguration(any())
        verify(containerService).createAndStartContainers(any(), any())
        verify(containerService).validateContainersAreStarted(any())
    }

    @Test
    fun checkPostResponseIsNotOk() {
        val project = Project.stub(null)
        val execution = Execution.stub(project)

        assertThrows<IllegalArgumentException> {
            execution.toRunRequest(URL("https://someUrl"))
        }
    }

    @Test
    fun `should cleanup execution artifacts`() {
        webClient.post()
            .uri("/cleanup?executionId=42")
            .exchange()
            .expectStatus()
            .isOk

        Thread.sleep(2_500)
        verify(containerService, times(1)).cleanupAllByExecution(anyLong())
    }
}
