package com.saveourtool.common.request

import com.saveourtool.common.domain.Sdk
import kotlinx.serialization.Serializable

/**
 * @property executionId ID of [com.saveourtool.save.entities.Execution]
 * @property sdk
 * @property saveAgentUrl URL to download save-agent
 */
@Serializable
data class RunExecutionRequest(
    val executionId: Long,
    val sdk: Sdk,
    val saveAgentUrl: String,
)
