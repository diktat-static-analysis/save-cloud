package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.runner.ContainerRunner
import com.saveourtool.save.orchestrator.utils.AgentStatusInMemoryRepository
import com.saveourtool.save.utils.EmptyResponse
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class ExecutionService(
    private val configProperties: ConfigProperties,
    private val containerRunner: ContainerRunner,
    private val orchestratorAgentService: OrchestratorAgentService,
    private val agentStatusInMemoryRepository: AgentStatusInMemoryRepository,
    private val agentService: AgentService,
) {
    /**
     * This method should be called when all agents are done and execution status can be updated and cleanup can be performed
     *
     * @param executionId an ID of the execution, that will be checked.
     */
    @Suppress("TOO_LONG_FUNCTION", "AVOID_NULL_CHECKS")
    internal fun finalizeExecution(executionId: Long) {
        // Get a list of agents for this execution, if their statuses indicate that the execution can be terminated.
        // I.e., all agents must be stopped by this point in order to move further in shutdown logic.
        haveAllAgentsFinalStatusByExecutionId(executionId)
            .filter { haveFinalStatus ->
                if (!haveFinalStatus) {
                    log.debug { "Agents for execution $executionId are still running, so won't try to stop them" }
                }
                haveFinalStatus
            }
            .flatMap {
                // need to retry after some time, because for other agents BUSY state might have not been written completely
                log.debug("Waiting for ${configProperties.shutdown.checksIntervalMillis} ms to repeat `haveAllAgentsFinalStatusByExecutionId` call for execution=$executionId")
                Mono.delay(Duration.ofMillis(configProperties.shutdown.checksIntervalMillis)).then(
                    haveAllAgentsFinalStatusByExecutionId(executionId)
                )
            }
            .filter { it }
            .filterWhen {
                orchestratorAgentService.getExecutionStatus(executionId)
                    .map {
                        it !in setOf(ExecutionStatus.FINISHED, ExecutionStatus.ERROR)
                    }
                    .doOnNext { hasNotFinalStatus ->
                        if (!hasNotFinalStatus) {
                            log.info { "Execution id=$executionId already has final status, skip finalization" }
                        }
                    }
            }
            .flatMap {
                log.info { "For execution id=$executionId all agents have completed their lifecycle" }
                markExecutionBasedOnAgentStates(executionId)
                    .then(Mono.fromCallable {
                        agentStatusInMemoryRepository.tryDeleteAllByExecutionId(executionId)
                        containerRunner.cleanupAllByExecution(executionId)
                    })
            }
            .subscribeOn(agentService.scheduler)
            .subscribe()
    }

    /**
     * Updates status of execution [executionId] based on statues of agents assigned to execution
     *
     * @param executionId id of an Execution
     * @return Mono with response from backend
     */
    private fun markExecutionBasedOnAgentStates(
        executionId: Long,
    ): Mono<EmptyResponse> {
        // all { TERMINATED } -> FINISHED
        // all { CRASHED } -> ERROR; set all test executions to CRASHED
        return orchestratorAgentService
            .getAgentStatusesByExecutionId(executionId)
            .flatMap { agentStatuses ->
                // todo: take test execution statuses into account too
                if (agentStatuses.areAllStatesIn(AgentState.TERMINATED)) {
                    updateExecution(executionId, ExecutionStatus.FINISHED)
                } else if (agentStatuses.areAllStatesIn(AgentState.CRASHED)) {
                    updateExecution(executionId, ExecutionStatus.ERROR,
                        "All agents for this execution were crashed unexpectedly"
                    ).then(markAllTestExecutionsOfExecutionAsFailed(executionId))
                } else {
                    Mono.error(NotImplementedError("Updating execution (id=$executionId) status for agents with statuses $agentStatuses is not supported yet"))
                }
            }
    }

    /**
     * Marks the execution to specified state
     *
     * @param executionId execution that should be updated
     * @param executionStatus new status for execution
     * @param failReason to show to user in case of error status
     * @return a bodiless response entity
     */
    fun updateExecution(executionId: Long, executionStatus: ExecutionStatus, failReason: String? = null): Mono<EmptyResponse> =
            orchestratorAgentService.updateExecutionStatus(executionId, executionStatus, failReason)

    /**
     * Checks [AgentStatusDto] in DB to detect that agents have completed their jobs.
     *
     * We assume, that all agents will eventually have one of statuses [areFinishedOrStopped].
     * Situations when agent gets stuck with a different status and for whatever reason is unable to update
     * it, are not handled. Anyway, such agents should be eventually stopped by [HeartBeatInspector].
     *
     * @param executionId containerId of an agent
     * @return [Mono] with result that all agents assigned to [executionId] have final status
     */
    private fun haveAllAgentsFinalStatusByExecutionId(executionId: Long): Mono<Boolean> = orchestratorAgentService
        .getAgentStatusesByExecutionId(executionId)
        .map { agentStatuses ->
            log.debug { "For executionId=$executionId agent statuses are $agentStatuses" }
            // with new logic, should we check only for CRASHED, STOPPED, TERMINATED?
            agentStatuses.areFinishedOrStopped()
                .also { areAllAgentsFinishedOrStopped ->
                    if (areAllAgentsFinishedOrStopped) {
                        log.debug { "For execution id=$executionId there are finished or stopped agents" }
                    }
                }
        }

    /**
     * Checks whether all agent under one execution have completed their jobs.
     *
     * @param executionId ID of an execution
     * @return true if all agents match [areIdleOrFinished]
     */
    fun areAllAgentsIdleOrFinished(executionId: Long): Mono<Boolean> = orchestratorAgentService
        .getAgentStatusesByExecutionId(executionId)
        .map { agentStatuses ->
            log.debug("For executionId=$executionId agent statuses are $agentStatuses")
            agentStatuses.areIdleOrFinished()
        }

    /**
     * Mark agent's test executions as failed
     *
     * @param executionId the ID of execution, for which, corresponding test executions should be marked as failed
     * @return a bodiless response entity
     */
    fun markAllTestExecutionsOfExecutionAsFailed(
        executionId: Long,
    ): Mono<EmptyResponse> = orchestratorAgentService.markAllTestExecutionsOfExecutionAsFailed(executionId)

    private fun Collection<AgentStatusDto>.areIdleOrFinished() = areAllStatesIn(*finishedOrStoppedStates, AgentState.IDLE)

    private fun Collection<AgentStatusDto>.areFinishedOrStopped() = areAllStatesIn(*finishedOrStoppedStates)

    private fun Collection<AgentStatusDto>.areAllStatesIn(vararg states: AgentState) = all { it.state in states }

    companion object {
        private val log: Logger = getLogger<ExecutionService>()
        private val finishedOrStoppedStates = arrayOf(AgentState.FINISHED, AgentState.CRASHED, AgentState.TERMINATED)
    }
}
