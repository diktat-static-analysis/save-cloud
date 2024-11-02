package com.saveourtool.save.backend.service

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.Project
import com.saveourtool.common.entities.User
import com.saveourtool.common.permission.SetRoleRequest
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.service.LnkUserProjectService
import com.saveourtool.common.service.ProjectService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.util.function.Tuple2

/**
 * Service for managing user permissions
 */
@Service
class PermissionService(
    private val projectService: ProjectService,
    private val userRepository: UserRepository,
    private val lnkUserProjectService: LnkUserProjectService,
) {
    /**
     * @param user
     * @param project
     * @return role of [user] in [project], may throw exception if data is not consistent
     */
    @Suppress("UnsafeCallOnNullableType")
    fun getRole(user: User, project: Project): Role = lnkUserProjectService.findRoleByUserIdAndProject(user.id!!, project)

    /**
     * @param organizationName
     * @param projectName
     * @param setRoleRequest
     */
    fun setRole(organizationName: String, projectName: String, setRoleRequest: SetRoleRequest): Mono<Unit> =
            findUserAndProject(setRoleRequest.userName, organizationName, projectName)
                .map { (user: User, project: Project) ->
                    lnkUserProjectService.setRole(user, project, setRoleRequest.role)
                }

    /**
     * @param organizationName
     * @param projectName
     * @param userName
     */
    fun removeRole(organizationName: String, projectName: String, userName: String): Mono<Unit> =
            findUserAndProject(userName, organizationName, projectName)
                .map { (user: User, project: Project) ->
                    lnkUserProjectService.removeRole(user, project)
                }

    /**
     * @param userName
     * @param organizationName
     * @param projectName
     * @return `Mono<Tuple2<User, Project>>`
     */
    @Suppress("TYPE_ALIAS")
    internal fun findUserAndProject(userName: String,
                                    organizationName: String,
                                    projectName: String
    ): Mono<Tuple2<User, Project>> = Mono.zip(
        userRepository.findByName(userName).toMono(),
        projectService.findByNameAndOrganizationNameAndCreatedStatus(projectName, organizationName).toMono(),
    )
}
