package com.saveourtool.save.test.analysis.algorithms

import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.test.analysis.metrics.RegularTestMetrics
import com.saveourtool.common.test.analysis.results.IrregularTest
import com.saveourtool.common.test.analysis.results.Regression
import com.saveourtool.save.test.analysis.api.TestRuns
import com.saveourtool.save.test.analysis.api.TestStatusProvider
import com.saveourtool.save.test.analysis.api.TestStatusProviderScope

/**
 * Regression detection algorithm.
 *
 * @param minimumRunCount the minimum run count a sample should have to be
 *   representative.
 * @property testStatusProvider the test status provider.
 */
class RegressionDetection(
    private val minimumRunCount: Int,
    override val testStatusProvider: TestStatusProvider<TestResultStatus>,
) : Algorithm, TestStatusProviderScope<TestResultStatus> {
    init {
        require(minimumRunCount > 0) {
            "Minimum run count should be positive: $minimumRunCount"
        }
    }

    @Suppress("NestedBlockDepth")
    override fun invoke(runs: TestRuns, metrics: RegularTestMetrics): IrregularTest? {
        require(runs.size == metrics.runCount) {
            "Runs and metrics report different run count: ${runs.size} != ${metrics.runCount}"
        }

        return when {
            metrics.runCount >= minimumRunCount -> when (metrics.failureRate) {
                /*
                 * Permanent success.
                 */
                0.0 -> null

                /*
                 * Permanent failure.
                 */
                1.0 -> null

                else -> when (metrics.flipCount) {
                    /*
                     * Statistics for a regression contains exactly a single flip.
                     */
                    1 -> with(testStatusProvider) {
                        when {
                            runs.first().isSuccess() && runs.last().isFailure() -> Regression("1 regression over ${metrics.runCount} run(s)")
                            else -> null
                        }
                    }

                    else -> null
                }
            }

            /*
             * Non-representative sample.
             */
            else -> null
        }
    }
}
