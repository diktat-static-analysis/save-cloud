package org.cqfn.save.backend.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.cqfn.save.backend.configs.ApiSwaggerSupport
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.service.PermissionService
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.User
import org.cqfn.save.permission.Permission
import org.cqfn.save.permission.SetRoleRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
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
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@ApiSwaggerSupport
@Tags(Tag(name = "api"), Tag(name = "permissions"))
@RestController
@RequestMapping("/api/projects/roles")
@Suppress("MISSING_KDOC_ON_FUNCTION")
class PermissionController(
    private val permissionService: PermissionService,
    private val organizationService: OrganizationService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
) {
    @GetMapping("/{organizationName}/{projectName}")
    @Operation(
        description = "Get role for a user on a particular project",
        parameters = [
            Parameter(`in` = ParameterIn.HEADER, name = "X-Authorization-Source", required = true),
        ]
    )
    @ApiResponse(responseCode = "200", description = "Permission added")
    @ApiResponse(responseCode = "404", description = "Requested user or project doesn't exist or the project is hidden from the current user")
    fun getRole(@PathVariable organizationName: String,
                @PathVariable projectName: String,
                @RequestParam userName: String,
                authentication: Authentication,
    ): Mono<Role> = permissionService.findUserAndProject(userName, organizationName, projectName)
        .filter { (_, project) ->
            // roles are available for all VIEWERs of public project and for any member of private project
            projectPermissionEvaluator.hasPermission(authentication, project, Permission.READ)
        }
        .map { (user: User, project: Project) ->
            permissionService.getRole(user, project)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }

    @PostMapping("/{organizationName}/{projectName}")
    @Operation(
        description = "Set role for a user on a particular project",
        parameters = [
            Parameter(`in` = ParameterIn.HEADER, name = "X-Authorization-Source", required = true),
        ]
    )
    @ApiResponse(responseCode = "200", description = "Permission added")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this organization members")
    @ApiResponse(responseCode = "404", description = "Requested user or project doesn't exist")
    @PreAuthorize("@organizationService.canChangeRoles(#organizationName, #authentication.details.id)")
    fun setRole(@PathVariable organizationName: String,
                @PathVariable projectName: String,
                @RequestBody setRoleRequest: SetRoleRequest,
                authentication: Authentication,
    ) = permissionService.addRole(organizationName, projectName, setRoleRequest)
        .switchIfEmpty {
            logger.info("Attempt to perform role update $setRoleRequest, but user or organization was not found")
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }

    companion object {
        @JvmStatic private val logger = LoggerFactory.getLogger(PermissionController::class.java)
    }
}
