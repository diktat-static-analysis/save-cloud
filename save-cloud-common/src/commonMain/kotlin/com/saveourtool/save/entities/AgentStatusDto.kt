/**
 * Contains extra class [com.saveourtool.save.entities.AgentStatusesForExecution]
 */

package com.saveourtool.save.entities

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.utils.getCurrentLocalDateTime
import kotlinx.datetime.LocalDateTime

/**
 * @property state current state of the agent
 * @property containerId id of the agent's container
 * @property time
 */
data class AgentStatusDto(
    val state: AgentState,
    val containerId: String,
    val time: LocalDateTime = getCurrentLocalDateTime(),
)

/**
 * Statuses of a group of agents for a single Execution
 *
 * @property executionId id of Execution
 * @property agentStatuses list of [AgentStatusDto]s
 */
data class AgentStatusesForExecution(
    val executionId: Long,
    val agentStatuses: List<AgentStatusDto>,
)
