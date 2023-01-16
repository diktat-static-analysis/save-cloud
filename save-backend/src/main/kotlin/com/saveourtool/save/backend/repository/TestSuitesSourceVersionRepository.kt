package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceVersion
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [TestsSourceVersion]
 */
@Repository
@Suppress(
    "IDENTIFIER_LENGTH",
    "FUNCTION_NAME_INCORRECT_CASE",
    "FunctionNaming",
    "FunctionName",
)
interface TestSuitesSourceVersionRepository : BaseEntityRepository<TestsSourceVersion> {
    /**
     * @param snapshot
     * @param name
     * @return [TestsSourceVersion] found by [name] in provided [TestsSourceSnapshot]
     */
    fun findBySnapshotAndName(snapshot: TestsSourceSnapshot, name: String): TestsSourceVersion?

    /**
     * @param source
     * @param name
     * @return [TestsSourceVersion] found by [name] in provided [TestSuitesSource]
     */
    fun findBySnapshot_SourceAndName(source: TestSuitesSource, name: String): TestsSourceVersion?

    /**
     * @param source
     * @return all [TestsSourceVersion] in provided [TestSuitesSource]
     */
    fun findAllBySnapshot_Source(source: TestSuitesSource): Collection<TestsSourceVersion>

    /**
     * @param snapshot
     * @return all [TestsSourceVersion] which are linked to provide [TestsSourceSnapshot]
     */
    fun findAllBySnapshot(snapshot: TestsSourceSnapshot): Collection<TestsSourceVersion>
}
