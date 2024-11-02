package com.saveourtool.common.test.analysis.results

import com.saveourtool.common.test.analysis.metrics.RegularTestMetrics
import kotlinx.serialization.Serializable

/**
 * A permanent failure &mdash; a test that has a 100%
 * [failure rate][RegularTestMetrics.failureRate].
 *
 * @property detailMessage the detail message string of this result.
 * @see RegularTestMetrics.failureRate
 */
@Serializable
data class PermanentFailure(override val detailMessage: String = "") : IrregularTest()
