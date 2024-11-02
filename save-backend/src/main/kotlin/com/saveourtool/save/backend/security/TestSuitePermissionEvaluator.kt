package com.saveourtool.save.backend.security

import com.saveourtool.common.entities.Organization
import com.saveourtool.common.entities.TestSuite
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.permission.Rights
import com.saveourtool.save.backend.service.LnkOrganizationTestSuiteService

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * Class that is capable of assessing organization's permissions regarding test suites.
 */
@Component
class TestSuitePermissionEvaluator(
    private var lnkOrganizationTestSuiteService: LnkOrganizationTestSuiteService
) {
    /**
     * @param organization
     * @param testSuite
     * @param permission
     * @param authentication
     * @return true if [organization] has required [permission] over [testSuite]
     */
    fun hasPermission(
        organization: Organization,
        testSuite: TestSuite,
        permission: Permission,
        @Suppress("UnusedParameter") authentication: Authentication?,
    ): Boolean = lnkOrganizationTestSuiteService.getDto(organization, testSuite).rights.let { currentRights ->
        when (permission) {
            Permission.READ -> testSuite.isPublic || canAccessTestSuite(currentRights)
            Permission.WRITE, Permission.DELETE -> canMaintainTestSuite(currentRights)
            Permission.BAN -> throw IllegalStateException("Permission is not correct")
        }
    }

    private fun canMaintainTestSuite(rights: Rights) = rights == Rights.MAINTAIN

    private fun canAccessTestSuite(rights: Rights) = canMaintainTestSuite(rights) || rights == Rights.USE
}
