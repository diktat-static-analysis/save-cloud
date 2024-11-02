package com.saveourtool.common.domain

import kotlin.js.JsExport
import kotlinx.serialization.Serializable

/**
 * User roles
 * @property formattedName string representation of the [Role] that should be printed
 * @property priority
 */
@Suppress("MAGIC_NUMBER", "MagicNumber")
@Serializable
@JsExport
enum class Role(val formattedName: String, val priority: Int) {
    /**
     * admin in organization
     */
    ADMIN("Admin", 2),

    /**
     * Has no role (synonym to null)
     */
    NONE("None", 0),

    /**
     * User that has created this project
     */
    OWNER("Owner", 3),

    /**
     * Superuser, that has access to everything
     */
    SUPER_ADMIN("Super admin", 4),

    /**
     * Has readonly access to public projects.
     */
    VIEWER("Viewer", 1),
    ;

    /**
     * @return this role with default prefix for spring-security
     */
    fun asSpringSecurityRole() = "ROLE_$name"

    /**
     * The method compares the priority between two roles
     *
     * @param that role to compare
     * @return comparison result
     */
    fun isHigherOrEqualThan(that: Role) = this.priority >= that.priority

    /**
     * The method compares the priority between two roles
     *
     * @param that role to compare
     * @return comparison result
     */
    fun isLowerThan(that: Role) = this.priority < that.priority

    /**
     * @return true if current role has `write` permission
     */
    fun hasWritePermission() = this.isHigherOrEqualThan(ADMIN)

    /**
     * @return true if current role has `delete` permission
     */
    fun hasDeletePermission() = this.isHigherOrEqualThan(OWNER)

    companion object {
        /**
         * @param springSecurityRole
         * @return [Role] found by [springSecurityRole] using [asSpringSecurityRole]
         */
        fun fromSpringSecurityRole(springSecurityRole: String): Role? = values().find { it.asSpringSecurityRole() == springSecurityRole }
    }
}
