package com.saveourtool.common.entities

import kotlinx.serialization.Serializable

/**
 * @property containerId id of the container, inside which the agent is running
 * @property containerName name of the container, inside which the agent is running
 * @property version version of save-agent [generated.SAVE_CLOUD_VERSION]
 */
@Serializable
data class AgentDto(
    val containerId: String,
    val containerName: String,
    val version: String,
)
