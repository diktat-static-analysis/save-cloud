package com.saveourtool.save.test.analysis.internal

import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.save.test.analysis.api.TestId
import com.saveourtool.save.test.analysis.api.TestRun
import kotlin.time.Duration

/**
 * @property executionId the unique execution id.
 * @property testId the unique test id.
 * @property testRun the recent test run information with a status and optional
 *   duration.
 */
data class ExtendedTestRun(
    val executionId: Long,
    val testId: TestId,
    val testRun: TestRun,
) {
    /**
     * @return the implementation-specific test status.
     */
    @Suppress("unused")
    val status: TestResultStatus by testRun::status

    /**
     * @return test duration (if batch size is 1) or batch duration (otherwise).
     */
    @Suppress("unused")
    val durationOrNull: Duration? by testRun::durationOrNull

    /**
     * @param executionId the unique execution id.
     * @param testId the unique test id.
     * @param status the implementation-specific test status.
     * @param durationOrNull test duration (if batch size is 1) or batch
     *   duration (otherwise).
     */
    constructor(
        executionId: Long,
        testId: TestId,
        status: TestResultStatus,
        durationOrNull: Duration?,
    ) : this(
        executionId,
        testId,
        TestRun(status, durationOrNull),
    )
}
