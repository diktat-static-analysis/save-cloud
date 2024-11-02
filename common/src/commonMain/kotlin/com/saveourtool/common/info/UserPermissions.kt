package com.saveourtool.common.info

import kotlinx.serialization.Serializable

/**
 * @property inOrganizations user permissions in organizations
 */
@Serializable
data class UserPermissions(
    val inOrganizations: Map<String, UserPermissionsInOrganization> = emptyMap(),
) {
    companion object {
        /**
         * Value that represents an empty [UserPermissions]
         */
        val empty = UserPermissions(
            inOrganizations = emptyMap(),
        )
    }
}
