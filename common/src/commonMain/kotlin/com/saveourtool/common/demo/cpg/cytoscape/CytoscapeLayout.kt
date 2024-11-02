package com.saveourtool.common.demo.cpg.cytoscape

/**
 * @property layoutName lib-defined name of layout
 */
enum class CytoscapeLayout(val layoutName: String) {
    /**
     * Puts nodes in a hierarchy, based on a breadthfirst traversal of the graph.
     *
     * It is best suited to trees and forests in its default top-down mode,
     * and it is best suited to DAGs in its circle mode.
     */
    BREADTHFIRST("breadthfirst"),

    /**
     * Puts nodes in a circle
     */
    CIRCLE("circle"),

    /**
     * Puts nodes in concentric circles, based on a metric that you specify to segregate the nodes into levels
     */
    CONCENTRIC("concentric"),

    /**
     * The cose (Compound Spring Embedder) layout uses a physics simulation to lay out graphs.
     * It works well with non-compound graphs, and it has additional logic to support compound graphs well.
     *
     * The cose layout is very fast and produces good results.
     * The cose-bilkent extension is an evolution of the algorithm that is more computationally expensive
     * but produces near-perfect results.
     */
    COSE("cose"),

    /**
     * Puts nodes in a well-spaced grid
     */
    GRID("grid"),

    /**
     * Puts all nodes at (0, 0), it’s useful for debugging purposes
     */
    NULL("null"),

    /**
     * Puts nodes in the positions you specify manually
     */
    PRESET("preset"),

    /**
     * Puts nodes in random positions within the viewport
     */
    RANDOM("random"),
    ;

    companion object {
        val availableLayouts = values().filterNot { it == PRESET || it == NULL }
        val preferredLayout = GRID
    }
}
