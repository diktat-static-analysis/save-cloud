package com.saveourtool.common.agent

import com.saveourtool.common.test.analysis.metrics.NoDataAvailable
import com.saveourtool.common.test.analysis.metrics.TestMetrics
import com.saveourtool.common.test.analysis.results.AnalysisResult
import kotlinx.serialization.Serializable

/**
 * An "extended" [test execution][TestExecutionDto] with some extra information,
 * such as test metrics.
 *
 * Use [TestExecutionDto.toExtended] to create an extension.
 *
 * @property testExecution the actual test execution data.
 * @property hasDebugInfo whether debug info data is available for this test execution
 * @property testMetrics scalar test metrics.
 * @property analysisResults test analysis results.
 * @see TestExecutionDto
 */
@Serializable
data class TestExecutionExtDto(
    val testExecution: com.saveourtool.common.agent.TestExecutionDto,
    val hasDebugInfo: Boolean? = null,
    val testMetrics: TestMetrics = NoDataAvailable.instance,
    val analysisResults: List<AnalysisResult> = emptyList(),
)
