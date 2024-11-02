package com.saveourtool.common.repository

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.LnkUserOrganization
import com.saveourtool.common.entities.Organization
import com.saveourtool.common.entities.OrganizationStatus
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * Repository of lnkUserProject
 */
@Repository
interface LnkUserOrganizationRepository : BaseEntityRepository<LnkUserOrganization> {
    /**
     * @param organization
     * @return lnkUserOrganization by organization
     */
    fun findByOrganization(organization: Organization): List<LnkUserOrganization>

    /**
     * @param userId
     * @param organizationName
     * @return lnkUserOrganization by user ID and organization name
     */
    fun findByUserIdAndOrganizationName(userId: Long, organizationName: String): LnkUserOrganization?

    /**
     * @param userName
     * @param organizationName
     * @return lnkUserOrganization by user name and organization name
     */
    fun findByUserNameAndOrganizationName(userName: String, organizationName: String): LnkUserOrganization?

    /**
     * @param userId
     * @param organization
     * @return [LnkUserOrganization] if user is connected to [organization] and `null` otherwise
     */
    fun findByUserIdAndOrganization(userId: Long, organization: Organization): LnkUserOrganization?

    /**
     * @param userName
     * @param organization
     * @return lnkUserOrganization by user name and organization
     */
    fun findByUserNameAndOrganization(userName: String, organization: Organization): LnkUserOrganization?

    /**
     * @param userId
     * @param canCreateContests flag that indicates if organization can create contests
     * @param roles list of roles that are required for user
     * @return list of [LnkUserOrganization] where user has role from [roles] and [Organization] can create contests
     */
    fun findByUserIdAndOrganizationCanCreateContestsAndRoleIn(userId: Long, canCreateContests: Boolean, roles: List<Role>): List<LnkUserOrganization>

    /**
     * @param userName
     * @param canCreateContests flag that indicates if organization can create contests
     * @param roles list of roles that are required for user
     * @return list of [LnkUserOrganization] where user has role from [roles] and [Organization] can create contests
     */
    fun findByUserNameAndOrganizationCanCreateContestsAndRoleIn(userName: String, canCreateContests: Boolean, roles: List<Role>): List<LnkUserOrganization>

    /**
     * @param userId
     * @param organizationId
     * @return [LnkUserOrganization] if user is connected to organization with [organizationId] and `null` otherwise
     */
    fun findByUserIdAndOrganizationId(userId: Long, organizationId: Long): LnkUserOrganization?

    /**
     * Save [LnkUserOrganization] using only ids and role string.
     *
     * @param userId
     * @param organizationId
     * @param role
     */
    @Transactional
    @Modifying
    @Query(
        value = "insert into save_cloud.lnk_user_organization (organization_id, user_id, role) values (:organization_id, :user_id, :role)",
        nativeQuery = true,
    )
    fun save(
        @Param("organization_id") organizationId: Long,
        @Param("user_id") userId: Long,
        @Param("role") role: String
    )

    /**
     * @param userId
     * @return List of [LnkUserOrganization] in which user with [userId] participates
     */
    fun findByUserId(userId: Long): List<LnkUserOrganization>

    /**
     * @param userId
     * @param canBulkUpload
     * @param statuses
     * @return List of [LnkUserOrganization] in which user with [userId] participates
     */
    fun findByUserIdAndOrganizationCanBulkUploadAndOrganizationStatusIn(userId: Long, canBulkUpload: Boolean, statuses: Set<OrganizationStatus>): List<LnkUserOrganization>

    /**
     * @param userName
     * @param status status of organization
     * @return List of [LnkUserOrganization] in which user with [userName] participates
     */
    fun findByUserNameAndOrganizationStatus(userName: String, status: OrganizationStatus): List<LnkUserOrganization>

    /**
     * @param userName
     * @param organizationName
     * @param status status of organization
     * @return List of [LnkUserOrganization] in which user with [userName] participates
     */
    fun findByUserNameAndOrganizationStatusAndOrganizationName(userName: String, status: OrganizationStatus, organizationName: String): LnkUserOrganization?

    /**
     * @param userId id of user
     */
    fun deleteByUserId(userId: Long)
}
