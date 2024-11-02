package com.saveourtool.save.test.analysis.entities

import com.saveourtool.common.entities.Test

/**
 * Plugin name, intended to be assignment-incompatible with the regular string.
 *
 * @property value the underlying string value.
 */
@JvmInline
value class PluginName(val value: String) {
    override fun toString(): String =
            value
}

/**
 * @return the plugin name of this test.
 */
fun Test.pluginName(): PluginName =
        PluginName(pluginName)
