package com.saveourtool.save.backend.security

import com.saveourtool.common.entities.OrganizationStatus
import com.saveourtool.common.info.UserPermissions
import com.saveourtool.common.info.UserPermissionsInOrganization
import com.saveourtool.common.repository.LnkUserOrganizationRepository
import com.saveourtool.save.authservice.utils.username

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * Class that is capable of assessing user's permissions regarding.
 */
@Component
class UserPermissionEvaluator(
    private var lnkUserOrganizationRepository: LnkUserOrganizationRepository,
) {
    /**
     * @param authentication
     * @return UserPermissions
     */
    fun getUserPermissions(
        authentication: Authentication,
    ): UserPermissions {
        val lnkOrganizations = lnkUserOrganizationRepository.findByUserNameAndOrganizationStatus(authentication.username(), OrganizationStatus.CREATED)

        return UserPermissions(
            lnkOrganizations.associate { it.organization.name to UserPermissionsInOrganization(it.organization.canCreateContests, it.organization.canBulkUpload) },
        )
    }

    /**
     * @param userName
     * @return UserPermissions
     */
    fun getUserPermissionsByName(
        userName: String,
    ): UserPermissions {
        val lnkOrganizations = lnkUserOrganizationRepository.findByUserNameAndOrganizationStatus(userName, OrganizationStatus.CREATED)

        return UserPermissions(
            lnkOrganizations.associate { it.organization.name to UserPermissionsInOrganization(it.organization.canCreateContests, it.organization.canBulkUpload) },
        )
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
}
