package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.TestsSourceSnapshot
import com.saveourtool.common.entities.TestsSourceVersion
import com.saveourtool.common.spring.repository.BaseEntityRepository
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
interface TestsSourceVersionRepository : BaseEntityRepository<TestsSourceVersion> {
    /**
     * @param snapshotId
     * @return all [TestsSourceVersion] which are linked to [TestsSourceSnapshot] (by [snapshotId])
     */
    fun findAllBySnapshotId(snapshotId: Long): Collection<TestsSourceVersion>

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return [TestsSourceVersion] which linked to some [TestsSourceSnapshot] (by [organizationName], [sourceName]) with provided [version]
     */
    fun findBySnapshot_Source_OrganizationNameAndSnapshot_SourceNameAndName(
        organizationName: String,
        sourceName: String,
        version: String,
    ): TestsSourceVersion?

    /**
     * @param snapshotId
     * @param version
     * @return [TestsSourceVersion] which linked to some [TestsSourceSnapshot] (by [snapshotId]) with provided [version]
     */
    fun findBySnapshotIdAndName(
        snapshotId: Long,
        version: String,
    ): TestsSourceVersion?

    /**
     * @param sourceId
     * @param version
     * @return [TestsSourceVersion] which linked to some [TestsSourceSnapshot] from some [com.saveourtool.save.entities.TestSuitesSource] (by [sourceId]) with provided [version]
     */
    fun findBySnapshot_SourceIdAndName(
        sourceId: Long,
        version: String,
    ): TestsSourceVersion?

    /**
     * @param organizationName
     * @return all [TestsSourceVersion] which linked to some [TestsSourceSnapshot] (by [organizationName])
     */
    fun findAllBySnapshot_Source_OrganizationName(
        organizationName: String,
    ): Collection<TestsSourceVersion>

    /**
     * @param organizationName
     * @param sourceName
     * @return all [TestsSourceVersion] which linked to some [TestsSourceSnapshot] (by [organizationName], [sourceName])
     */
    fun findAllBySnapshot_Source_OrganizationNameAndSnapshot_SourceName(
        organizationName: String,
        sourceName: String,
    ): Collection<TestsSourceVersion>
}
