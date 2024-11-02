package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.Git
import com.saveourtool.common.entities.Organization
import com.saveourtool.common.entities.TestSuitesSource
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository of [TestSuitesSource]
 */
@Repository
interface TestSuitesSourceRepository : BaseEntityRepository<TestSuitesSource> {
    /**
     * @param organizationId
     * @return found entities
     */
    fun findAllByOrganizationId(organizationId: Long): List<TestSuitesSource>

    /**
     * @param organizationId
     * @param name
     * @return found entity or null
     */
    fun findByOrganizationIdAndName(organizationId: Long, name: String): TestSuitesSource?

    /**
     * @param organizationName
     * @param name
     * @return found entity or null
     */
    fun findByOrganizationNameAndName(organizationName: String, name: String): TestSuitesSource?

    /**
     * @param organization
     * @param git
     * @param testRootPath
     * @return found entity or null
     */
    fun findByOrganizationAndGitAndTestRootPath(
        organization: Organization,
        git: Git,
        testRootPath: String
    ): TestSuitesSource?

    /**
     * @param git
     * @return found entities
     */
    fun findAllByGit(git: Git): List<TestSuitesSource>

    /**
     * @param organizationName
     * @param name
     * @return found entity or null
     */
    @Suppress(
        "IDENTIFIER_LENGTH",
        "FUNCTION_NAME_INCORRECT_CASE",
        "FunctionNaming",
        "FunctionName",
    )
    fun findByOrganization_NameAndName(organizationName: String, name: String): TestSuitesSource?
}
