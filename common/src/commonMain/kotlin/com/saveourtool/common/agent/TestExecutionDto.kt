package com.saveourtool.common.agent

import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.entities.DtoWithId
import com.saveourtool.common.test.analysis.metrics.NoDataAvailable
import com.saveourtool.common.test.analysis.metrics.TestMetrics
import com.saveourtool.common.test.analysis.results.AnalysisResult

import kotlinx.serialization.Serializable

/**
 * @property filePath
 * @property pluginName name of a plugin which will execute test at [filePath]
 * @property agentContainerId
 * @property agentContainerName
 * @property status
 * @property startTimeSeconds
 * @property endTimeSeconds
 * @property testSuiteName a name of test suite, a test from which has been executed
 * @property tags list of tags of current test
 * @property unmatched number of unmatched checks/validations in test (false negative results)
 * @property matched number of matched checks/validations in test (true positive results)
 * @property expected number of all checks/validations in test (unmatched + matched)
 * @property unexpected number of matched,but not expected checks/validations in test (false positive results)
 * @property executionId
 * @property id ID of saved entity or null
 */
@Serializable
data class TestExecutionDto(
    val filePath: String,
    val pluginName: String,
    val agentContainerId: String?,
    val agentContainerName: String?,
    val status: TestResultStatus,
    val startTimeSeconds: Long?,
    val endTimeSeconds: Long?,
    val testSuiteName: String,
    val tags: List<String>,
    val unmatched: Long?,
    val matched: Long?,
    val expected: Long?,
    val unexpected: Long?,
    val executionId: Long,
    override val id: Long? = null,
) : DtoWithId() {
    /**
     * @param hasDebugInfo whether debug info data is available for this test execution
     * @param testMetrics scalar test metrics.
     * @param analysisResults test analysis results.
     * @return an "extended" version of this test execution with extra information.
     */
    fun toExtended(
        hasDebugInfo: Boolean? = null,
        testMetrics: TestMetrics = NoDataAvailable.instance,
        analysisResults: List<AnalysisResult> = emptyList(),
    ): com.saveourtool.common.agent.TestExecutionExtDto =
            com.saveourtool.common.agent.TestExecutionExtDto(
                this,
                hasDebugInfo,
                testMetrics,
                analysisResults,
            )
}
