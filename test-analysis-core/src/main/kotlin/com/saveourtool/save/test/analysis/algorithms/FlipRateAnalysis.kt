package com.saveourtool.save.test.analysis.algorithms

import com.saveourtool.common.test.analysis.metrics.RegularTestMetrics
import com.saveourtool.common.test.analysis.results.FlakyTest
import com.saveourtool.common.test.analysis.results.IrregularTest
import com.saveourtool.save.test.analysis.api.TestRuns

/**
 * _Flip-rate_ based flaky test detection algorithm.
 *
 * A test _flip_ is a status change (either from _successful_ to a _failed_ or
 * vice versa) over two consecutive test runs.
 *
 * A _flip rate_ is the ratio of the actual flip number to the maximum possible
 * flip number (i.e. run count minus one, see
 * [RegularTestMetrics.flipRate] and [RegularTestMetrics.flipRatePercentage]):
 *
 * ```
 *         N          + N
 *          pass➔fail    fail➔pass
 * R     = ────────────────────────
 *  flip          N    - 1
 *                 run
 * ```
 *
 * If the _flip rate_ exceeds a certain [threshold][flipRateThreshold], the test
 * is considered _flaky_.
 *
 * @param minimumRunCount the minimum run count a sample should have to be
 *   representative.
 * @param flipRateThreshold the _flip rate_ threshold.
 *   If the threshold is exceeded, the test is considered _flaky_.
 * @see RegularTestMetrics.flipRate
 * @see RegularTestMetrics.flipRatePercentage
 */
@Suppress("FLOAT_IN_ACCURATE_CALCULATIONS")
class FlipRateAnalysis(
    private val minimumRunCount: Int,
    private val flipRateThreshold: Double,
) : Algorithm {
    init {
        require(minimumRunCount > 0) {
            "Minimum run count should be positive: $minimumRunCount"
        }
        require(0.0 < flipRateThreshold && flipRateThreshold < 1.0) {
            "Flip Rate threshold should be in range (0.0, 1.0): $flipRateThreshold"
        }
    }

    override fun invoke(runs: TestRuns, metrics: RegularTestMetrics): IrregularTest? {
        require(runs.size == metrics.runCount) {
            "Runs and metrics report different run count: ${runs.size} != ${metrics.runCount}"
        }

        return when {
            metrics.flipRate > flipRateThreshold && metrics.runCount >= minimumRunCount ->
                FlakyTest("Flip rate of ${metrics.flipRatePercentage}% over ${metrics.runCount} run(s)")

            else -> null
        }
    }
}
