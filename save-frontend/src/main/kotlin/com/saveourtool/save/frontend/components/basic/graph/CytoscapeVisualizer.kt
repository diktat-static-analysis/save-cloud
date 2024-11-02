@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.graph

import com.saveourtool.common.demo.cpg.CpgGraph
import com.saveourtool.common.demo.cpg.cytoscape.CytoscapeLayout
import com.saveourtool.save.frontend.externals.graph.asCytoscapeGraph
import com.saveourtool.save.frontend.externals.graph.cytoscape.cytoscape
import com.saveourtool.save.frontend.externals.reactace.AceMarkers
import com.saveourtool.save.frontend.externals.reactace.getAceMarkers
import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import web.cssom.*
import web.html.HTMLDivElement

val cytoscapeVisualizer: FC<CytoscapeVisualizerProps> = FC { props ->
    val divRef: MutableRefObject<HTMLDivElement> = useRef(null)
    val graph = useMemo(props.graph) { props.graph.asCytoscapeGraph() }
    val cytoscapeJs = useMemo(graph, props.layout) {
        cytoscape(graph, props.layout, divRef)
    }

    val (selectedNode, setSelectedNode) = useState<dynamic>(undefined)

    val showAllNodes = {
        props.aceMarkersStateSetter(emptyArray())
        if (cytoscapeJs != undefined) {
            cytoscapeJs.nodes().show()
            cytoscapeJs.edges().show()
        }
    }

    useEffect {
        if (cytoscapeJs != undefined) {
            cytoscapeJs.bind("tap", "node") { event ->
                showAllNodes()
                val clickedNode = event.target
                if (selectedNode != clickedNode) {
                    val neighbors = clickedNode.neighborhood().add(clickedNode)
                    cytoscapeJs.nodes().forEach { node ->
                        val isInNeighbours: Boolean = neighbors.has(node) as Boolean
                        if (!isInNeighbours) {
                            node.hide()
                        }
                    }

                    val clickedNodeId = clickedNode.id() as String
                    val positionString = props.graph
                        .nodes
                        .find { it.key == clickedNodeId }
                        ?.attributes
                        ?.additionalInfo
                        ?.location
                    props.aceMarkersStateSetter { getAceMarkers(positionString) }
                    setSelectedNode { clickedNode }
                } else {
                    setSelectedNode { undefined }
                }
            }
        }
    }
    div {
        ref = divRef
        style = jso {
            width = "100%".unsafeCast<Width>()
            height = "90%".unsafeCast<Height>()
        }
    }

    div {
        id = "collapse"
        val show = if (selectedNode == undefined) {
            "hide"
        } else {
            val nodeId = selectedNode.id() as String
            props.graph
                .nodes
                .find { node -> node.key == nodeId }
                ?.let { node ->
                    displayCpgNodeAdditionalInfo(
                        node.attributes.label,
                        props.query,
                        node.attributes.additionalInfo,
                    ) {
                        showAllNodes()
                        setSelectedNode(it)
                    }
                }
            "show"
        }
        className = ClassName("col-auto p-0 position-absolute width overflow-auto $show")
        style = jso {
            top = "0px".unsafeCast<Top>()
            right = "0px".unsafeCast<Right>()
            maxHeight = "100%".unsafeCast<MaxHeight>()
        }
    }
}

/**
 * [Props] for [cytoscapeVisualizer]
 */
external interface CytoscapeVisualizerProps : Props {
    /**
     * Graph to render, must be set
     */
    var graph: CpgGraph

    /**
     * [CytoscapeLayout] to apply to graph, must be set
     */
    var layout: CytoscapeLayout

    /**
     * Query to neo4J
     */
    var query: String

    /**
     * [StateSetter] to define [AceMarkers]
     */
    var aceMarkersStateSetter: StateSetter<AceMarkers>
}
