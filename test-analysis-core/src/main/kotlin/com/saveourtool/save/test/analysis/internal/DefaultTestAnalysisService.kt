package com.saveourtool.save.test.analysis.internal

import com.saveourtool.common.test.analysis.metrics.NoDataAvailable
import com.saveourtool.common.test.analysis.metrics.RegularTestMetrics
import com.saveourtool.common.test.analysis.results.AnalysisResult
import com.saveourtool.common.test.analysis.results.RegularTest
import com.saveourtool.save.test.analysis.algorithms.Algorithm
import com.saveourtool.save.test.analysis.api.TestAnalysisService
import com.saveourtool.save.test.analysis.api.TestId
import com.saveourtool.save.test.analysis.api.TestStatisticsStorage

/**
 * The default implementation of [TestAnalysisService].
 *
 * @param algorithms the algorithms used by this service.
 * @property statisticsStorage the storage of statistical data about test runs.
 */
internal class DefaultTestAnalysisService(
    override val statisticsStorage: TestStatisticsStorage,
    private vararg val algorithms: Algorithm
) : TestAnalysisService {
    override fun analyze(id: TestId): List<AnalysisResult> {
        val testRuns = statisticsStorage.getExecutionStatistics(id)

        return when (val metrics = statisticsStorage.getTestMetrics(id)) {
            is NoDataAvailable -> listOf(RegularTest.instance)
            is RegularTestMetrics -> algorithms
                .asSequence()
                .map { algorithm ->
                    algorithm(testRuns, metrics)
                }
                .filterNotNull()
                .toList()
                .ifEmpty {
                    listOf(RegularTest.instance)
                }
        }
    }
}
