/**
 * File that contains CpgEdge definition
 */

package com.saveourtool.common.demo.cpg

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @property key id of an edge
 * @property source the beginning of the edge
 * @property target the end of the edge
 * @property attributes displaying attributes
 */
@Serializable
@ExperimentalSerializationApi
data class CpgEdge(
    val key: String,
    val source: String,
    val target: String,
    @EncodeDefault(ALWAYS) val attributes: CpgEdgeAttributes = CpgEdgeAttributes(),
)

/**
 * @property label label for edge
 * @property color edge and label color in format '#FFFFFF', if null, the default color is set
 * @property size size of edge, 10 by default
 */
@Serializable
@ExperimentalSerializationApi
data class CpgEdgeAttributes(
    @EncodeDefault(NEVER) val label: String? = null,
    @EncodeDefault(NEVER) val color: String? = null,
    @EncodeDefault(ALWAYS) val size: Int = 2,
)
