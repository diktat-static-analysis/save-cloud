package com.saveourtool.common.entities

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property message
 * @property userName
 * @property userAvatar
 * @property createDate
 * @property section
 * @property userRating
 */
@Serializable
data class CommentDto(
    val message: String,
    val userName: String,
    val userRating: Long,
    val userAvatar: String?,
    val createDate: LocalDateTime?,
    val section: String = "",
) {
    companion object {
        val empty = CommentDto(
            "",
            "Unknown",
            0,
            null,
            null,
        )
    }
}
