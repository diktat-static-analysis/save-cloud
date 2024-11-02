package com.saveourtool.common.agent

import kotlinx.serialization.Serializable

/**
 * @property batchSize corresponds to flag `--batch-size` of save-cli (optional)
 * @property batchSeparator corresponds to flag `--batch-separator` of save-cli (optional)
 * @property overrideExecCmd corresponds to flag `--override-exec-cmd` of save-cli (optional)
 * @property overrideExecFlags corresponds to flag `--override-exec-flags` of save-cli (optional)
 */
// FIXME: need to replace with [com.saveourtool.save.core.plugin.PluginConfig]
@Serializable
data class SaveCliOverrides(
    val batchSize: Int? = null,
    val batchSeparator: String? = null,
    val overrideExecCmd: String? = null,
    val overrideExecFlags: String? = null,
)
