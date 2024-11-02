package com.saveourtool.common.agent

import com.saveourtool.common.domain.TestResultDebugInfo
import kotlinx.serialization.Serializable

/**
 * @property cliArgs command line arguments for SAVE launch
 * @property executionDataUploadUrl an url to upload execution data ([List] of [TestExecutionResult])
 * @property debugInfoUploadUrl an url to upload debug info ([TestResultDebugInfo])
 */
@Serializable
data class AgentRunConfig(
    val cliArgs: String,
    val executionDataUploadUrl: String,
    val debugInfoUploadUrl: String,
)
