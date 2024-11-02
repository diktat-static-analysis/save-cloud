package com.saveourtool.common.service

import com.saveourtool.common.domain.OrganizationSaveStatus
import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.Organization
import com.saveourtool.common.entities.OrganizationStatus
import com.saveourtool.common.entities.ProjectStatus
import com.saveourtool.common.entities.ProjectStatus.CREATED
import com.saveourtool.common.filters.OrganizationFilter
import com.saveourtool.common.info.UserPermissions
import com.saveourtool.common.info.UserPermissionsInOrganization
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.repository.LnkUserOrganizationRepository
import com.saveourtool.common.repository.OrganizationRepository
import com.saveourtool.common.utils.AvatarType
import com.saveourtool.common.utils.hasRole
import com.saveourtool.common.utils.orNotFound
import com.saveourtool.common.utils.username
import com.saveourtool.common.validation.isValidLengthName
import org.jetbrains.annotations.Blocking
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service for organization
 */
@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val lnkUserOrganizationRepository: LnkUserOrganizationRepository,
    private val projectService: ProjectService,
    private val userDetailsService: UserService,
) {
    /**
     * Store [organization] in the database
     *
     * @param organization an [Organization] to store
     * @return organization's id, should never return null
     */
    @Suppress("UnsafeCallOnNullableType", "TooGenericExceptionCaught")
    @Transactional
    fun saveOrganization(organization: Organization): Pair<Long, OrganizationSaveStatus> {
        val (organizationId, organizationSaveStatus) = if (!organization.name.isValidLengthName()) {
            Pair(0L, OrganizationSaveStatus.INVALID_NAME)
        } else if (organizationRepository.validateName(organization.name) != 0L) {
            organizationRepository.saveHighLevelName(organization.name)
            Pair(organizationRepository.save(organization).id, OrganizationSaveStatus.NEW)
        } else {
            Pair(0L, OrganizationSaveStatus.CONFLICT)
        }
        requireNotNull(organizationId) { "Should have gotten an ID for organization from the database" }
        return Pair(organizationId, organizationSaveStatus)
    }

    /**
     * @param name
     * @return organization with [name]
     */
    fun getOrganizationByName(name: String): Organization = organizationRepository.findByName(name).orNotFound { "Organization with name: $name not found" }

    /**
     * @param id
     * @return organization with [id]
     */
    fun getOrganizationById(id: Long): Organization = organizationRepository.findByIdOrNull(id).orNotFound { "Organization with id: $id not found" }

    /**
     * @param authentication
     * @param organizationName name of organization
     * @param permission
     * @return true if [authentication] has [permission] in [organizationName], otherwise -- false
     */
    @Blocking
    fun hasPermissionInOrganization(
        authentication: Authentication?,
        organizationName: String,
        permission: Permission,
    ): Boolean {
        authentication ?: return permission == Permission.READ
        val userName = authentication.username()
        val user = userDetailsService.getUserByName(userName)
        val userId = user.requiredId()
        if (authentication.hasRole(Role.SUPER_ADMIN)) {
            return true
        }

        val organization = getOrganizationByName(organizationName)
        val organizationRole = organization.id?.let { lnkUserOrganizationRepository.findByUserIdAndOrganizationId(userId, it)?.role }
        organizationRole ?: return permission == Permission.READ
        return when (permission) {
            Permission.READ -> hasReadAccess(userId, organizationRole)
            Permission.WRITE -> hasWriteAccess(userId, organizationRole)
            Permission.DELETE -> hasDeleteAccess(userId, organizationRole)
            Permission.BAN -> hasBanAccess(userId, organizationRole)
        }
    }

    /**
     * @param authentication
     * @param organizationName
     * @return UserPermissions
     */
    fun getUserPermissionsByOrganizationName(
        authentication: Authentication,
        organizationName: String,
    ): UserPermissions {
        val lnkOrganization = lnkUserOrganizationRepository.findByUserNameAndOrganizationStatusAndOrganizationName(authentication.username(), OrganizationStatus.CREATED,
            organizationName)

        val isPermittedCreateContest = lnkOrganization?.organization?.canCreateContests ?: false
        val isPermittedToBulkUpload = lnkOrganization?.organization?.canBulkUpload ?: false

        return UserPermissions(
            mapOf(organizationName to UserPermissionsInOrganization(isPermittedCreateContest, isPermittedToBulkUpload)),
        )
    }

    /**
     * @param organizationFilter
     * @param pageable [Pageable]
     * @return list of organizations with that match [organizationFilter]
     */
    fun getFiltered(organizationFilter: OrganizationFilter, pageable: Pageable): List<Organization> = organizationRepository.findByNameStartingWithAndStatusIn(
        organizationFilter.prefix,
        organizationFilter.statuses,
        pageable,
    )

    /**
     * @param organization
     * @return organization
     */
    fun updateOrganization(organization: Organization): Organization = organizationRepository.save(organization)

    /**
     * @param name
     * @param statuses
     * @return organization by name and statuses
     */
    fun findByNameAndStatuses(name: String, statuses: Set<OrganizationStatus>) =
            organizationRepository.findByNameAndStatusIn(name, statuses)

    /**
     * @param name
     * @return organization by name with [CREATED] status
     */
    fun findByNameAndCreatedStatus(name: String) = findByNameAndStatuses(name, EnumSet.of(OrganizationStatus.CREATED))

    /**
     * Mark organization [organization] as deleted
     *
     * @param organization an [Organization] to delete
     * @return deleted organization
     */
    fun deleteOrganization(organization: Organization): Organization = changeOrganizationStatus(organization, OrganizationStatus.DELETED)

    /**
     * @param organizationName
     * @param authentication
     * @return global rating of organization by name [organizationName] based on ratings of all projects under this organization
     */
    fun getGlobalRating(organizationName: String, authentication: Authentication?) =
            projectService.getProjectsByOrganizationNameAndCreatedStatus(organizationName, authentication)
                .collectList()
                .map { projectsList ->
                    projectsList.sumOf { it.contestRating }
                }

    /**
     * Mark organization with [organization] as created.
     * If an organization was previously banned, then all its projects become deleted.
     *
     * @param organization an [Organization] to create
     * @return recovered organization
     */
    @Transactional
    fun recoverOrganization(organization: Organization): Organization {
        if (organization.status == OrganizationStatus.BANNED) {
            projectService.getAllByOrganizationName(organization.name).forEach {
                it.status = ProjectStatus.DELETED
                projectService.updateProject(it)
            }
        }
        return changeOrganizationStatus(organization, OrganizationStatus.CREATED)
    }

    /**
     * @param name
     * @return organization by name
     * @throws NoSuchElementException
     */
    fun getByName(name: String) = findByNameAndCreatedStatus(name)
        ?: throw NoSuchElementException("There is no organization with name $name.")

    /**
     * @return all organizations that were registered in SAVE
     */
    fun findAll(): List<Organization> = organizationRepository.findAll()

    /**
     * Mark organization with [organization] and all its projects as banned.
     *
     * @param organization an [Organization] to ban
     * @return banned organization
     */
    @Transactional
    fun banOrganization(organization: Organization): Organization {
        projectService.getAllByOrganizationName(organization.name).forEach {
            it.status = ProjectStatus.BANNED
            projectService.updateProject(it)
        }
        return changeOrganizationStatus(organization, OrganizationStatus.BANNED)
    }

    /**
     * @param organizationName the unique name of the organization.
     * @return `true` if this organization has at least one non-deleted project,
     *   `false` otherwise.
     */
    fun hasProjects(organizationName: String): Boolean =
            projectService.getAllByOrganizationName(organizationName).any { project ->
                project.status == CREATED
            }

    /**
     * Mark organization with [organization] as [newProjectStatus].
     * Before performing the function, check for user permissions by the [organization].
     *
     * @param newProjectStatus is new status for [organization]
     * @param organization is organization in which the status will be changed
     * @return organization
     */
    @Suppress("UnsafeCallOnNullableType")
    private fun changeOrganizationStatus(organization: Organization, newProjectStatus: OrganizationStatus): Organization = organization
        .apply {
            status = newProjectStatus
        }
        .let {
            organizationRepository.save(it)
        }

    /**
     * We change the version just to work-around the caching on the frontend
     *
     * @param name
     * @return the id (version) of new avatar
     * @throws NoSuchElementException
     */
    fun updateAvatarVersion(name: String): String {
        val organization = organizationRepository.findByName(name).orNotFound()
        var version = organization.avatar?.substringAfterLast("?")?.toInt() ?: 0
        val newAvatar = "${AvatarType.ORGANIZATION.toUrlStr(name)}?${++version}"

        organization.apply {
            avatar = newAvatar
        }.orNotFound { "Organization with name [$name] was not found." }
        organization.let { organizationRepository.save(it) }

        return newAvatar
    }

    @Suppress("FunctionOnlyReturningConstant", "UNUSED_PARAMETER")
    private fun hasReadAccess(userId: Long?, organizationRole: Role): Boolean = true

    private fun hasWriteAccess(userId: Long?, organizationRole: Role): Boolean = hasDeleteAccess(userId, organizationRole) ||
            userId?.let { organizationRole == Role.ADMIN } ?: false

    private fun hasDeleteAccess(userId: Long?, organizationRole: Role): Boolean =
            hasBanAccess(userId, organizationRole) || userId?.let { organizationRole == Role.OWNER } ?: false

    /**
     * Only [Role.SUPER_ADMIN] can ban the project. And a user with such a global role has permissions for all actions.
     * Since we have all the rights issued depending on the following, you need to set [false] here
     */
    @Suppress("FunctionOnlyReturningConstant", "UnusedParameter")
    private fun hasBanAccess(userId: Long?, organizationRole: Role): Boolean = false
}
