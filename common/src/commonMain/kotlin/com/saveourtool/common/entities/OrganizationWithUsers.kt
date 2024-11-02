package com.saveourtool.common.entities

import com.saveourtool.common.domain.Role
import kotlinx.serialization.Serializable

/**
 * @property organization
 * @property userRoles map where keys are usernames and values are their [Role]s
 */
@Serializable
data class OrganizationWithUsers(
    val organization: OrganizationDto,
    val userRoles: Map<String, Role>,
)
