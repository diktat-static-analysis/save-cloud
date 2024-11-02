package com.saveourtool.common.entities

import com.saveourtool.common.execution.ExecutionDto
import kotlinx.serialization.Serializable

/**
 * @property execution
 * @property agent
 */
@Serializable
data class LnkExecutionAgentDto(
    val execution: ExecutionDto,
    val agent: AgentDto,
)
