package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.TestSuitesSource
import com.saveourtool.common.entities.TestsSourceSnapshot
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [TestsSourceSnapshot]
 */
@Suppress(
    "IDENTIFIER_LENGTH",
    "FUNCTION_NAME_INCORRECT_CASE",
    "FunctionNaming",
    "FunctionName",
)
@Repository
interface TestsSourceSnapshotRepository : BaseEntityRepository<TestsSourceSnapshot> {
    /**
     * @param sourceId
     * @param commitId
     * @return [TestsSourceSnapshot] found by [commitId] in [TestSuitesSource] (by [sourceId])
     */
    fun findBySourceIdAndCommitId(sourceId: Long, commitId: String): TestsSourceSnapshot?

    /**
     * @param source [com.saveourtool.save.entities.TestSuitesSource]
     * @return all [TestsSourceSnapshot] found by provided values
     */
    fun findAllBySource(
        source: TestSuitesSource,
    ): List<TestsSourceSnapshot>
}
