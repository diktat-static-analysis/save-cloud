package com.saveourtool.common.agent

import kotlinx.serialization.Serializable

/**
 * Info about Agent
 *
 * @property containerId unique ID of the agent
 * @property containerName unique name of the agent
 * @property version version of **save-agent**
 */
@Serializable
data class AgentInfo(
    val containerId: String,
    val containerName: String,
    val version: String,
)
