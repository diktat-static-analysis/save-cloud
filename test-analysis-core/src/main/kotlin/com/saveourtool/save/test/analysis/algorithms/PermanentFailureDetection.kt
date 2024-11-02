package com.saveourtool.save.test.analysis.algorithms

import com.saveourtool.common.test.analysis.metrics.RegularTestMetrics
import com.saveourtool.common.test.analysis.results.IrregularTest
import com.saveourtool.common.test.analysis.results.PermanentFailure
import com.saveourtool.save.test.analysis.api.TestRuns

/**
 * _Permanent failure_ detection algorithm.
 */
class PermanentFailureDetection : Algorithm {
    override fun invoke(runs: TestRuns, metrics: RegularTestMetrics): IrregularTest? {
        require(runs.size == metrics.runCount) {
            "Runs and metrics report different run count: ${runs.size} != ${metrics.runCount}"
        }

        return when (metrics.failureRate) {
            1.0 -> PermanentFailure("All ${metrics.runCount} run(s) have failed")
            else -> null
        }
    }
}
