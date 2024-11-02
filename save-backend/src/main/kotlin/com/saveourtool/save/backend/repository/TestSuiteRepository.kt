package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.TestSuite
import com.saveourtool.common.entities.TestsSourceSnapshot
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repositories for TestSuite
 */
@Suppress(
    "IDENTIFIER_LENGTH",
    "FUNCTION_NAME_INCORRECT_CASE",
    "FunctionNaming",
    "FunctionName",
)
@Repository
interface TestSuiteRepository : BaseEntityRepository<TestSuite> {
    /**
     * @param sourceSnapshot source snapshot of the test suite
     * @return matched test suites
     */
    fun findAllBySourceSnapshot(
        sourceSnapshot: TestsSourceSnapshot,
    ): List<TestSuite>

    /**
     * @param isPublic flag that indicates if given [TestSuite] is available for every organization or not
     * @return List of [TestSuite]s
     */
    fun findByIsPublic(isPublic: Boolean): List<TestSuite>
}
