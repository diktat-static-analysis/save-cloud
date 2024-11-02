/**
 * Configuration classes for save-agent
 */

package com.saveourtool.save.agent

import com.saveourtool.common.agent.AgentEnvName
import com.saveourtool.common.agent.AgentInfo
import com.saveourtool.common.utils.optionalEnv
import com.saveourtool.common.utils.requiredEnv
import com.saveourtool.save.agent.utils.SAVE_CLI_EXECUTABLE_NAME
import com.saveourtool.save.agent.utils.TEST_SUITES_DIR_NAME
import com.saveourtool.save.core.config.LogType
import com.saveourtool.save.core.config.OutputStreamType
import com.saveourtool.save.core.config.ReportType

import generated.SAVE_CLOUD_VERSION

import kotlinx.serialization.Serializable

/**
 * Configuration for save agent.
 *
 * @property info agent info
 * @property heartbeat configuration of heartbeats
 * @property cliCommand a command that agent will use to run SAVE cli
 * @property requestTimeoutMillis timeout for all http request
 * @property retry configuration for HTTP request retries
 * @property debug whether debug logging should be enabled
 * @property testSuitesDir directory where tests and additional files need to be stored into
 * @property logFilePath path to logs of save-cli execution
 * @property save additional configuration for save-cli
 * @property kubernetes a flag which shows that agent runs in k8s
 */
@Serializable
data class AgentConfiguration(
    val info: AgentInfo,
    val heartbeat: HeartbeatConfig,
    val cliCommand: String = "./$SAVE_CLI_EXECUTABLE_NAME",
    val requestTimeoutMillis: Long = 60000,
    val retry: RetryConfig = RetryConfig(),
    val debug: Boolean = false,
    val testSuitesDir: String = TEST_SUITES_DIR_NAME,
    val logFilePath: String = "logs.txt",
    val kubernetes: Boolean = false,
    val save: SaveCliConfig = SaveCliConfig(),
) {
    companion object {
        /**
         * @return [AgentConfiguration] with required fields initialized from env
         */
        internal fun initializeFromEnv() = AgentConfiguration(
            info = AgentInfo(
                containerId = requiredEnv(AgentEnvName.CONTAINER_ID.name),
                containerName = requiredEnv(AgentEnvName.CONTAINER_NAME.name),
                version = SAVE_CLOUD_VERSION,
            ),
            heartbeat = HeartbeatConfig(
                url = requiredEnv(AgentEnvName.HEARTBEAT_URL.name),
            ),
            kubernetes = optionalEnv(AgentEnvName.KUBERNETES.name).toBoolean(),
        )
    }
}

/**
 * @property url URL of heartbeat endpoint
 * @property intervalMillis interval between heartbeats to orchestrator in milliseconds
 */
@Serializable
data class HeartbeatConfig(
    val url: String,
    val intervalMillis: Long = 15000,
)

/**
 * @property attempts number of retries when sending data
 * @property initialRetryMillis interval between successive attempts to send data
 */
@Serializable
data class RetryConfig(
    val attempts: Int = 5,
    val initialRetryMillis: Long = 2000,
)

/**
 * @property reportType corresponds to flag `--report-type` of save-cli
 * @property reportDir corresponds to flag `--report-dir` of save-cli
 * @property logType corresponds to flag `--log` of save-cli
 * @property resultOutput corresponds to flag `--result-output` of save-cli
 */
@Serializable
data class SaveCliConfig(
    val reportType: ReportType = ReportType.JSON,
    val resultOutput: OutputStreamType = OutputStreamType.FILE,
    val reportDir: String = "save-reports",
    val logType: LogType = LogType.ALL,
)
