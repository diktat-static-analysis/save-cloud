package com.saveourtool.save.backend.service

import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.backend.repository.ProjectProblemRepository
import com.saveourtool.save.backend.repository.ProjectRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.cosv.repository.CosvMetadataRepository
import com.saveourtool.save.entities.ProjectProblem
import com.saveourtool.save.entities.ProjectProblemDto
import com.saveourtool.save.filters.ProjectProblemFilter
import com.saveourtool.save.utils.getByIdOrNotFound
import com.saveourtool.save.utils.orNotFound
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for project problem
 *
 * @property projectRepository
 * @property projectProblemRepository
 * @property cosvMetadataRepository
 * @property userRepository
 */
@Service
class ProjectProblemService(
    private val projectRepository: ProjectRepository,
    private val projectProblemRepository: ProjectProblemRepository,
    private val cosvMetadataRepository: CosvMetadataRepository,
    private val userRepository: UserRepository,
) {
    /**
     * @param projectName name of project
     * @param organizationName name of organization
     * @return list of project problems
     */
    fun getAllProblemsByProjectNameAndProjectOrganizationName(projectName: String, organizationName: String) =
            projectProblemRepository.getAllProblemsByProjectNameAndProjectOrganizationName(projectName, organizationName)

    /**
     * @param id id of project problem
     * @return project problem by id
     */
    fun getProjectProblemById(id: Long) = projectProblemRepository.getByIdOrNotFound(id)

    /**
     * @param problem problem of project
     * @param authentication auth info of a current user
     */
    @Transactional
    fun saveProjectProblem(problem: ProjectProblemDto, authentication: Authentication) {
        val vulnerabilityMetadata = problem.identifier?.let { cosvMetadataRepository.findByCosvId(it) }
        val project = projectRepository.findByNameAndOrganizationName(problem.projectName, problem.organizationName).orNotFound()
        val userId = authentication.userId()
        val user = userRepository.getByIdOrNotFound(userId)

        val projectProblem = ProjectProblem(
            name = problem.name,
            description = problem.description,
            critical = problem.critical,
            cosvMetadata = vulnerabilityMetadata,
            project = project,
            userId = user.requiredId(),
            isClosed = false,
        )
        projectProblemRepository.save(projectProblem)
    }

    /**
     * @param projectProblemDto problem of project
     */
    @Transactional
    fun updateProjectProblem(projectProblemDto: ProjectProblemDto) {
        val problem = projectProblemDto.id?.let { projectProblemRepository.getByIdOrNotFound(it) }.orNotFound()
        val vulnerabilityMetadata = projectProblemDto.identifier?.let { cosvMetadataRepository.findByCosvId(it) }
        problem.apply {
            name = projectProblemDto.name
            description = projectProblemDto.description
            critical = projectProblemDto.critical
            isClosed = projectProblemDto.isClosed
            cosvMetadata = vulnerabilityMetadata
        }
        projectProblemRepository.save(problem)
    }

    /**
     * @param projectProblemFilter
     * @return list of project problems
     */
    fun getFiltered(projectProblemFilter: ProjectProblemFilter): List<ProjectProblem> =
            with(projectProblemFilter) {
                projectProblemRepository.getAllProblemsByProjectNameAndProjectOrganizationNameAndIsClosed(projectName, organizationName, isClosed)
            }
}
