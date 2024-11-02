/**
 * Data classes for agent status
 */

package com.saveourtool.common.entities

import com.saveourtool.common.agent.AgentState
import com.saveourtool.common.spring.entity.BaseEntity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime

/**
 * @property startTime staring time of status
 * @property endTime time of update
 * @property state current state of the agent
 * @property agent agent who's state is described
 */
@Entity
class AgentStatus(
    var startTime: LocalDateTime,
    var endTime: LocalDateTime,

    @Enumerated(EnumType.STRING)
    var state: AgentState,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    var agent: Agent,
) : BaseEntity() {
    /**
     * @return this object converted to [AgentStatusDto]
     */
    fun toDto() = AgentStatusDto(state, agent.containerId, endTime.toKotlinLocalDateTime())
}

/**
 * @param agentResolver resolver for [Agent] by [AgentStatusDto.containerId]
 * @return [AgentStatus] built from [AgentStatusDto]
 */
fun AgentStatusDto.toEntity(agentResolver: (String) -> Agent) = AgentStatus(
    startTime = time.toJavaLocalDateTime(),
    endTime = time.toJavaLocalDateTime(),
    state = state,
    agent = agentResolver(containerId)
)
