package com.saveourtool.common.execution

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property status
 * @property failReason
 */
@Serializable
data class ExecutionUpdateDto(
    val id: Long,
    val status: ExecutionStatus,
    val failReason: String? = null
)
