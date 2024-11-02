package com.saveourtool.common.entities.cosv

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property identifier
 * @property modified
 * @property id
 * @property prevCosvFileId
 */
@Serializable
data class CosvFileDto(
    val id: Long,
    val identifier: String,
    val modified: LocalDateTime,
    val prevCosvFileId: Long?,
)
