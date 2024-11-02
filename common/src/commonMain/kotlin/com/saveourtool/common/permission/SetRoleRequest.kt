package com.saveourtool.common.permission

import com.saveourtool.common.domain.Role

import kotlinx.serialization.Serializable

/**
 * @property userName user whose role will be updated
 * @property role a new role
 */
@Serializable
data class SetRoleRequest(
    val userName: String,
    val role: Role,
)
