/**
 * Various utilities
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.common.agent.AgentEnvName
import com.saveourtool.common.utils.optionalEnv
import com.saveourtool.save.agent.AgentConfiguration
import com.saveourtool.save.core.logging.logTrace

internal const val SAVE_CLI_EXECUTABLE_NAME = "save-linuxX64.kexe"
internal const val TEST_SUITES_DIR_NAME = "test-suites"

/**
 * @return [AgentConfiguration] with some values overridden from env variables
 */
internal fun AgentConfiguration.updateFromEnv(): AgentConfiguration {
    logTrace("Initial agent config: $this; applying overrides from env")
    return copy(
        info = info.copy(
            containerId = optionalEnv(AgentEnvName.CONTAINER_ID.name) ?: info.containerId,
            containerName = optionalEnv(AgentEnvName.CONTAINER_NAME.name) ?: info.containerName,
        ),
        heartbeat = heartbeat.copy(
            url = optionalEnv(AgentEnvName.HEARTBEAT_URL.name) ?: heartbeat.url,
        ),
        cliCommand = optionalEnv(AgentEnvName.CLI_COMMAND.name) ?: cliCommand,
        debug = optionalEnv(AgentEnvName.DEBUG.name)?.toBoolean() ?: debug,
        testSuitesDir = optionalEnv(AgentEnvName.TEST_SUITES_DIR.name) ?: testSuitesDir,
    )
}
