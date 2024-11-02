package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDto
import javax.persistence.Entity

/**
 * @property containerId id of the container, inside which the agent is running
 * @property containerName name of the container, inside which the agent is running
 * @property version
 */
@Entity
class Agent(
    var containerId: String,
    var containerName: String,

    var version: String,
) : BaseEntityWithDto<AgentDto>() {
    /**
     * @return [AgentDto] from [Agent]
     */
    override fun toDto(): AgentDto = AgentDto(
        containerId = containerId,
        containerName = containerName,
        version = version,
    )
}

/**
 * @return [Agent] from [AgentDto]
 */
fun AgentDto.toEntity() = Agent(
    containerId = containerId,
    containerName = containerName,
    version = version,
)
