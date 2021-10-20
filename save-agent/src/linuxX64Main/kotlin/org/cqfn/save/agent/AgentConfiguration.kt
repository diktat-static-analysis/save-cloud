/**
 * Configuration classes for save-agent
 */

package org.cqfn.save.agent

import kotlinx.serialization.Serializable

/**
 * Configuration for save agent.
 *
 * @property id agent id
 * @property backend configuration for connection to backend
 * @property orchestratorUrl URL of SAVE orchestrator
 * @property heartbeat configuration of heartbeats
 * @property requestTimeoutMillis timeout for all http request
 * @property executionDataRetryAttempts number of retries when sending execution data
 * @property executionDataInitialRetryMillis interval between successive attempts to send execution data
 * @property cliCommand a command that agent will use to run SAVE cli
 * @property debug whether debug logging should be enabled
 */
@Serializable
data class AgentConfiguration(
    val id: String,
    val backend: BackendConfig,
    val orchestratorUrl: String,
    val heartbeat: HeartbeatConfig,
    val requestTimeoutMillis: Long,
    val retry: RetryConfig,
    val debug: Boolean = false,
    val cliCommand: String,
    val logFilePath: String = "logs.txt"
)

/**
 * @property intervalMillis interval between heartbeats to orchestrator in milliseconds
 */
@Serializable
data class HeartbeatConfig(
    val intervalMillis: Long,
)

/**
 * Configuration for connection to backend service
 *
 * @property url URL of backend
 * @property additionalDataEndpoint endpoint to post additional data (version etc.) to
 * @property executionDataEndpoint endpoint to post execution data to
 */
@Serializable
data class BackendConfig(
    val url: String,
    val additionalDataEndpoint: String = "saveAgentVersion",
    val executionDataEndpoint: String = "saveTestResult",
)

@Serializable
data class RetryConfig(
    val attempts: Int,
    val initialRetryMillis: Long,
)
