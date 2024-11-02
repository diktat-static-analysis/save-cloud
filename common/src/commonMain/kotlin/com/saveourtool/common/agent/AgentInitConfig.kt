package com.saveourtool.common.agent

import com.saveourtool.common.utils.DEFAULT_SETUP_SH_TIMEOUT_MILLIS
import kotlinx.serialization.Serializable

/**
 * @property saveCliUrl an url to download save-cli
 * @property testSuitesSourceSnapshotUrl an url to download snapshot of test suites source with tests
 * @property additionalFileNameToUrl a map of file name to url to download additional file
 * @property saveCliOverrides overrides for save-cli
 * @property setupShTimeoutMillis amount of milliseconds to run setup.sh if it is present, [DEFAULT_SETUP_SH_TIMEOUT_MILLIS] by default
 */
@Serializable
data class AgentInitConfig(
    val saveCliUrl: String,
    val testSuitesSourceSnapshotUrl: String,
    val additionalFileNameToUrl: Map<String, String>,
    val saveCliOverrides: com.saveourtool.common.agent.SaveCliOverrides,
    val setupShTimeoutMillis: Long = DEFAULT_SETUP_SH_TIMEOUT_MILLIS,
)
