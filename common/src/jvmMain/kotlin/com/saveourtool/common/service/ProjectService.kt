package com.saveourtool.common.service

import com.saveourtool.common.domain.ProjectSaveStatus
import com.saveourtool.common.entities.*
import com.saveourtool.common.filters.ProjectFilter
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.repository.ProjectRepository
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.security.ProjectPermissionEvaluator
import com.saveourtool.common.utils.blockingToMono
import com.saveourtool.common.utils.switchIfEmptyToNotFound

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.*

/**
 * Service for project
 *
 * @param projectRepository
 */
@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
    private val userRepository: UserRepository,
) {
    /**
     * Store [project] in the database
     *
     * @param project a [Project] to store
     * @return project's id, should never return null
     */
    @Suppress("UnsafeCallOnNullableType")
    fun getOrSaveProject(project: Project): Pair<Long, ProjectSaveStatus> {
        val (projectId, projectSaveStatus) = projectRepository.findByNameAndOrganizationName(project.name, project.organization.name)?.let {
            Pair(it.id, ProjectSaveStatus.EXIST)
        } ?: run {
            val savedProject = projectRepository.save(project)
            Pair(savedProject.id, ProjectSaveStatus.NEW)
        }
        requireNotNull(projectId) { "Should have gotten an ID for project from the database" }
        return Pair(projectId, projectSaveStatus)
    }

    /**
     * Mark organization with [project] as [newProjectStatus]
     * Before performing the function, check for user permissions by the [project].
     *
     * @param newProjectStatus is new status for [project]
     * @param project is organization in which the status will be changed
     * @return project
     */
    @Suppress("UnsafeCallOnNullableType")
    fun changeProjectStatus(project: Project, newProjectStatus: ProjectStatus): Project = project
        .apply {
            status = newProjectStatus
        }
        .let {
            projectRepository.save(it)
        }

    /**
     * @param project [Project] to be updated
     * @return updated [project]
     */
    fun updateProject(project: Project): Project = run {
        requireNotNull(project.id) {
            "Project must be taken from DB so it's id must not be null"
        }
        projectRepository.save(project)
    }

    /**
     * @return list of all projects
     */
    fun getProjects(): Flux<Project> = projectRepository.findAll().let { Flux.fromIterable(it) }

    /**
     * @param name
     * @param organization
     */
    @Suppress("KDOC_WITHOUT_RETURN_TAG")  // remove after new release of diktat
    fun findByNameAndOrganization(name: String, organization: Organization) = projectRepository.findByNameAndOrganization(name, organization)

    /**
     * @param name
     * @param organizationName
     * @param statuses
     * @return project by [name], [organizationName] and [statuses]
     */
    fun findByNameAndOrganizationNameAndStatusIn(name: String, organizationName: String, statuses: Set<ProjectStatus>) =
            projectRepository.findByNameAndOrganizationNameAndStatusIn(name, organizationName, statuses)

    /**
     * @param name
     * @param organizationName
     * @return project by [name], [organizationName] and [ProjectStatus.CREATED] status
     */
    fun findByNameAndOrganizationNameAndCreatedStatus(name: String, organizationName: String) =
            findByNameAndOrganizationNameAndStatusIn(name, organizationName, EnumSet.of(ProjectStatus.CREATED))

    /**
     * @param organizationName
     * @return List of the Organization projects
     */
    fun getAllByOrganizationName(organizationName: String) = projectRepository.findByOrganizationName(organizationName)

    /**
     * @param organizationName
     * @return Flux of the Organization projects
     */
    fun getAllAsFluxByOrganizationName(organizationName: String) = getAllByOrganizationName(organizationName).let { Flux.fromIterable(it) }

    /**
     * @param organizationName is [organization] name
     * @param authentication
     * @param statuses is status`s
     * @return list of not deleted projects
     */
    fun getProjectsByOrganizationNameAndStatusIn(
        organizationName: String,
        authentication: Authentication?,
        statuses: Set<ProjectStatus>
    ): Flux<Project> = getAllAsFluxByOrganizationName(organizationName)
        .filter {
            it.status in statuses
        }
        .filter {
            projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ)
        }

    /**
     * @param organizationName
     * @param authentication
     * @return projects by organizationName and [CREATED] status
     */
    fun getProjectsByOrganizationNameAndCreatedStatus(organizationName: String, authentication: Authentication?) =
            getProjectsByOrganizationNameAndStatusIn(organizationName, authentication, EnumSet.of(ProjectStatus.CREATED))

    /**
     * @param projectFilter is filter for [projects]
     * @return project's with filter
     */
    fun getFiltered(projectFilter: ProjectFilter): List<Project> = projectRepository.findAll { root, _, cb ->
        val publicPredicate = projectFilter.public?.let { cb.equal(root.get<Boolean>("public"), it) } ?: cb.and()
        val orgNamePredicate = if (projectFilter.organizationName.isBlank()) {
            cb.and()
        } else {
            cb.equal(root.get<Organization>("organization").get<String>("name"), projectFilter.organizationName)
        }
        val namePredicate = if (projectFilter.name.isBlank()) {
            cb.and()
        } else {
            cb.equal(root.get<String>("name"), projectFilter.name)
        }

        cb.and(
            root.get<ProjectStatus>("status").`in`(projectFilter.statuses),
            publicPredicate,
            orgNamePredicate,
            namePredicate,
        )
    }

    /**
     * @param value is a string for a wrapper to search by match on a string in the database
     * @return string by match on a string in the database
     */
    private fun wrapValue(value: String) = value.let {
        "%$value%"
    }

    /**
     * @param authentication [Authentication] of the user who wants to access the project
     * @param projectName name of the project
     * @param organizationName organization that owns the project
     * @param permission requested [Permission]
     * @param messageIfNotFound if project is not found, include this into 404 response body
     * @param statusIfForbidden return this status if permission is not allowed fot the current user
     * @return `Mono` with project; `Mono.error` if project cannot be accessed by the current user.
     */
    @Transactional(readOnly = true)
    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
    fun findWithPermissionByNameAndOrganization(
        authentication: Authentication,
        projectName: String,
        organizationName: String,
        permission: Permission,
        messageIfNotFound: String? = null,
        statusIfForbidden: HttpStatus = HttpStatus.FORBIDDEN,
    ): Mono<Project> = with(projectPermissionEvaluator) {
        Mono.fromCallable { findByNameAndOrganizationNameAndCreatedStatus(projectName, organizationName) }
            .switchIfEmpty {
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, messageIfNotFound))
            }
            .filterByPermission(authentication, permission, statusIfForbidden)
    }

    /**
     * @param userName
     * @return optional of user
     */
    fun findUserByName(userName: String): User? = userRepository.findByName(userName)

    /**
     * @param id
     * @return [Project] with given [id]
     */
    fun findById(id: Long): Optional<Project> = projectRepository.findById(id)

    /**
     * @param name
     * @param organizationName
     * @param lazyMessage
     * @return [Mono] of [Project] or [Mono.error] if [Project] is not found
     */
    fun projectByCoordinatesOrNotFound(
        name: String,
        organizationName: String,
        lazyMessage: () -> String,
    ): Mono<Project> = blockingToMono {
        findByNameAndOrganizationNameAndCreatedStatus(name, organizationName)
    }
        .switchIfEmptyToNotFound(lazyMessage)
}
