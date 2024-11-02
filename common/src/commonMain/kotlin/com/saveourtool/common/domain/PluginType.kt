@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.common.domain

import com.saveourtool.save.core.config.TestConfigSections

private val contestAllowedPlugins = listOf(TestConfigSections.WARN)

// todo: Probably should fix name in save-cli
typealias PluginType = TestConfigSections

/**
 * @return true if given TestSuite can be chosen for contest, false otherwise
 */
fun List<PluginType>.isAllowedForContests() = this == contestAllowedPlugins

/**
 * @return [PluginType] from [String]
 */
fun String.toPluginType(): PluginType = when (this) {
    "WarnPlugin" -> PluginType.WARN
    "FixPlugin" -> PluginType.FIX
    "FixAndWarnPlugin" -> PluginType.`FIX AND WARN`
    "" -> PluginType.GENERAL
    else -> throw IllegalArgumentException("No such plugin.")
}

/**
 * fixme: Will need to support pluginName in save-cli
 *
 * @return Pretty name from [PluginType]
 */
fun PluginType.pluginName() = when (this) {
    PluginType.WARN -> "WarnPlugin"
    PluginType.FIX -> "FixPlugin"
    PluginType.`FIX AND WARN` -> "FixAndWarnPlugin"
    else -> "UNKNOWN"
}
