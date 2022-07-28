package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.orNotFound
import org.slf4j.Logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for [com.saveourtool.save.entities.TestSuitesSource]
 */
@Service
class TestSuitesSourceService(
    private val testSuitesSourceRepository: TestSuitesSourceRepository,
    private val organizationService: OrganizationService,
    private val gitService: GitService,
) {
    /**
     * @param organization [TestSuitesSource.organization]
     * @param name [TestSuitesSource.name]
     * @return entity of [TestSuitesSource] or null
     */
    fun findByName(organization: Organization, name: String) =
            testSuitesSourceRepository.findByOrganizationIdAndName(organization.requiredId(), name)

    /**
     * @param organizationName [TestSuitesSource.organization]
     * @param name [TestSuitesSource.name]
     * @return entity of [TestSuitesSource] or null
     */
    fun findByName(organizationName: String, name: String) =
            testSuitesSourceRepository.findByOrganizationIdAndName(organizationService.getByName(organizationName).requiredId(), name)

    /**
     * @param organizationName [Organization.name] from [TestSuitesSource.organization]
     * @param name [TestSuitesSource.name]
     * @return entity of [TestSuitesSource] or error
     */
    fun getByName(organizationName: String, name: String): TestSuitesSource = findByName(organizationName, name)
        .orNotFound {
            "TestSuitesSource not found by name $name in $organizationName"
        }

    /**
     * @param organization
     * @param git
     * @param testRootPath
     * @param branch
     * @return existed [TestSuitesSourceDto] with provided values or created a new one as auto-generated entity
     */
    @Transactional
    fun getOrCreate(
        organization: Organization,
        git: Git,
        testRootPath: String,
        branch: String,
    ) = testSuitesSourceRepository.findByOrganizationAndGitAndBranchAndTestRootPath(
        organization,
        git,
        branch,
        testRootPath
    ) ?: createAutoGenerated(organization, git, testRootPath, branch)

    private fun createAutoGenerated(
        organization: Organization,
        git: Git,
        testRootPath: String,
        branch: String,
    ) = testSuitesSourceRepository.save(
        TestSuitesSource(
            organization = organization,
            name = defaultTestSuitesSourceName(git.url, branch, testRootPath),
            description = "auto created test suites source by git coordinates",
            git = git,
            branch = branch,
            testRootPath = testRootPath,
        )
    )

    /**
     * @return list of [TestSuitesSource] for STANDARD tests or empty
     */
    fun findStandardTestSuitesSources(): List<TestSuitesSource> {
        val git = gitService.findByUrl(STANDARD_TEST_SUITE_URL) ?: return emptyList()
        return testSuitesSourceRepository.findAllByGit(git)
    }

    companion object {
        private val log: Logger = getLogger<TestSuitesSourceService>()

        // FIXME: a hardcoded value of url for standard test suites
        private const val STANDARD_TEST_SUITE_URL = "https://github.com/saveourtool/save-cli"

        /**
         * @return default name fot [com.saveourtool.save.entities.TestSuitesSource]
         */
        private fun defaultTestSuitesSourceName(
            url: String,
            branch: String,
            subDirectory: String
        ): String = buildString {
            append(url.replace("https?://".toRegex(), ""))
            append("/tree")
            append("/$branch")
            if (subDirectory.isNotBlank()) {
                append("/$subDirectory")
            }
        }
            .replace("/", "_")
            .replace("\\", "_")
    }
}
