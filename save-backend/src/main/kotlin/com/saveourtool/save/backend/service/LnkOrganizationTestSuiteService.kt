package com.saveourtool.save.backend.service

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.*
import com.saveourtool.common.permission.Rights
import com.saveourtool.common.utils.orNotFound
import com.saveourtool.save.backend.repository.LnkOrganizationTestSuiteRepository

import org.springframework.stereotype.Service

/**
 * Service of [LnkOrganizationTestSuite]
 */
@Service
class LnkOrganizationTestSuiteService(
    private val lnkOrganizationTestSuiteRepository: LnkOrganizationTestSuiteRepository,
) {
    /**
     * @param organization
     * @return all [TestSuite]s with rights for [organization]
     */
    fun getAllTestSuitesByOrganization(organization: Organization) =
            lnkOrganizationTestSuiteRepository.findByOrganization(organization)
                .map {
                    it.testSuite
                }

    /**
     * @param organization
     * @param testSuite
     * @return [LnkOrganizationTestSuite] by [organization] and [testSuite]
     */
    fun findByOrganizationAndTestSuite(organization: Organization, testSuite: TestSuite) =
            lnkOrganizationTestSuiteRepository.findByOrganizationAndTestSuite(organization, testSuite)

    /**
     * Set [rights] of [organization] over [testSuite] or delete them if [rights] is [Role.NONE].
     */
    @Suppress("KDOC_WITHOUT_PARAM_TAG")
    fun setOrDeleteRights(organization: Organization, testSuite: TestSuite, rights: Rights) {
        if (rights == Rights.NONE) {
            removeRights(organization, testSuite)
        }
        val lnkOrganizationTestSuite = findByOrganizationAndTestSuite(organization, testSuite)
            ?.apply { this.rights = rights }
            ?: LnkOrganizationTestSuite(organization, testSuite, rights)
        lnkOrganizationTestSuiteRepository.save(lnkOrganizationTestSuite)
    }

    /**
     * @param organization
     * @param testSuite
     * @return [LnkOrganizationTestSuiteDto] of [organization] over [testSuite]
     */
    fun getDto(organization: Organization, testSuite: TestSuite) =
            findByOrganizationAndTestSuite(organization, testSuite)?.toDto() ?: LnkOrganizationTestSuiteDto(
                organization.toDto(),
                testSuite.toDto(),
                Rights.NONE
            )

    /**
     * Removes rights of [organization] over [testSuite].
     *
     * @param organization
     * @param testSuite
     * @return Unit
     */
    fun removeRights(organization: Organization, testSuite: TestSuite) = findByOrganizationAndTestSuite(organization, testSuite)
        ?.requiredId()
        ?.let { lnkOrganizationTestSuiteRepository.deleteById(it) }
        .orNotFound {
            "Cannot unlink organization with name ${organization.name} from test suite with name ${testSuite.name} because no link was found."
        }
}
