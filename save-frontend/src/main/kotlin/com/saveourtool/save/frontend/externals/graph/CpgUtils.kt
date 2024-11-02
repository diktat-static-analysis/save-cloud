/**
 * Utils for CpgGraph and Sigma in general
 */

package com.saveourtool.save.frontend.externals.graph

import com.saveourtool.common.demo.cpg.CpgEdge
import com.saveourtool.common.demo.cpg.CpgGraph
import com.saveourtool.common.demo.cpg.CpgNode
import com.saveourtool.common.demo.cpg.cytoscape.CytoscapeEdge
import com.saveourtool.common.demo.cpg.cytoscape.CytoscapeGraph
import com.saveourtool.common.demo.cpg.cytoscape.CytoscapeNode

import js.core.jso

import kotlin.random.Random
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val cpgJsonSerializer = Json { encodeDefaults = true }

/**
 * @return serialized graph that can be used with useLoadGraph hook
 */
fun CpgGraph.toGraphologyJson() = let { graph ->
    @Suppress("UnusedPrivateProperty", "UNUSED_VARIABLE")
    val str = cpgJsonSerializer.encodeToString(graph)
    js("JSON.parse(str);")
}

/**
 * Paint [CpgNode] with color
 *
 * @param color color to paint the [CpgNode] to
 * @return [CpgNode] painted with [color]
 */
fun CpgNode.paint(color: String) = copy(
    attributes = attributes.copy(color = color)
)

/**
 * Paint [CpgEdge] with color
 *
 * @param color color to paint the [CpgEdge] to
 * @return [CpgEdge] painted with [color]
 */
fun CpgEdge.paint(color: String) = copy(
    attributes = attributes.copy(color = color)
)

/**
 * Paint all the [CpgNode]s of [CpgGraph] with [color].
 *
 * @param color color to paint the [CpgGraph.nodes] to, if null - each [CpgNode] is painted into random color
 * @return [CpgGraph] with all [CpgGraph.nodes] painted with [color] (or randomly)
 */
fun CpgGraph.paintNodes(
    color: String? = null
) = nodes.map { node ->
    node.paint(color ?: getRandomHexColor())
}
    .let { coloredNodes ->
        copy(nodes = coloredNodes)
    }

/**
 * Paint all the [CpgEdge]s of [CpgGraph] with [color].
 *
 * @param color color to paint the [CpgGraph.edges] to, if null - each [CpgEdge] is painted into random color
 * @return [CpgGraph] with all [CpgGraph.edges] painted with [color] (or randomly)
 */
fun CpgGraph.paintEdges(
    color: String? = null
) = edges.map { edge ->
    edge.paint(color ?: getRandomHexColor())
}
    .let { coloredEdges ->
        copy(edges = coloredEdges)
    }

/**
 * @return [CytoscapeEdge] from [CpgEdge]
 */
fun CpgEdge.asCytoscapeEdge() = CytoscapeEdge(
    CytoscapeEdge.Data(key, source, target, attributes.label),
    false
)

/**
 * @return [CytoscapeNode] from [CpgNode]
 */
fun CpgNode.asCytoscapeNode() = CytoscapeNode(
    CytoscapeNode.Data(key, label = attributes.label)
)

/**
 * @return [CytoscapeGraph] from [CpgGraph]
 */
fun CpgGraph.asCytoscapeGraph() = CytoscapeGraph(
    nodes.map { it.asCytoscapeNode() },
    edges.map { it.asCytoscapeEdge() },
    CytoscapeGraph.Attributes(name = attributes.name)
)

/**
 * @param edgeType type of the edge that should be displayed
 * @param isRenderEdgeLabels flag that defines if edge labels should be displayed or not
 * @return settings of sigmaContainer
 */
fun getSigmaContainerSettings(
    edgeType: String = "arrow",
    isRenderEdgeLabels: Boolean = true,
): dynamic = jso {
    renderEdgeLabels = isRenderEdgeLabels
    defaultEdgeType = edgeType
}

/**
 * Get random hex color
 *
 * @param isPastel flag to generate only pastel colors - true by default
 * @return randomized color in format "#RRGGBB"
 */
@Suppress("MAGIC_NUMBER")
fun getRandomHexColor(isPastel: Boolean = true) = buildString {
    append("#")
    val from = if (isPastel) 127 else 0
    append(Random.nextInt(from, 255).toString(16))
    append(Random.nextInt(from, 255).toString(16))
    append(Random.nextInt(from, 255).toString(16))
}
