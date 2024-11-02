package com.saveourtool.common.security

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.Project
import com.saveourtool.common.entities.ProjectStatus
import com.saveourtool.common.entities.User
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.repository.LnkUserProjectRepository
import com.saveourtool.common.service.LnkUserOrganizationService
import com.saveourtool.common.service.LnkUserProjectService
import com.saveourtool.common.service.UserService
import com.saveourtool.common.utils.getHighestRole
import com.saveourtool.common.utils.hasRole
import com.saveourtool.common.utils.username

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Class that is capable of assessing user's permissions regarding projects.
 */
@Component
class ProjectPermissionEvaluator(
    private var lnkUserProjectService: LnkUserProjectService,
    private var lnkUserProjectRepository: LnkUserProjectRepository,
    private var lnkUserOrganizationService: LnkUserOrganizationService,
    private var userDetailsService: UserService,
) {
    /**
     * @param authentication [Authentication] describing an authenticated request
     * @param project is organization in which we want to change the status
     * @param newStatus is new status in [project]
     * @return whether user described by [authentication] can have permission on change [project] status on [newStatus]
     * @throws IllegalStateException
     */
    fun hasPermissionToChangeStatus(authentication: Authentication?, project: Project, newStatus: ProjectStatus): Boolean {
        val oldStatus = project.status

        return when {
            oldStatus == newStatus -> throw IllegalStateException("invalid status")
            oldStatus.isBan() || newStatus.isBan() -> hasPermission(authentication, project, Permission.BAN)
            else -> hasPermission(authentication, project, Permission.DELETE)
        }
    }

    /**
     * @param authentication [Authentication] describing an authenticated request
     * @param project
     * @param permission
     * @return whether user described by [authentication] can have [permission] on project [project]
     */
    fun hasPermission(authentication: Authentication?, project: Project, permission: Permission): Boolean {
        authentication ?: return when (permission) {
            Permission.READ -> project.public
            Permission.BAN, Permission.DELETE, Permission.WRITE -> false
        }
        if (authentication.hasRole(Role.SUPER_ADMIN)) {
            return true
        }

        val userName = authentication.username()
        val user = userDetailsService.getUserByName(userName)
        val userId = user.requiredId()
        val organizationRole = lnkUserOrganizationService.findRoleByUserIdAndOrganization(userId, project.organization)
        val projectRole = lnkUserProjectService.findRoleByUserIdAndProject(userId, project)

        return when (permission) {
            Permission.READ -> project.public || hasReadAccess(userId, projectRole, organizationRole)
            Permission.WRITE -> hasWriteAccess(userId, projectRole, organizationRole)
            Permission.DELETE -> hasDeleteAccess(userId, projectRole, organizationRole)
            Permission.BAN -> hasBanAccess(userId, projectRole, organizationRole)
        }
    }

    /**
     * @param authentication
     * @param permission
     * @param statusIfForbidden
     * @return a [Mono] containing the project or `Mono.error` if project can't or shouldn't be accessed by the current user
     */
    fun Mono<Project?>.filterByPermission(
        authentication: Authentication?,
        permission: Permission,
        statusIfForbidden: HttpStatus,
    ) = switchIfEmpty { Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND)) }
        .cast<Project>()
        .map { actualProject ->
            actualProject to hasPermission(authentication, actualProject, permission)
        }
        .filter { (project, isPermissionGranted) -> project.public || isPermissionGranted }
        .flatMap { (project, isPermissionGranted) ->
            if (isPermissionGranted) {
                Mono.just(project)
            } else {
                // project is public, but current user lacks permissions
                Mono.error(ResponseStatusException(statusIfForbidden))
            }
        }
        .switchIfEmpty {
            // We get here if `!project.public && !isPermissionGranted`, i.e.
            // if project either is not found or shouldn't be visible for current user.
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }

    private fun hasReadAccess(userId: Long?, projectRole: Role, organizationRole: Role): Boolean = hasWriteAccess(userId, projectRole, organizationRole) ||
            userId?.let { projectRole == Role.VIEWER } ?: false

    private fun hasWriteAccess(userId: Long?, projectRole: Role, organizationRole: Role): Boolean = hasDeleteAccess(userId, projectRole, organizationRole) ||
            userId?.let { projectRole == Role.ADMIN } ?: false

    private fun hasDeleteAccess(userId: Long?, projectRole: Role, organizationRole: Role): Boolean =
            hasBanAccess(userId, projectRole, organizationRole) || userId?.let {
                getHighestRole(organizationRole, projectRole) == Role.OWNER
            } ?: false

    /**
     * Only [SUPER_ADMIN] can ban the project. And a user with such a global role has permissions for all actions.
     * Since we have all the rights issued depending on the following, you need to set [false] here
     */
    @Suppress("FunctionOnlyReturningConstant", "UnusedParameter")
    private fun hasBanAccess(userId: Long?, projectRole: Role, organizationRole: Role): Boolean = false

    /**
     * @param authentication
     * @param execution
     * @param permission
     * @return [Mono] containing `true` if the current user is granted [permission] on the project for this [execution] or Mono with `false` otherwise
     */
    fun checkPermissions(authentication: Authentication, execution: Execution, permission: Permission): Mono<Boolean> =
            Mono.justOrEmpty(execution.project)
                .filterByPermission(authentication, permission, HttpStatus.FORBIDDEN)
                .map { true }
                .defaultIfEmpty(false)

    /**
     * @param project in which the role is going to be changed
     * @param authentication auth info of a current user
     * @param otherUser user whose role is going to be changed
     * @param requestedRole role that is going to be set
     * @return true if user can change roles in project and false otherwise
     */
    @Suppress("UnsafeCallOnNullableType")
    fun canChangeRoles(
        project: Project,
        authentication: Authentication,
        otherUser: User,
        requestedRole: Role = Role.NONE
    ): Boolean {
        val selfRole = lnkUserProjectService.getGlobalRoleOrProjectRole(authentication, project)
        val otherUserId = otherUser.id!!
        val otherRole = lnkUserProjectRepository.findByUserIdAndProject(otherUserId, project)?.role ?: Role.NONE
        return isProjectAdminOrHigher(selfRole) &&
                hasAnotherUserLessPermissions(selfRole, otherRole) &&
                isRequestedPermissionsCanBeSetByUser(selfRole, requestedRole)
    }

    /**
     * @param selfRole
     * @param otherRole
     * @return true if [otherRole] has less permissions than [selfRole], false otherwise.
     */
    fun hasAnotherUserLessPermissions(selfRole: Role, otherRole: Role): Boolean = selfRole.priority > otherRole.priority

    /**
     * @param selfRole
     * @param requestedRole
     * @return true if [requestedRole] can be set by user with role [selfRole], false otherwise.
     */
    fun isRequestedPermissionsCanBeSetByUser(selfRole: Role, requestedRole: Role): Boolean = selfRole.priority > requestedRole.priority

    /**
     * @param userRole
     * @return true if [userRole] is [Role.ADMIN] or higher, false otherwise.
     */
    fun isProjectAdminOrHigher(userRole: Role): Boolean = userRole.priority >= Role.ADMIN.priority
}

private fun ProjectStatus.isBan(): Boolean =
        this == ProjectStatus.BANNED
