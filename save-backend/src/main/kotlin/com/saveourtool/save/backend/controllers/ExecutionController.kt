package com.saveourtool.save.backend.controllers

import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.Project
import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.execution.ExecutionUpdateDto
import com.saveourtool.common.execution.TestingType
import com.saveourtool.common.filters.ExecutionFilter
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.security.ProjectPermissionEvaluator
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.service.ProjectService
import com.saveourtool.common.utils.blockingMap
import com.saveourtool.common.utils.orNotFound
import com.saveourtool.common.utils.switchIfEmptyToNotFound
import com.saveourtool.common.v1
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.storage.ExecutionInfoStorage
import com.saveourtool.save.backend.utils.toMonoOrNotFound
import com.saveourtool.save.core.utils.runIf

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.GroupedFlux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.util.concurrent.atomic.AtomicBoolean

import kotlinx.datetime.toJavaLocalDateTime

/**
 * Controller that accepts executions
 */
@RestController
@Suppress("LongParameterList")
class ExecutionController(private val executionService: ExecutionService,
                          private val projectService: ProjectService,
                          private val projectPermissionEvaluator: ProjectPermissionEvaluator,
                          private val organizationService: OrganizationService,
                          private val executionInfoStorage: ExecutionInfoStorage,
) {
    private val log = LoggerFactory.getLogger(ExecutionController::class.java)

    /**
     * @param executionUpdateDto
     * @return empty Mono
     */
    @PostMapping("/internal/updateExecutionByDto")
    fun updateExecution(@RequestBody executionUpdateDto: ExecutionUpdateDto): Mono<Unit> = Mono.fromCallable {
        executionService.updateExecutionStatus(
            executionService.findExecution(executionUpdateDto.id).orNotFound(),
            executionUpdateDto.status
        )
    }.flatMap {
        executionInfoStorage.upsertIfRequired(executionUpdateDto)
    }

    /**
     * Get execution by id
     *
     * @param id id of execution
     * @param authentication
     * @return execution if it has been found
     */
    @GetMapping(path = ["/api/$v1/execution", "/internal/execution"])
    @Transactional(readOnly = true)
    @Suppress("UnsafeCallOnNullableType")
    fun getExecution(
        @RequestParam id: Long,
        authentication: Authentication?
    ): Mono<Execution> = executionService.findExecution(id).toMonoOrNotFound("Execution with id=$id is not found")
        .runIf({ authentication != null }) {
            filterWhen { execution -> projectPermissionEvaluator.checkPermissions(authentication!!, execution, Permission.READ) }
        }

    /**
     * @param executionId
     * @return execution dto
     */
    @GetMapping(path = ["/internal/executionDto"])
    fun getExecutionDto(
        @RequestParam executionId: Long,
    ): Mono<ExecutionDto> =
            executionService.findExecution(executionId)
                .toMonoOrNotFound()
                .map { it.toDto() }

    /**
     * @param executionId
     * @param authentication
     * @return execution dto
     */
    @GetMapping(path = ["/api/$v1/executionDto"])
    fun getExecutionDto(@RequestParam executionId: Long, authentication: Authentication): Mono<ExecutionDto> =
            executionService.findExecution(executionId)
                .toMonoOrNotFound()
                .filterWhen { execution -> projectPermissionEvaluator.checkPermissions(authentication, execution, Permission.READ) }
                .map { it.toDto() }

    /**
     * @param projectName
     * @param authentication
     * @param organizationName
     * @param filters
     * @return list of execution dtos
     */
    @Suppress("PARAMETER_NAME_IN_OUTER_LAMBDA")
    @PostMapping(path = ["/api/$v1/executionDtoList"])
    fun getExecutionByProject(
        @RequestParam projectName: String,
        @RequestParam organizationName: String,
        @RequestBody(required = false) filters: ExecutionFilter?,
        authentication: Authentication,
    ): Mono<List<ExecutionDto>> = organizationService.findByNameAndCreatedStatus(organizationName)
        .toMono()
        .switchIfEmptyToNotFound {
            "Organization with name [$organizationName] was not found."
        }
        .zipWhen { organization ->
            projectService.findWithPermissionByNameAndOrganization(authentication, projectName, organization.name, Permission.READ)
        }
        .map { (organization, _) ->
            filters?.let {
                val startTime = filters.startTime.toJavaLocalDateTime()
                val endTime = filters.endTime.toJavaLocalDateTime()
                executionService.getExecutionByNameAndOrganizationAndStartTimeBetween(
                    projectName,
                    organization,
                    startTime,
                    endTime,
                )
            } ?: executionService.getExecutionByNameAndOrganization(projectName, organization)
        }
        .map {
            it.filter {execution ->
                execution.type != TestingType.CONTEST_MODE
            }.map { execution ->
                execution.toDto()
            }
                .reversed()
        }

    /**
     * Get latest (by start time an) execution by project name and organization
     *
     * @param name project name
     * @param organizationName
     * @param authentication
     * @return Execution
     * @throws ResponseStatusException if execution is not found
     */
    @GetMapping(path = ["/api/$v1/latestExecution"])
    fun getLatestExecutionForProject(@RequestParam name: String, @RequestParam organizationName: String, authentication: Authentication): Mono<ExecutionDto> =
            Mono.justOrEmpty(
                executionService.getLatestExecutionByProjectNameAndProjectOrganizationName(name, organizationName)
            )
                .switchIfEmpty {
                    Mono.error(ResponseStatusException(HttpStatus.NO_CONTENT))
                }
                .filterWhen { projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ) }
                .map { it.toDto() }

    /**
     * Delete all, except participating in contests, executions, by project name and organization
     *
     * @param name name of project
     * @param organizationName organization of project
     * @param authentication
     * @return ResponseEntity
     */
    @PostMapping(path = ["/api/$v1/execution/delete-all-except-contest"])
    @Suppress("UnsafeCallOnNullableType")
    fun deleteExecutionForProject(
        @RequestParam name: String,
        @RequestParam organizationName: String,
        authentication: Authentication,
    ): Mono<ResponseEntity<*>> = projectService.findWithPermissionByNameAndOrganization(
        authentication,
        name,
        organizationName,
        Permission.DELETE,
        messageIfNotFound = "Could not find the project with name: $name and owner: $organizationName or related objects",
    )
        .blockingMap { project ->
            executionService.deleteExecutionExceptParticipatingInContestsByProjectNameAndProjectOrganization(project.name, project.organization)
            ResponseEntity.ok().build<String>()
        }

    /**
     * Batch delete executions by a list of IDs.
     *
     * @param executionIds list of ids
     * @param authentication
     * @return ResponseEntity:
     * - status 200 if all executions have been deleted or some have been deleted and some are missing
     * - status 400 if all (not missing) executions don't belong to the same project
     * - status 403 if operation is not permitted **at least for some executions**
     * - status 404 if all executions are missing or the project is hidden from the current user
     * @throws ResponseStatusException
     */
    @PostMapping(path = ["/api/$v1/execution/delete"])
    @Suppress("TOO_LONG_FUNCTION", "NonBooleanPropertyPrefixedWithIs")
    fun deleteExecutionsByExecutionIds(@RequestParam executionIds: List<Long>, authentication: Authentication): Mono<ResponseEntity<*>> {
        val isProjectHidden = AtomicBoolean(false)
        return Flux.fromIterable(executionIds)
            .findPresentExecutions()
            .groupBy { it.project }
            .singleOrEmpty()
            .switchIfEmpty {
                Mono.error(ResponseStatusException(HttpStatus.BAD_REQUEST, "Some executions belong to different projects, can't delete all at once"))
            }
            .flatMapMany { groupedFlux: GroupedFlux<Project, Execution> ->
                val project = groupedFlux.key()
                groupedFlux.flatMap { execution ->
                    with(projectPermissionEvaluator) {
                        Mono.justOrEmpty(execution.project).filterByPermission(
                            authentication, Permission.DELETE, HttpStatus.FORBIDDEN
                        )
                    }
                        .map { execution }
                        .doOnError(ResponseStatusException::class.java) {
                            log.warn("Cannot delete execution id=${execution.id}, because operation is not allowed on project id=${project.id} (status ${it.status})")
                            if (it.status == HttpStatus.NOT_FOUND) {
                                isProjectHidden.set(true)
                            }
                        }
                }
            }
            .mapNotNull<Long> { it.id }
            .collectList()
            .map { filteredExecutionIds ->
                // at this point we should have only present executions from a project, that user has access to
                executionService.deleteByIds(filteredExecutionIds)
                if (filteredExecutionIds.isNotEmpty()) {
                    ResponseEntity.ok().build<Void>()
                } else if (isProjectHidden.get()) {
                    // all executions belong to a project, that the user is not allowed to see
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body("All executions are missing")
                } else {
                    ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                }
            }
    }

    /**
     * @return Flux of executions, that are present by ID; or `Flux.error` with status 404 if all executions are missing
     */
    private fun Flux<Long>.findPresentExecutions(): Flux<Execution> = collectMap({ id -> id }) { id -> executionService.findExecution(id) }
        .flatMapMany { idsToExecutions ->
            idsToExecutions.filterValues { it == null }.takeIf { it.isNotEmpty() }?.let { missingExecutions ->
                log.warn("Cannot delete executions with ids=${missingExecutions.keys} because they are missing in the DB")
                if (missingExecutions.size == idsToExecutions.size) {
                    return@flatMapMany Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND, "All executions are missing"))
                }
            }
            Flux.fromIterable(idsToExecutions.mapValues { it.value }.values)
        }
}
