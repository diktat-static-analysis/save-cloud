package com.saveourtool.common.demo.cpg.cytoscape

import kotlinx.serialization.Serializable

/**
 * @property data
 * @property pannable
 */
@Serializable
data class CytoscapeEdge(val data: Data, val pannable: Boolean = true) {
    /**
     * @property id
     * @property source
     * @property target
     * @property label
     */
    @Serializable
    data class Data(
        val id: String,
        val source: String,
        val target: String,
        val label: String? = null
    )
}
