package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.LnkUserProjectRepository
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.springframework.stereotype.Service

/**
 * Service of lnkUserProjects
 */
@Service
class LnkUserProjectService(private val lnkUserProjectRepository: LnkUserProjectRepository) {
    /**
     * @param project
     * @param role user role in project
     * @return all users with role in project
     */
    fun getAllUsersByProjectAndRole(project: Project, role: Role) = lnkUserProjectRepository.findByProject(project)
        .filter { it.role == role }
        .map { it.user }

    fun findRoleByUserIdAndProject(userId: Long, project: Project) = lnkUserProjectRepository.findByUserIdAndProject(userId, project)
        .map { it.role }
        .ifEmpty { listOf(Role.VIEWER) }
        .singleOrNull()
        ?: throw IllegalStateException("Multiple roles are set for userId=$userId and project=$project")
}
