package com.saveourtool.cosv.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.common.entities.*
import com.saveourtool.common.filters.ProjectFilter
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.security.ProjectPermissionEvaluator
import com.saveourtool.common.service.ProjectService
import com.saveourtool.common.utils.*
import com.saveourtool.common.v1

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
import java.util.*

/**
 * Controller for working with projects.
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "projects"),
)
@RestController
@RequestMapping(path = ["/api/$v1/projects"])
class ProjectController(
    private val projectService: ProjectService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
) {
    @GetMapping("/")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get all available projects.",
        description = "Get all projects, available for current user.",
    )
    @ApiResponse(responseCode = "200", description = "Projects successfully fetched.")
    fun getProjects(
        authentication: Authentication,
    ): Flux<Project> = projectService.getProjects()
        .filter {
            projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ)
        }

    @PostMapping("/by-filters")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get projects matching filters",
        description = "Get filtered projects available for the current user.",
    )
    @Parameters(
        Parameter(name = "projectFilter", `in` = ParameterIn.DEFAULT, description = "project filters", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched projects.")
    fun getFilteredProjects(
        @RequestBody projectFilter: ProjectFilter,
        authentication: Authentication?,
    ): Flux<ProjectDto> =
            blockingToFlux { projectService.getFiltered(projectFilter) }
                .filter {
                    projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ)
                }
                .map { it.toDto() }
}
