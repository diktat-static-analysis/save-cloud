package org.cqfn.save.entities

import org.cqfn.save.domain.Role

import kotlinx.serialization.Serializable

/**
 * @property name
 * @property source
 * @property projects
 * @property source where the user identity is coming from, e.g. "github"
 */
@Serializable
data class UserDto(
    val name: String?,
    val source: String,
    val projects: Map<String, Role?>,
)
