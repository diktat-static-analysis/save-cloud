package com.saveourtool.save.test.analysis.api

import com.saveourtool.common.test.analysis.results.AnalysisResult
import com.saveourtool.save.test.analysis.algorithms.FlipRateAnalysis
import com.saveourtool.save.test.analysis.algorithms.PermanentFailureDetection
import com.saveourtool.save.test.analysis.algorithms.RegressionDetection
import com.saveourtool.save.test.analysis.internal.DefaultTestAnalysisService

/**
 * Analyzes test runs for the given test id; see [analyze] for details.
 *
 * @see analyze
 */
interface TestAnalysisService {
    /**
     * The statistical data about test runs this service uses during analysis.
     */
    val statisticsStorage: TestStatisticsStorage

    /**
     * Analyzes test runs for the given test [id] and returns the list of
     * results.
     *
     * @param id the unique test id.
     * @return the list of analysis results for the given test [id].
     */
    fun analyze(id: TestId): List<AnalysisResult>

    companion object Factory {
        /**
         * The default [flip rate threshold][FlipRateAnalysis.flipRateThreshold].
         */
        private const val FLIP_RATE_THRESHOLD = 0.3

        /**
         * The minimum number of test runs for a sample to be considered
         * representative.
         */
        internal const val MINIMUM_RUN_COUNT = 10

        /**
         * Creates a new service instance.
         *
         * @param statisticsStorage the statistical data about test runs to be
         *   used during analysis.
         * @return a new instance of the default implementation.
         */
        operator fun invoke(statisticsStorage: TestStatisticsStorage): TestAnalysisService =
                DefaultTestAnalysisService(
                    statisticsStorage,
                    FlipRateAnalysis(
                        MINIMUM_RUN_COUNT,
                        FLIP_RATE_THRESHOLD,
                    ),
                    RegressionDetection(
                        MINIMUM_RUN_COUNT,
                        statisticsStorage.testStatusProvider,
                    ),
                    PermanentFailureDetection(),
                )
    }
}
