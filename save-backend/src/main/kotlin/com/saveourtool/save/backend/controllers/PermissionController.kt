package com.saveourtool.save.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.common.domain.Role
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.permission.SetRoleRequest
import com.saveourtool.common.security.OrganizationPermissionEvaluator
import com.saveourtool.common.security.ProjectPermissionEvaluator
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.service.ProjectService
import com.saveourtool.common.utils.switchIfEmptyToNotFound
import com.saveourtool.common.utils.trace
import com.saveourtool.common.v1
import com.saveourtool.save.authservice.utils.username
import com.saveourtool.save.backend.service.PermissionService

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@ApiSwaggerSupport
@Tags(
    Tag(name = "projects"),
    Tag(name = "roles"),
)
@RestController
@RequestMapping(path = ["/api/$v1"])
@Suppress("MISSING_KDOC_TOP_LEVEL")
class PermissionController(
    private val projectService: ProjectService,
    private val permissionService: PermissionService,
    private val organizationService: OrganizationService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
) {
    @GetMapping("/projects/{organizationName}/{projectName}/users/roles")
    @RequiresAuthorizationSourceHeader
    @Operation(
        method = "GET",
        summary = "Get role for a user on a particular project.",
        description = "If userName is not present, then will return the role of current user in given project, " +
                "otherwise will return role of user with name userName in project with name projectName.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization in which given project is in", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of a project", required = true),
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "name of a user that is being requested", required = false)
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched user's role")
    @ApiResponse(
        responseCode = "404", description = "Requested user or project doesn't exist or the user doesn't have enough permissions " +
                "(i.e. project is hidden from the current user)"
    )
    @Suppress("UnsafeCallOnNullableType")
    fun getRole(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(required = false) userName: String?,
        authentication: Authentication,
    ): Mono<Role> = getUserAndProjectOrNotFound(userName ?: authentication.username(), projectName, organizationName, authentication)
        .map { (user, project) ->
            permissionService.getRole(user, project)
                .also {
                    logger.trace {
                        "User ${user.name} has role $it"
                    }
                }
        }
        .switchIfEmptyToNotFound()

    @PostMapping("/projects/{organizationName}/{projectName}/users/roles")
    @RequiresAuthorizationSourceHeader
    @Operation(
        method = "POST",
        summary = "Set role for a user on a particular project",
        description = "Set role for a user on a particular project",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization in which given project is in", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of a project", required = true),
        Parameter(name = "setRoleRequest", `in` = ParameterIn.PATH, description = "setRoleRequest passed through body", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Permission added")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or project doesn't exist")
    fun setRole(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestBody setRoleRequest: SetRoleRequest,
        authentication: Authentication,
    ) = getUserAndProjectOrNotFound(setRoleRequest.userName, projectName, organizationName, authentication)
        .filter { (user, project) ->
            // fixme: could be `@PreAuthorize`, but organizationService cannot be found smh
            val organization = organizationService.findByNameAndCreatedStatus(organizationName)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
            val hasOrganizationPermissions = organizationPermissionEvaluator.canChangeRoles(organization, authentication, user, setRoleRequest.role)
            val hasProjectPermissions = projectPermissionEvaluator.canChangeRoles(project, authentication, user, setRoleRequest.role)
            hasOrganizationPermissions || hasProjectPermissions
        }
        .flatMap {
            permissionService.setRole(organizationName, projectName, setRoleRequest)
        }
        .switchIfEmpty {
            logger.info("Attempt to perform role update $setRoleRequest with insufficient permissions")
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }

    @DeleteMapping("/projects/{organizationName}/{projectName}/users/roles/{userName}")
    @RequiresAuthorizationSourceHeader
    @Operation(
        method = "DELETE",
        summary = "Removes user's role on a particular project",
        description = "Removes user's role on a particular project",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization in which given project is in", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of a project", required = true),
        Parameter(name = "userName", `in` = ParameterIn.PATH, description = "username", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Permission removed")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or project doesn't exist")
    fun removeRole(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @PathVariable userName: String,
        authentication: Authentication,
    ) = getUserAndProjectOrNotFound(userName, projectName, organizationName, authentication)
        .filter { (user, project) ->
            val organization = organizationService.findByNameAndCreatedStatus(organizationName)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
            val hasOrganizationPermissions = organizationPermissionEvaluator.canChangeRoles(organization, authentication, user)
            val hasProjectPermissions = projectPermissionEvaluator.canChangeRoles(project, authentication, user)
            hasOrganizationPermissions || hasProjectPermissions
        }
        .switchIfEmpty {
            logger.info("Attempt to remove $userName from $organizationName/$projectName with insufficient permissions")
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .flatMap {
            permissionService.removeRole(organizationName, projectName, userName)
        }
        .switchIfEmptyToNotFound()

    private fun getUserAndProjectOrNotFound(
        userName: String,
        projectName: String,
        organizationName: String,
        authentication: Authentication?,
    ) = Mono.just(userName)
        .filter {
            it.isNotBlank()
        }
        .switchIfEmptyToNotFound {
            USER_NOT_FOUND_ERROR_MESSAGE
        }
        .zipWith(
            projectService.findByNameAndOrganizationNameAndCreatedStatus(projectName, organizationName).toMono()
        )
        .filter { (_, project) ->
            // if project is hidden from the user, who attempts permission update,
            // then we should return 404
            projectPermissionEvaluator.hasPermission(authentication, project, Permission.READ)
        }
        .switchIfEmptyToNotFound {
            PROJECT_NOT_FOUND_ERROR_MESSAGE
        }
        .flatMap { (userName, project) ->
            Mono.zip(
                projectService.findUserByName(userName).toMono(),
                project.toMono()
            )
        }
        .switchIfEmptyToNotFound {
            USER_NOT_FOUND_ERROR_MESSAGE
        }

    companion object {
        @JvmStatic private val logger = LoggerFactory.getLogger(PermissionController::class.java)
        private const val PROJECT_NOT_FOUND_ERROR_MESSAGE = "Project with such name was not found."
        private const val USER_NOT_FOUND_ERROR_MESSAGE = "User with such name was not found."
    }
}
