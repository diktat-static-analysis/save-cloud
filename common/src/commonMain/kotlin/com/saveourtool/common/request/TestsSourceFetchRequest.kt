package com.saveourtool.common.request

import com.saveourtool.common.test.TestsSourceSnapshotDto
import com.saveourtool.common.test.TestsSourceVersionDto
import com.saveourtool.common.test.TestsSourceVersionInfo
import com.saveourtool.common.testsuite.TestSuitesSourceDto
import com.saveourtool.common.testsuite.TestSuitesSourceFetchMode
import com.saveourtool.common.utils.GIT_HASH_PREFIX_LENGTH
import com.saveourtool.common.utils.getCurrentLocalDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property source
 * @property mode
 * @property version
 * @property createdByUserId
 */
@Serializable
data class TestsSourceFetchRequest(
    val source: TestSuitesSourceDto,

    val mode: TestSuitesSourceFetchMode,
    val version: String,

    val createdByUserId: Long,
) {
    /**
     * @param commitId [TestsSourceSnapshotDto.commitId]
     * @param commitTime [TestsSourceSnapshotDto.commitTime]
     * @return [TestsSourceSnapshotDto] created by provided values and [TestsSourceFetchRequest]
     */
    fun createSnapshot(
        commitId: String,
        commitTime: LocalDateTime,
    ): TestsSourceSnapshotDto = TestsSourceSnapshotDto(
        sourceId = source.requiredId(),
        commitId = commitId,
        commitTime = commitTime,
    )

    /**
     * @param snapshot populate [TestsSourceVersionDto.snapshotId]
     * @return [TestsSourceVersionInfo] created by provided values and [TestsSourceFetchRequest]
     */
    fun createVersion(
        snapshot: TestsSourceSnapshotDto,
    ): TestsSourceVersionDto = TestsSourceVersionDto(
        snapshotId = snapshot.requiredId(),
        name = calculateVersion(snapshot),
        type = mode,
        createdByUserId = createdByUserId,
        creationTime = getCurrentLocalDateTime(),
    )

    private fun calculateVersion(
        snapshot: TestsSourceSnapshotDto,
    ): String = if (mode == TestSuitesSourceFetchMode.BY_BRANCH) {
        "$version (${snapshot.commitId.take(GIT_HASH_PREFIX_LENGTH)})"
    } else {
        version
    }
}
