package com.saveourtool.common.entities

import com.saveourtool.common.validation.Validatable
import com.saveourtool.common.validation.isValidUrl
import kotlinx.serialization.Serializable

/**
 * @property url
 * @property username
 * @property password
 */
@Serializable
data class GitDto(
    val url: String,
    val username: String? = null,
    val password: String? = null,
) : Validatable {
    override fun validate(): Boolean = url.isValidUrl()
    companion object {
        val empty = GitDto("")
    }
}
