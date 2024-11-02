package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDto
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property execution execution that is connected to [agent]
 * @property agent agent assigned to [execution]
 */
@Entity
class LnkExecutionAgent(
    @ManyToOne
    @JoinColumn(name = "execution_id")
    var execution: Execution,

    @ManyToOne
    @JoinColumn(name = "agent_id")
    var agent: Agent,
) : BaseEntityWithDto<LnkExecutionAgentDto>() {
    override fun toDto() = LnkExecutionAgentDto(
        execution.toDto(),
        agent.toDto(),
    )
}
