package com.saveourtool.save.backend.service

import com.saveourtool.common.entities.TestSuite
import com.saveourtool.common.entities.TestSuite.Companion.toEntity
import com.saveourtool.common.entities.TestsSourceSnapshot
import com.saveourtool.common.filters.TestSuiteFilter
import com.saveourtool.common.permission.Rights
import com.saveourtool.common.testsuite.TestSuiteDto
import com.saveourtool.common.utils.orNotFound
import com.saveourtool.save.backend.repository.TestSuiteRepository

import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

import java.time.LocalDateTime

/**
 * Service for test suites
 */
@Service
@Suppress("LongParameterList")
class TestSuitesService(
    private val testSuiteRepository: TestSuiteRepository,
    @Lazy
    private val testsSourceVersionService: TestsSourceVersionService,
    private val lnkOrganizationTestSuiteService: LnkOrganizationTestSuiteService,
) {
    /**
     * Save new test suites to DB
     *
     * @param testSuiteDto test suite that should be checked and possibly saved
     * @return saved [TestSuite]
     */
    @Transactional
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "UnsafeCallOnNullableType")
    fun saveTestSuite(testSuiteDto: TestSuiteDto): TestSuite {
        // FIXME: need to check logic about [dateAdded]
        // It's kind of upsert (insert or update) with key of all fields excluding [dateAdded]
        // This logic will be removed after https://github.com/saveourtool/save-cli/issues/429

        val testSuiteCandidate = testSuiteDto.toEntity(testsSourceVersionService::getSnapshotEntity)
            .apply {
                dateAdded = null
            }
        // try to find TestSuite in the DB based on all non-null properties of `testSuite`
        // NB: that's why `dateAdded` is null in the mapping above
        val description = testSuiteCandidate.description
        val testSuite = testSuiteRepository
            .findOne(
                Example.of(testSuiteCandidate.apply { this.description = null })
            )
            .orElseGet {
                // if testSuite is not present in the DB, we will save it with current timestamp
                testSuiteCandidate.apply {
                    dateAdded = LocalDateTime.now()
                    this.description = description
                }
            }
        testSuiteRepository.save(testSuite)
        lnkOrganizationTestSuiteService.setOrDeleteRights(testSuite.sourceSnapshot.source.organization, testSuite, Rights.MAINTAIN)
        return testSuite
    }

    /**
     * @param id
     * @return test suite with [id]
     */
    fun findTestSuiteById(id: Long): TestSuite? = testSuiteRepository.findByIdOrNull(id)

    /**
     * @param ids
     * @return List of [TestSuite] by [ids]
     */
    fun findTestSuitesByIds(ids: List<Long>): List<TestSuite> = ids.mapNotNull { id ->
        testSuiteRepository.findByIdOrNull(id)
    }

    /**
     * @param filters
     * @return [List] of [TestSuite] that match [filters]
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    fun findTestSuitesMatchingFilters(filters: TestSuiteFilter): List<TestSuite> =
            ExampleMatcher.matchingAll()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                .withMatcher("language", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                .withMatcher("tags", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                .withIgnorePaths("description", "sourceSnapshot", "version", "dateAdded", "plugins")
                .let {
                    Example.of(
                        TestSuite(
                            filters.name,
                            "",
                            TestsSourceSnapshot.empty,
                            null,
                            filters.language,
                            filters.tags
                        ),
                        it
                    )
                }
                .let { testSuiteRepository.findAll(it) }

    /**
     * @param id
     * @return test suite with [id]
     * @throws ResponseStatusException if [TestSuite] is not found by [id]
     */
    fun getById(id: Long) = testSuiteRepository.findByIdOrNull(id)
        .orNotFound { "TestSuite (id=$id) not found" }

    /**
     * @return public [TestSuite]s
     */
    fun getPublicTestSuites() = testSuiteRepository.findByIsPublic(true)

    /**
     * @param sourceSnapshot source snapshot of the test suite
     * @return matched test suites
     */
    fun getBySourceSnapshot(
        sourceSnapshot: TestsSourceSnapshot,
    ): List<TestSuite> = testSuiteRepository.findAllBySourceSnapshot(sourceSnapshot)

    /**
     * @param testSuites list of test suites to be updated
     * @return saved [testSuites]
     */
    fun updateTestSuites(testSuites: List<TestSuite>): List<TestSuite> = testSuiteRepository.saveAll(testSuites)
}
