package com.saveourtool.save.orchestrator.service

import com.saveourtool.common.agent.AgentInitConfig
import com.saveourtool.common.agent.AgentRunConfig
import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.entities.AgentDto
import com.saveourtool.common.entities.AgentStatusDto
import com.saveourtool.common.entities.AgentStatusDtoList
import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.common.execution.ExecutionUpdateDto
import com.saveourtool.common.spring.utils.applyAll
import com.saveourtool.common.utils.*
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

/**
 * Service for work with agents and backend
 */
@Component
class BackendOrchestratorAgentService(
    @Value("\${orchestrator.backend-url}") private val backendUrl: String,
    customizers: List<WebClientCustomizer>,
) : OrchestratorAgentService {
    private val webClientBackend = WebClient.builder()
        .baseUrl(backendUrl)
        .applyAll(customizers)
        .build()

    override fun getInitConfig(containerId: String): Mono<AgentInitConfig> = webClientBackend
        .get()
        .uri("/agents/get-init-config?containerId=$containerId")
        .retrieve()
        .bodyToMono()

    override fun getNextRunConfig(containerId: String): Mono<AgentRunConfig> = webClientBackend
        .get()
        .uri("/agents/get-next-run-config?containerId=$containerId")
        .retrieve()
        .bodyToMono()

    override fun addAgent(executionId: Long, agent: AgentDto): Mono<EmptyResponse> = webClientBackend
        .post()
        .uri("/agents/insert?executionId=$executionId")
        .bodyValue(agent)
        .retrieve()
        .toBodilessEntity()

    override fun updateAgentStatus(agentStatus: AgentStatusDto): Mono<EmptyResponse> =
            webClientBackend
                .post()
                .uri("/updateAgentStatus")
                .bodyValue(agentStatus)
                .retrieve()
                .toBodilessEntity()

    override fun getReadyForTestingTestExecutions(containerId: String): Mono<TestExecutionList> = webClientBackend.get()
        .uri("/test-executions/get-by-container-id?containerId=$containerId&status=${TestResultStatus.READY_FOR_TESTING}")
        .retrieve()
        .bodyToMono()

    override fun getExecutionStatus(
        executionId: Long,
    ): Mono<ExecutionStatus> =
            webClientBackend.get()
                .uri("/executionDto?executionId=$executionId")
                .retrieve()
                .bodyToMono<ExecutionDto>()
                .map { it.status }

    override fun updateExecutionStatus(
        executionId: Long,
        executionStatus: ExecutionStatus,
        failReason: String?,
    ): Mono<EmptyResponse> =
            webClientBackend.post()
                .uri("/updateExecutionByDto")
                .bodyValue(ExecutionUpdateDto(executionId, executionStatus, failReason))
                .retrieve()
                .toBodilessEntity()

    override fun getAgentStatusesByExecutionId(executionId: Long): Mono<AgentStatusDtoList> = webClientBackend
        .get()
        .uri("/getAgentStatusesByExecutionId?executionId=$executionId")
        .retrieve()
        .bodyToMono()

    override fun markReadyForTestingTestExecutionsOfAgentAsFailed(
        containerId: String,
    ): Mono<EmptyResponse> {
        log.debug("Attempt to mark test executions of containerId=$containerId as failed with internal error")
        return webClientBackend.post()
            .uri("/test-executions/mark-ready-for-testing-as-failed-by-container-id?containerId=$containerId")
            .retrieve()
            .toBodilessEntity()
    }

    override fun markAllTestExecutionsOfExecutionAsFailed(executionId: Long): Mono<EmptyResponse> {
        log.debug("Attempt to mark test executions of execution=$executionId as failed with internal error")
        return webClientBackend.post()
            .uri("/test-executions/mark-all-as-failed-by-execution-id?executionId=$executionId")
            .retrieve()
            .toBodilessEntity()
    }

    companion object {
        private val log: Logger = getLogger<BackendOrchestratorAgentService>()
    }
}
