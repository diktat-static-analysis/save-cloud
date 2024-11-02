package com.saveourtool.common.entities

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property id
 * @property message
 * @property createDate
 */
@Serializable
class NotificationDto(
    val id: Long,
    val message: String,
    val createDate: LocalDateTime?,
)
