package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.*
import com.saveourtool.common.permission.Rights
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository of [LnkOrganizationTestSuite]
 */
@Repository
interface LnkOrganizationTestSuiteRepository : BaseEntityRepository<LnkOrganizationTestSuite> {
    /**
     * @param organization
     * @return [LnkOrganizationTestSuite] by [organization]
     */
    fun findByOrganization(organization: Organization): List<LnkOrganizationTestSuite>

    /**
     * @param organization
     * @param testSuite
     * @return [LnkOrganizationTestSuite] by [organization] and [testSuite]
     */
    fun findByOrganizationAndTestSuite(organization: Organization, testSuite: TestSuite): LnkOrganizationTestSuite?

    /**
     * @param organization
     * @param rights
     * @return List of [LnkOrganizationTestSuite] where [organization] has [rights]
     */
    fun findByOrganizationAndRights(organization: Organization, rights: Rights): List<LnkOrganizationTestSuite>

    /**
     * @param organizationId
     * @param testSuiteId
     * @return [LnkOrganizationTestSuite] by [organizationId] and [testSuiteId]
     */
    fun findByOrganizationIdAndTestSuiteId(organizationId: Long, testSuiteId: Long): LnkOrganizationTestSuite?
}
