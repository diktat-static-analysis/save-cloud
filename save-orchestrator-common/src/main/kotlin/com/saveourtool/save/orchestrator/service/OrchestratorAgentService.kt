package com.saveourtool.save.orchestrator.service

import com.saveourtool.common.agent.AgentInitConfig
import com.saveourtool.common.agent.AgentRunConfig
import com.saveourtool.common.agent.TestExecutionDto
import com.saveourtool.common.entities.*
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.common.test.TestBatch
import com.saveourtool.common.utils.EmptyResponse

import reactor.core.publisher.Mono

typealias TestExecutionList = List<TestExecutionDto>

/**
 * Repository to work with agents
 */
interface OrchestratorAgentService {
    /**
     * Gets config to init agent
     *
     * @param containerId
     * @return [Mono] of [AgentInitConfig]
     */
    fun getInitConfig(containerId: String): Mono<AgentInitConfig>

    /**
     * Gets new tests ids
     *
     * @param containerId
     * @return [Mono] of [TestBatch]
     */
    fun getNextRunConfig(containerId: String): Mono<AgentRunConfig>

    /**
     * Save new agents to the DB and insert their statuses
     *
     * @param executionId ID of an execution
     * @param agent [AgentDto] to save in the DB
     * @return a Mono without body
     */
    fun addAgent(executionId: Long, agent: AgentDto): Mono<EmptyResponse>

    /**
     * @param agentStatus [AgentStatusDto] to update/insert in the DB
     * @return a Mono without body
     */
    fun updateAgentStatus(agentStatus: AgentStatusDto): Mono<EmptyResponse>

    /**
     * Get List of [TestExecutionDto] for agent [containerId] have status READY_FOR_TESTING
     *
     * @param containerId agent for which data is checked
     * @return list of saved [TestExecutionDto]
     */
    fun getReadyForTestingTestExecutions(containerId: String): Mono<TestExecutionList>

    /**
     * Fetches the status of execution
     *
     * @param executionId execution for which the status is required
     * @return a Mono with [ExecutionStatus]
     */
    fun getExecutionStatus(
        executionId: Long,
    ): Mono<ExecutionStatus>

    /**
     * Marks the execution to specified state
     *
     * @param executionId execution that should be updated
     * @param executionStatus new status for execution
     * @param failReason to show to user in case of error status
     * @return a Mono without body
     */
    fun updateExecutionStatus(
        executionId: Long,
        executionStatus: ExecutionStatus,
        failReason: String?,
    ): Mono<EmptyResponse>

    /**
     * @param executionId ID of an execution
     * @return Mono with [AgentStatusDtoList]: agent statuses belonged to a [com.saveourtool.save.entities.Execution] with provided ID
     */
    fun getAgentStatusesByExecutionId(executionId: Long): Mono<AgentStatusDtoList>

    /**
     * Mark agent's test executions as failed
     *
     * @param containerId the agent container ID, for which, corresponding test executions should be marked as failed
     * @return a Mono without body
     */
    fun markReadyForTestingTestExecutionsOfAgentAsFailed(containerId: String): Mono<EmptyResponse>

    /**
     * Mark agent's test executions as failed
     *
     * @param executionId the ID of an execution, for which, corresponding test executions should be marked as failed
     * @return a Mono without body
     */
    fun markAllTestExecutionsOfExecutionAsFailed(executionId: Long): Mono<EmptyResponse>
}
