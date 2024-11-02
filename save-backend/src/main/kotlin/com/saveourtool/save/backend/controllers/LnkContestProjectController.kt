/**
 * Controller for processing links between users and their roles in organizations:
 * 1) to put new roles of users
 * 2) to get users and their roles by organization
 * 3) to remove users from organizations
 */

package com.saveourtool.save.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.common.entities.LnkContestProject
import com.saveourtool.common.entities.contest.ContestResult
import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.service.LnkUserProjectService
import com.saveourtool.common.service.ProjectService
import com.saveourtool.common.utils.*
import com.saveourtool.common.v1
import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.backend.service.*

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Controller for processing links between projects and contests with scores
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "contests"),
)
@RestController
@RequestMapping("/api/$v1/contests")
class LnkContestProjectController(
    private val lnkContestProjectService: LnkContestProjectService,
    private val lnkContestExecutionService: LnkContestExecutionService,
    private val lnkUserProjectService: LnkUserProjectService,
    private val contestService: ContestService,
    private val projectService: ProjectService,
) {
    @GetMapping("/{contestName}/scores")
    @Operation(
        method = "GET",
        summary = "Get scores of all projects in contest.",
        description = "Get scores of all projects in contest.",
    )
    @Parameters(
        Parameter(name = "contestName", `in` = ParameterIn.PATH, description = "name of a contest", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched contest scores.")
    fun getRatingsInContest(
        @PathVariable contestName: String,
    ): Flux<ContestResult> = Flux.fromIterable(lnkContestProjectService.getAllByContestName(contestName))
        .getScores()

    @GetMapping("/{organizationName}/{projectName}/best")
    @Operation(
        method = "GET",
        summary = "Get best contests of a project.",
        description = "Get list of contests in which given project has higher results.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of a project", required = true),
        Parameter(name = "amount", `in` = ParameterIn.PATH, description = "number of contests that will be fetched, default is 4", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched best project contests.")
    fun getBestProjectContests(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(defaultValue = "4") amount: Int,
    ): Flux<ContestResult> = Flux.fromIterable(
        lnkContestProjectService.getByProjectNameAndOrganizationName(projectName, organizationName, amount)
    ).getScores()

    private fun Flux<LnkContestProject>.getScores() = map {
        it to lnkContestProjectService.getBestScoreOfProjectInContestWithName(it.project, it.contest.name)
    }
        .map { (lnkContestProject, score) ->
            lnkContestProject.toContestResult(score)
        }

    @GetMapping("/{contestName}/eligible-projects")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "GET",
        summary = "Get projects that can participate in contest.",
        description = "Get list of user's projects that can participate in a given contest.",
    )
    @Parameters(
        Parameter(name = "contestName", `in` = ParameterIn.PATH, description = "name of a contest", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched projects available for contest.")
    fun getAvailableProjectsForContest(
        @PathVariable contestName: String,
        authentication: Authentication,
    ): Mono<List<String>> = Mono.fromCallable {
        lnkUserProjectService.getProjectsByUserIdAndStatuses(authentication.userId()).filter { it.public }
    }
        .map { userProjects ->
            userProjects to lnkContestProjectService.getProjectsFromListAndContest(contestName, userProjects).map { it.project }
        }
        .map { (projects, projectsFromContest) ->
            projects.minus(projectsFromContest.toSet()).map { "${it.organization.name}/${it.name}" }
        }

    @GetMapping("/{organizationName}/{projectName}/eligible-contests")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "GET",
        summary = "Get contests that can be participated.",
        description = "Get list of contest names that a given project can participate in.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of a project", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched contests available for project.")
    fun getAvailableContestsForProject(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<List<String>> = Mono.fromCallable {
        lnkContestProjectService.getByProjectNameAndOrganizationName(projectName, organizationName, MAX_AMOUNT)
            .mapNotNull { it.contest.id }
    }
        .map { it.toSet() }
        .map { contestIds ->
            if (contestIds.isEmpty()) {
                contestService.findContestsInProgress(MAX_AMOUNT).map { it.name }
            } else {
                contestService.getAllActiveContestsNotFrom(contestIds, MAX_AMOUNT).map { it.name }
            }
        }

    @GetMapping("/{contestName}/executions/{organizationName}/{projectName}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "GET",
        summary = "Get project executions in contest.",
        description = "Get list of execution of a project with given name in contest with given name.",
    )
    @Parameters(
        Parameter(name = "contestName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of a project", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched latest project execution in contest.")
    @ApiResponse(responseCode = "404", description = "Either contest is not found or project is not found or execution is not found.")
    fun getContestExecutionsForProject(
        @PathVariable contestName: String,
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Flux<ExecutionDto> = getContestAndProject(contestName, organizationName, projectName)
        .flatMapIterable { (contest, project) ->
            lnkContestExecutionService.getPageExecutionsByContestAndProject(
                contest,
                project,
                PageRequest.ofSize(MAX_AMOUNT)
            )
        }
        .map {
            it.execution.toDto()
        }

    @GetMapping("/{contestName}/executions/{organizationName}/{projectName}/latest")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "GET",
        summary = "Get latest project execution in contest.",
        description = "Get latest execution of a project with given name in contest with given name.",
    )
    @Parameters(
        Parameter(name = "contestName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of a project", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched latest project execution in contest.")
    @ApiResponse(responseCode = "404", description = "Either contest is not found or project is not found or execution is not found.")
    fun getLatestExecutionOfProjectInContest(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @PathVariable contestName: String,
    ): Mono<ExecutionDto> = getContestAndProject(contestName, organizationName, projectName)
        .flatMap { (contest, project) ->
            lnkContestExecutionService.getLatestExecutionByContestAndProject(contest, project).toMono()
        }
        .switchIfEmptyToNotFound {
            "No executions found for project $organizationName/$projectName in contest $contestName."
        }
        .map {
            it.execution.toDto()
        }

    private fun getContestAndProject(contestName: String, organizationName: String, projectName: String) = blockingToMono {
        contestService.findByName(contestName)
    }
        .switchIfEmptyToNotFound {
            "Could not find contest with name $contestName."
        }
        .zipWith(projectService.findByNameAndOrganizationNameAndCreatedStatus(projectName, organizationName).toMono())
        .switchIfEmptyToNotFound {
            "Could not find project with name $organizationName/$projectName."
        }

    @GetMapping("/{contestName}/enroll")
    @PreAuthorize("isAuthenticated()")
    @RequiresAuthorizationSourceHeader
    @Operation(
        method = "GET",
        summary = "Register for a contest.",
        description = "Register your public project for a contest.",
    )
    @Parameters(
        Parameter(name = "contestName", `in` = ParameterIn.PATH, description = "name of a contest", required = true),
        Parameter(name = "organizationName", `in` = ParameterIn.QUERY, description = "name of an organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.QUERY, description = "name of a project", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully enrolled for a contest.")
    @ApiResponse(responseCode = "403", description = "Not enough permissions to enroll for a contest with given project.")
    @ApiResponse(responseCode = "404", description = "Either given project or given contest was not found.")
    @ApiResponse(responseCode = "409", description = "Only public projects can participate in contests.")
    @Suppress("TOO_LONG_FUNCTION")
    fun enrollForContest(
        @PathVariable contestName: String,
        @RequestParam projectName: String,
        @RequestParam organizationName: String,
        authentication: Authentication,
    ): Mono<StringResponse> = Mono.zip(
        projectService.findWithPermissionByNameAndOrganization(
            authentication,
            projectName,
            organizationName,
            Permission.WRITE,
            "No such project found or not enough permission to see the project",
            HttpStatus.FORBIDDEN,
        ),
        blockingToMono { contestService.findByName(contestName) },
    )
        .switchIfEmptyToNotFound()
        .filter { (project, _) ->
            project.public
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "Private projects cannot be enrolled in the contest"
        }
        .map { (project, contest) ->
            lnkContestProjectService.saveLnkContestProject(project, contest)
        }
        .map {
            if (it) {
                ResponseEntity.ok("You have successfully enrolled for this contest!")
            } else {
                ResponseEntity.ok("You are already enrolled for this contest.")
            }
        }

    @GetMapping("/{contestName}/my-results")
    @PreAuthorize("isAuthenticated()")
    @RequiresAuthorizationSourceHeader
    @Operation(
        method = "GET",
        summary = "Get your best results in contest.",
        description = "Get list of best results of your projects in a given contest.",
    )
    @Parameters(
        Parameter(name = "contestName", `in` = ParameterIn.PATH, description = "name of a contest", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched your best results.")
    @ApiResponse(responseCode = "404", description = "Either given project or given contest was not found.")
    fun getBestResultsInUserProjects(
        @PathVariable contestName: String,
        authentication: Authentication,
    ): Flux<ContestResult> = blockingToMono {
        contestService.findByName(contestName)
    }
        .switchIfEmptyToNotFound {
            "Contest with name $contestName was not found."
        }
        .map { contest ->
            contest to lnkUserProjectService.getProjectsByUserIdAndStatuses(authentication.userId()).map { it.requiredId() }
        }
        .flatMapMany { (contest, projectIds) ->
            blockingToFlux {
                lnkContestExecutionService.getLatestExecutionByContestAndProjectIds(contest, projectIds)
            }
        }
        .map {
            it.toContestResult()
        }

    companion object {
        const val MAX_AMOUNT = 512
    }
}
