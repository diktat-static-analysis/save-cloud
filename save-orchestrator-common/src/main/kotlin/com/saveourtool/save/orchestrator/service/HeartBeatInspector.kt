package com.saveourtool.save.orchestrator.service

import com.saveourtool.common.agent.Heartbeat
import com.saveourtool.common.entities.AgentStatusDto
import com.saveourtool.save.orchestrator.utils.AgentStatusInMemoryRepository

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Background inspector, which detect crashed agents
 * TODO: can be used to store data about existing agents on orchestrator startup ([#11](https://github.com/saveourtool/save-cloud/issues/11))
 */
@Component
class HeartBeatInspector(
    private val containerService: ContainerService,
    private val agentService: AgentService,
    private val agentStatusInMemoryRepository: AgentStatusInMemoryRepository,
) {
    /**
     * Collect information about the latest heartbeats from agents, in aim to determine crashed one later
     *
     * @param heartbeat
     */
    fun updateAgentHeartbeatTimeStamps(heartbeat: Heartbeat) {
        agentStatusInMemoryRepository.upsert(
            executionId = heartbeat.executionProgress.executionId,
            AgentStatusDto(
                containerId = heartbeat.agentInfo.containerId,
                state = heartbeat.state,
            ),
        )
    }

    /**
     * @param containerId
     */
    fun unwatchAgent(containerId: String) {
        agentStatusInMemoryRepository.delete(containerId)
    }

    /**
     * @param containerId
     */
    fun watchCrashedAgent(containerId: String) {
        agentStatusInMemoryRepository.markAsCrashed(containerId)
    }

    /**
     * Consider agent as crashed, if it didn't send heartbeats for some time
     */
    fun determineCrashedAgents() {
        agentStatusInMemoryRepository.updateByStatus { containerId -> containerService.isStopped(containerId) }
    }

    /**
     * Stop crashed agents and mark corresponding test executions as failed with internal error
     */
    fun processExecutionWithCrashedAgents() {
        agentStatusInMemoryRepository.processExecutionWithAllCrashedContainers { executionIds ->
            executionIds.forEach { executionId ->
                logger.warn("All agents for execution $executionId are crashed, initialize finalization of it.")
                agentService.finalizeExecution(executionId)
            }
        }
    }

    @Scheduled(cron = "\${orchestrator.heart-beat-inspector-cron}")
    private fun run() {
        determineCrashedAgents()
        processExecutionWithCrashedAgents()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HeartBeatInspector::class.java)
    }
}
