package com.saveourtool.save.test.analysis.api

import com.saveourtool.common.domain.TestResultStatus
import kotlin.time.Duration

/**
 * @property status the implementation-specific test status.
 * @property durationOrNull test duration (if batch size is 1) or batch duration
 *   (otherwise).
 */
data class TestRun(
    val status: TestResultStatus,
    val durationOrNull: Duration?
)
