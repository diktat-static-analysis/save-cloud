package com.saveourtool.common.demo.cpg.cytoscape

import com.saveourtool.common.demo.cpg.CpgNodeAdditionalInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @property data
 * @property position
 * @property selected whether the element is selected (default false)
 * @property selectable whether the selection state is mutable (default true)
 * @property locked when locked a node's position is immutable (default false)
 * @property grabbable whether the node can be grabbed and moved by the user
 * @property pannable whether dragging the node causes panning instead of grabbing
 * @property classes an array (or a space separated string) of class names that the element has
 * @property additionalInfo
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CytoscapeNode(
    val data: Data,
    val position: Position? = null,
    val selected: Boolean = false,
    val selectable: Boolean = true,
    val locked: Boolean = false,
    val grabbable: Boolean = true,
    val pannable: Boolean = false,
    val classes: List<String> = emptyList(),
    val additionalInfo: CpgNodeAdditionalInfo? = null,
) {
    /**
     * @property x
     * @property y
     */
    @Serializable
    data class Position(val x: Int, val y: Int)

    /**
     * @property id
     * @property parent
     * @property label
     */
    @Serializable
    data class Data(val id: String, val parent: String? = null, val label: String? = null)
}
