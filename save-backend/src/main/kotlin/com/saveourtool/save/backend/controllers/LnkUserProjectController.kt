/**
 * Controller for processing links between users and their roles in projects:
 * 1) to put new roles of users
 * 2) to get users and their roles by project
 * 3) to remove users from projects
 */

package com.saveourtool.save.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.security.ProjectPermissionEvaluator
import com.saveourtool.common.service.LnkUserProjectService
import com.saveourtool.common.service.ProjectService
import com.saveourtool.common.utils.switchIfEmptyToNotFound
import com.saveourtool.common.v1
import com.saveourtool.save.authservice.utils.userId

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Controller for processing links between users and their roles in projects
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "projects"),
)
@RestController
@RequestMapping("/api/$v1/projects")
class LnkUserProjectController(
    private val lnkUserProjectService: LnkUserProjectService,
    private val projectService: ProjectService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
) {
    @GetMapping(path = ["/get-for-current-user"])
    @RequiresAuthorizationSourceHeader
    @Operation(
        method = "GET",
        summary = "Get projects of current authenticated user",
        description = "Get list of projects related to current user",
    )
    @PreAuthorize("permitAll()")
    @ApiResponse(responseCode = "200", description = "Successfully fetched users from project.")
    fun getProjectsOfCurrentUser(authentication: Authentication): Flux<ProjectDto> = Flux.fromIterable(
        lnkUserProjectService.getProjectsByUserIdAndStatuses(authentication.userId())
    )
        .filter {
            it.public
        }
        .map { it.toDto() }

    @GetMapping(path = ["/{organizationName}/{projectName}/users"])
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get users from project with their roles.",
        description = "Get list of users that are connected with given project and their roles in it.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of a project", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched users from project.")
    @ApiResponse(responseCode = "404", description = "Project with such name was not found.")
    fun getAllUsersByProjectNameAndOrganizationName(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<List<UserInfo>> = projectService.findByNameAndOrganizationNameAndCreatedStatus(projectName, organizationName)
        .toMono()
        .switchIfEmptyToNotFound {
            "No project with name $projectName was found."
        }
        .filter {
            projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ)
        }
        .map { project ->
            lnkUserProjectService.getAllUsersAndRolesByProject(project)
        }
        .map { users ->
            users.map { (user, role) ->
                user.toUserInfo(mapOf("$organizationName/$projectName" to role))
            }
        }
        .defaultIfEmpty(emptyList())

    @GetMapping("/{organizationName}/{projectName}/users/not-from")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "GET",
        summary = "Get users not from project.",
        description = "Get list of users that are not connected with given project.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of a project", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched users not from project.")
    @ApiResponse(responseCode = "404", description = "Project with such name was not found or considered to be private.")
    @Suppress("TOO_LONG_FUNCTION")
    fun getAllUsersNotFromProjectWithNamesStartingWith(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam prefix: String,
        authentication: Authentication,
    ): Mono<List<UserInfo>> = Mono.zip(
        Mono.just(organizationName),
        Mono.just(projectName),
    )
        .filter {
            prefix.isNotEmpty()
        }
        .flatMap { (organizationName, projectName) ->
            projectService.findByNameAndOrganizationNameAndCreatedStatus(projectName, organizationName).toMono()
        }
        .switchIfEmptyToNotFound {
            "No project with name $projectName was found in organization $organizationName."
        }
        .filter { project ->
            projectPermissionEvaluator.hasPermission(authentication, project, Permission.READ)
        }
        .switchIfEmptyToNotFound {
            "No project with name $projectName was found in organization $organizationName."
        }
        .map { project ->
            lnkUserProjectService.getAllUsersByProject(project)
        }
        .map { users ->
            users.map { it.requiredId() }.toSet()
        }
        .map { projectUserIds ->
            projectUserIds to lnkUserProjectService.getNonProjectUsersByName(prefix, projectUserIds)
        }
        .map { (projectUserIds, exactMatchUsers) ->
            exactMatchUsers to
                    lnkUserProjectService.getNonProjectUsersByNamePrefix(
                        prefix,
                        projectUserIds + exactMatchUsers.map { it.requiredId() },
                        PAGE_SIZE - exactMatchUsers.size,
                    )
        }
        .map { (exactMatchUsers, prefixUsers) ->
            (exactMatchUsers + prefixUsers).map { it.toUserInfo() }
        }
        .defaultIfEmpty(emptyList())

    companion object {
        const val PAGE_SIZE = 5
    }
}
