package com.saveourtool.common.demo

import kotlinx.serialization.Serializable

/**
 * @property codeLines file as String that contains code requested for demo run
 * @property mode mode to determine the mode that the tool should be run in
 * @property config additional configuration file for demo
 */
@Serializable
data class DemoRunRequest(
    val codeLines: List<String>,
    val mode: String,
    val config: List<String>? = null,
) {
    companion object {
        val empty = DemoRunRequest(emptyList(), "", null)
    }
}
