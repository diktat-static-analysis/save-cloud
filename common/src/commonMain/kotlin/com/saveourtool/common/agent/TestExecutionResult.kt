package com.saveourtool.common.agent

import com.saveourtool.common.domain.TestResultStatus
import kotlinx.serialization.Serializable

/**
 * @property filePath
 * @property pluginName name of a plugin which will execute test at [filePath]
 * @property agentContainerId
 * @property agentContainerName
 * @property status
 * @property startTimeSeconds
 * @property endTimeSeconds
 * @property unmatched number of unmatched checks/validations in test (false negative results)
 * @property matched number of matched checks/validations in test (true positive results)
 * @property expected number of all checks/validations in test (unmatched + matched)
 * @property unexpected number of matched,but not expected checks/validations in test (false positive results)
 */
@Serializable
data class TestExecutionResult(
    val filePath: String,
    val pluginName: String,
    val agentContainerId: String,
    val agentContainerName: String,
    val status: TestResultStatus,
    val startTimeSeconds: Long,
    val endTimeSeconds: Long,
    val unmatched: Long?,
    val matched: Long?,
    val expected: Long?,
    val unexpected: Long?,
)
