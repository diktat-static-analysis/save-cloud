package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.common.testsuite.TestSuitesSourceDto
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property organization which this test suites source belongs to
 * @property name unique name of [TestSuitesSource]
 * @property description free text
 * @property git git credentials for this test suites source
 * @property testRootPath relative path to tests in source
 * @property latestFetchedVersion
 */
@Entity
@Suppress("LongParameterList")
class TestSuitesSource(
    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization,

    var name: String,
    var description: String?,

    @ManyToOne
    @JoinColumn(name = "git_id")
    var git: Git,
    var testRootPath: String,
    var latestFetchedVersion: String?,
) : BaseEntityWithDtoWithId<TestSuitesSourceDto>() {
    /**
     * @return entity as dto [TestSuitesSourceDto]
     */
    override fun toDto(): TestSuitesSourceDto = TestSuitesSourceDto(
        organizationName = organization.name,
        name = name,
        description = description,
        gitDto = git.toDto(),
        testRootPath = testRootPath,
        latestFetchedVersion = latestFetchedVersion,
        id = id,
    )

    companion object {
        val empty = TestSuitesSource(
            Organization.stub(-1),
            "",
            null,
            Git.empty,
            "",
            null,
        )

        /**
         * @param organization [Organization] from database
         * @param git [Git] from database
         * @param latestFetchedVersion
         * @return [TestSuitesSource] from [TestSuitesSourceDto]
         */
        fun TestSuitesSourceDto.toTestSuiteSource(
            organization: Organization,
            git: Git,
            latestFetchedVersion: String? = null,
        ): TestSuitesSource {
            require(organizationName == organization.name) {
                "Provided another organization: $organization"
            }
            require(gitDto == git.toDto()) {
                "Provided another git: $git"
            }
            return TestSuitesSource(
                organization,
                name,
                description,
                git,
                testRootPath,
                latestFetchedVersion,
            ).apply {
                this.id = this@toTestSuiteSource.id
            }
        }
    }
}
