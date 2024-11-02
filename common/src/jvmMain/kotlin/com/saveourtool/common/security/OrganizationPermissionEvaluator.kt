package com.saveourtool.common.security

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.Organization
import com.saveourtool.common.entities.OrganizationStatus
import com.saveourtool.common.entities.User
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.service.LnkUserOrganizationService
import com.saveourtool.common.service.UserService
import com.saveourtool.common.utils.hasRole
import com.saveourtool.common.utils.username

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * Class that is capable of assessing user's permissions regarding organizations.
 */
@Component
class OrganizationPermissionEvaluator(
    private var lnkUserOrganizationService: LnkUserOrganizationService,
    private var userDetailsService: UserService,
) {
    /**
     * @param authentication
     * @param organization
     * @param role required role
     * @return true if user with [authentication] has [role] in [organization].
     */
    fun hasOrganizationRole(authentication: Authentication?, organization: Organization, role: Role): Boolean {
        authentication ?: return false
        val userName = authentication.username()
        if (authentication.hasRole(Role.SUPER_ADMIN)) {
            return true
        }
        return lnkUserOrganizationService.findRoleByUserNameAndOrganization(userName, organization).isHigherOrEqualThan(role)
    }

    /**
     * @param authentication [Authentication] describing an authenticated request
     * @param organization is organization in which we want to change the status
     * @param newStatus is new status in [organization]
     * @return whether user described by [authentication] can have permission on change [organization] status on [newStatus]
     * @throws IllegalStateException
     */
    fun hasPermissionToChangeStatus(authentication: Authentication?, organization: Organization, newStatus: OrganizationStatus): Boolean {
        val oldStatus = organization.status

        return when {
            oldStatus == newStatus -> throw IllegalStateException("invalid status")
            oldStatus.isBan() || newStatus.isBan() -> hasPermission(authentication, organization, Permission.BAN)
            else -> hasPermission(authentication, organization, Permission.DELETE)
        }
    }

    /**
     * @param authentication [Authentication] describing an authenticated request
     * @param organization
     * @param permission
     * @return whether user described by [authentication] can have [permission] on [organization]
     */
    fun hasPermission(authentication: Authentication?, organization: Organization, permission: Permission): Boolean {
        authentication ?: return permission == Permission.READ
        val userName = authentication.username()
        val user = userDetailsService.getUserByName(userName)
        val userId = user.requiredId()
        if (authentication.hasRole(Role.SUPER_ADMIN)) {
            return true
        }

        val organizationRole = lnkUserOrganizationService.findRoleByUserIdAndOrganization(userId, organization)
        return when (permission) {
            Permission.READ -> hasReadAccess(userId, organizationRole)
            Permission.WRITE -> hasWriteAccess(userId, organizationRole)
            Permission.DELETE -> hasDeleteAccess(userId, organizationRole)
            Permission.BAN -> hasBanAccess(userId, organizationRole)
        }
    }

    /**
     * @param authentication
     * @param organizationName
     * @param requiredRole
     * @return true if user with [authentication] info has [requiredRole] in organization with name [organizationName] or globally
     */
    fun hasGlobalRoleOrOrganizationRole(authentication: Authentication, organizationName: String, requiredRole: Role): Boolean =
            lnkUserOrganizationService.getGlobalRoleOrOrganizationRole(authentication, organizationName).priority >= requiredRole.priority

    @Suppress("FunctionOnlyReturningConstant", "UNUSED_PARAMETER")
    private fun hasReadAccess(userId: Long?, organizationRole: Role): Boolean = true

    private fun hasWriteAccess(userId: Long?, organizationRole: Role): Boolean = hasDeleteAccess(userId, organizationRole) ||
            userId?.let { organizationRole == Role.ADMIN } ?: false

    private fun hasDeleteAccess(userId: Long?, organizationRole: Role): Boolean =
            hasBanAccess(userId, organizationRole) || userId?.let { organizationRole == Role.OWNER } ?: false

    /**
     * Only [Role.SUPER_ADMIN] can ban the project. And a user with such a global role has permissions for all actions.
     * Since we have all the rights issued depending on the following, you need to set [false] here
     */
    @Suppress("FunctionOnlyReturningConstant", "UnusedParameter")
    private fun hasBanAccess(userId: Long?, organizationRole: Role): Boolean = false

    /**
     * In case we widen number of users that can manage roles in an organization, there is a separate method.
     * Simply delegating now.
     *
     * @param organization in which the role is going to be changed
     * @param authentication auth info of a current user
     * @param otherUser user whose role is going to be changed
     * @param requestedRole role that is going to be set
     * @return whether the user can change roles in organization
     */
    @Suppress("UnsafeCallOnNullableType")
    fun canChangeRoles(
        organization: Organization,
        authentication: Authentication,
        otherUser: User,
        requestedRole: Role = Role.NONE
    ): Boolean {
        val selfRole = lnkUserOrganizationService.getGlobalRoleOrOrganizationRole(authentication, organization)
        val otherRole = lnkUserOrganizationService.findRoleByUserNameAndOrganization(otherUser.name, organization)
        return selfRole.isHigherOrEqualThan(Role.OWNER) || selfRole.isHigherOrEqualThan(Role.ADMIN) && hasAnotherUserLessPermissions(selfRole, otherRole) &&
                isRequestedPermissionsCanBeSetByUser(selfRole, requestedRole)
    }

    /**
     * @param organization
     * @param authentication
     */
    fun canCreateContests(
        organization: Organization,
        authentication: Authentication?,
    ): Boolean = authentication?.let {
        organization.canCreateContests && hasGlobalRoleOrOrganizationRole(it, organization.name, contestCreatorMinimalRole)
    } ?: false

    /**
     * @param selfRole
     * @param otherRole
     * @return true if user with [selfRole] has more permissions than user with [otherRole], false otherwise.
     */
    fun hasAnotherUserLessPermissions(selfRole: Role, otherRole: Role): Boolean = selfRole.priority > otherRole.priority

    /**
     * @param selfRole
     * @param requestedRole
     * @return true if [selfRole] is higher than [requestedRole], false otherwise
     */
    fun isRequestedPermissionsCanBeSetByUser(selfRole: Role, requestedRole: Role): Boolean = selfRole.priority > requestedRole.priority

    companion object {
        val contestCreatorMinimalRole = Role.ADMIN
    }
}

private fun OrganizationStatus.isBan(): Boolean =
        this == OrganizationStatus.BANNED
