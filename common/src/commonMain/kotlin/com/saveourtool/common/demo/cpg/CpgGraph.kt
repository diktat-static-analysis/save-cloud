/**
 * File that contains CpgGraph definition
 */

package com.saveourtool.common.demo.cpg

import kotlin.random.Random
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @property nodes list of [CpgNode]s
 * @property edges list of [CpgEdge]s
 * @property attributes graph attributes
 * @property options
 */
@Serializable
@ExperimentalSerializationApi
data class CpgGraph(
    val nodes: List<CpgNode> = emptyList(),
    val edges: List<CpgEdge> = emptyList(),
    @EncodeDefault(ALWAYS) val attributes: CpgGraphAttributes = CpgGraphAttributes(),
    @EncodeDefault(ALWAYS) val options: CpgGraphOptions = CpgGraphOptions(),
) {
    /**
     * Returns graph with parallel edges combined into one (fixes labels issue)
     *
     * @return Cpg graph with removed parallel edges
     */
    fun removeMultiEdges(): CpgGraph = combineCoDirectedEdges().combineAntiDirectedEdges()

    private fun combineCoDirectedEdges() = edges.groupBy { it.source to it.target }
        .map { (coordinates, parallelEdges) ->
            val newLabel = parallelEdges.joinToString(", ") { it.attributes.label ?: "" }
            val newId = parallelEdges.joinToString(", ") { it.key }
            val newSize = parallelEdges.first().attributes.size
            val paintColor = if (parallelEdges.size > 1) "#FFD5D4" else null
            CpgEdge(
                newId,
                coordinates.first,
                coordinates.second,
                CpgEdgeAttributes(newLabel, paintColor, newSize)
            )
        }
        .let { newEdges ->
            copy(edges = newEdges)
        }

    private fun combineAntiDirectedEdges() = edges.groupBy {
        maxOf(it.source, it.target) to minOf(it.source, it.target)
    }
        .map { (coordinates, parallelEdges) ->
            require(parallelEdges.size <= 2)
            val newLabel = parallelEdges.joinToString(" | ") { it.attributes.label ?: "" }
            val newId = parallelEdges.joinToString(" | ") { it.key }
            val newSize = parallelEdges.first().attributes.size
            val paintColor = if (parallelEdges.size != 1) {
                "#FA90FA"
            } else {
                parallelEdges[0].attributes.color ?: if (parallelEdges.size == 2) {
                    parallelEdges[1].attributes.color
                } else {
                    null
                }
            }
            listOf(
                CpgEdge(
                    newId,
                    coordinates.first,
                    coordinates.second,
                    CpgEdgeAttributes(newLabel, paintColor, newSize)
                ),
                CpgEdge(
                    "$newId-reversed",
                    coordinates.second,
                    coordinates.first,
                    CpgEdgeAttributes("", paintColor, newSize)
                )
            )
        }
        .flatten()
        .let { newEdges ->
            copy(edges = newEdges)
        }

    companion object {
        /**
         * Placeholder of a graph
         */
        val placeholder = CpgGraph(
            nodes = emptyList(),
            edges = emptyList(),
            options = CpgGraphOptions(),
            attributes = CpgGraphAttributes()
        )

        /**
         * Generate random graph with [numberOfNodes] nodes and [numberOfEdges] edges
         *
         * @param numberOfNodes requested amount of nodes in generated graph
         * @param numberOfEdges requested amount of edges in generated graph
         * @return generated graph with [numberOfNodes] nodes and [numberOfEdges] edges
         */
        fun randomGraph(numberOfNodes: Long, numberOfEdges: Long): CpgGraph = CpgGraph(
            LongRange(0, numberOfNodes - 1).map {
                CpgNode(
                    "$it-node",
                    CpgNodeAttributes(label = "$it-node"),
                )
            },
            LongRange(0, numberOfEdges - 1).map {
                CpgEdge(
                    "$it-edge",
                    "${Random.nextLong(0, numberOfNodes - 1)}-node",
                    "${Random.nextLong(0, numberOfNodes - 1)}-node",
                    CpgEdgeAttributes(label = "$it-edge"),
                )
            },
        )
    }
}

/**
 * @property name seems to be an id of a graph
 */
@Serializable
@ExperimentalSerializationApi
data class CpgGraphAttributes(
    @EncodeDefault(ALWAYS) val name: String = "Demo graph",
)

/**
 * @property type
 * @property multi
 * @property allowSelfLoops
 */
@Serializable
@ExperimentalSerializationApi
data class CpgGraphOptions(
    @EncodeDefault(ALWAYS) val type: String = "directed",
    @EncodeDefault(ALWAYS) val multi: Boolean = true,
    @EncodeDefault(ALWAYS) val allowSelfLoops: Boolean = true,
)
