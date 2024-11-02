/**
 * File with component that is used to load the graph into sigma
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.cpg

import com.saveourtool.common.demo.cpg.CpgGraph
import com.saveourtool.save.frontend.externals.graph.paintNodes
import com.saveourtool.save.frontend.externals.graph.sigma.layouts.LayoutInstance
import com.saveourtool.save.frontend.externals.graph.sigma.layouts.useLayoutCircular
import com.saveourtool.save.frontend.externals.graph.sigma.layouts.useLayoutForceAtlas2
import com.saveourtool.save.frontend.externals.graph.sigma.layouts.useLayoutRandom
import com.saveourtool.save.frontend.externals.graph.sigma.useLoadGraph
import com.saveourtool.save.frontend.externals.graph.toGraphologyJson

import js.core.jso
import react.FC
import react.Props
import react.useEffect

/**
 * Component that is used to load the graph into sigma
 */
@Suppress("MAGIC_NUMBER", "COMPLEX_EXPRESSION", "REDUNDANT_ELSE_IN_WHEN")
val graphLoader: FC<GraphLoaderProps> = FC { props ->
    val loadGraph = useLoadGraph()
    val circularAssign = useLayoutCircular().getPositions()
    val randomAssign = useLayoutRandom().getPositions()
    val atlasAssign = useLayoutForceAtlas2(jso {
        iterations = 150
        settings = jso {
            gravity = 10
            barnesHutOptimize = true
        }
    }).getPositions()

    useEffect(props.cpgGraph.nodes, props.selectedLayout) {
        loadGraph(props.cpgGraph.removeMultiEdges().paintNodes().toGraphologyJson())
        when (props.selectedLayout) {
            SigmaLayout.ATLAS -> {
                circularAssign()
                atlasAssign()
            }
            SigmaLayout.CIRCULAR -> circularAssign()
            SigmaLayout.RANDOM -> randomAssign()
            else -> circularAssign()
        }
    }
}

/**
 * Enum class that represents available layouts
 * @property layoutName
 */
enum class SigmaLayout(val layoutName: String) {
    /**
     * ForceAtlas2 layout, iterative algorithm
     */
    ATLAS("ForceAtlas2"),

    /**
     * Circular layout, all the nodes are placed to be on the came circle
     */
    CIRCULAR("Circular"),

    /**
     * Random layout, all the nodes are placed randomly
     */
    RANDOM("Random"),
    ;
    companion object {
        val preferredLayout = RANDOM
    }
}

/**
 * [Props] for [graphLoader] functional component
 */
external interface GraphLoaderProps : Props {
    /**
     * The graph to be processed and displayed using sigma
     */
    var cpgGraph: CpgGraph

    /**
     * Layout that should be applied to the graph
     */
    var selectedLayout: SigmaLayout
}

/**
 * Extension to get positions from the original library
 */
internal fun LayoutInstance.getPositions() = this.asDynamic()["positions"]
