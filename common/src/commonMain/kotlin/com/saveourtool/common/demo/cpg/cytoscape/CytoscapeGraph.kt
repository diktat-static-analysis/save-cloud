package com.saveourtool.common.demo.cpg.cytoscape

import kotlinx.serialization.Serializable

/**
 * @property nodes
 * @property edges
 * @property attributes
 */
@Serializable
data class CytoscapeGraph(
    val nodes: List<CytoscapeNode>,
    val edges: List<CytoscapeEdge>,
    val attributes: Attributes = Attributes(),
) {
    /**
     * @property name
     */
    @Serializable
    data class Attributes(val name: String = "Demo graph")
}
