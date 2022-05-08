package org.cqfn.save.backend.security

import org.cqfn.save.backend.service.LnkUserOrganizationService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Organization
import org.cqfn.save.permission.Permission
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * Class that is capable of assessing user's permissions regarding organizations.
 */
@Component
class OrganizationPermissionEvaluator {
    @Autowired
    private lateinit var lnkUserOrganizationService: LnkUserOrganizationService

    /**
     * @param authentication [Authentication] describing an authenticated request
     * @param organization
     * @param permission
     * @return whether user described by [authentication] can have [permission] on [organization]
     */
    fun hasPermission(authentication: Authentication?, organization: Organization, permission: Permission): Boolean {
        authentication ?: return false
        val userId = (authentication.details as AuthenticationDetails).id
        if (authentication.hasRole(Role.SUPER_ADMIN)) {
            return true
        }

        val organizationRole = lnkUserOrganizationService.findRoleByUserIdAndOrganization(userId, organization)
        return when (permission) {
            Permission.READ -> hasReadAccess(userId, organizationRole)
            Permission.WRITE -> hasWriteAccess(userId, organizationRole)
            Permission.DELETE -> hasDeleteAccess(userId, organizationRole)
        }
    }

    private fun Authentication.hasRole(role: Role): Boolean = authorities.any { it.authority == role.asSpringSecurityRole() }

    private fun hasReadAccess(userId: Long?, organizationRole: Role): Boolean =
            userId?.let { organizationRole.priority >= Role.VIEWER.priority } ?: false

    private fun hasWriteAccess(userId: Long?, organizationRole: Role): Boolean =
            userId?.let { organizationRole.priority >= Role.ADMIN.priority } ?: false

    private fun hasDeleteAccess(userId: Long?, organizationRole: Role): Boolean =
            userId?.let { organizationRole.priority >= Role.OWNER.priority } ?: false

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

    /**
     * @param userRole
     * @return true if [userRole] is [Role.ADMIN] or higher, false otherwise
     */
    fun isOrganizationAdminOrHigher(userRole: Role): Boolean = userRole.priority >= Role.ADMIN.priority
}
