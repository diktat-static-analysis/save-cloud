package com.saveourtool.common.entities

import com.saveourtool.common.execution.ExecutionDto
import kotlinx.serialization.Serializable

/**
 * @property execution
 * @property file
 */
@Serializable
data class LnkExecutionFileDto(
    val execution: ExecutionDto,
    val file: FileDto,
)
