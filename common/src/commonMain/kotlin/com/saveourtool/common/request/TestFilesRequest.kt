package com.saveourtool.common.request

import com.saveourtool.common.test.TestDto
import com.saveourtool.common.test.TestsSourceSnapshotDto
import kotlinx.serialization.Serializable

/**
 * @property test [TestDto] of a test that is requested
 * @property testsSourceSnapshot snapshot where this test exists
 */
@Serializable
data class TestFilesRequest(
    val test: TestDto,
    val testsSourceSnapshot: TestsSourceSnapshotDto,
)
