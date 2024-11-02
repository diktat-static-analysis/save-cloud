@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.graph

import com.saveourtool.common.demo.cpg.CpgGraph
import com.saveourtool.save.frontend.components.basic.cpg.SigmaLayout
import com.saveourtool.save.frontend.components.basic.cpg.graphEvents
import com.saveourtool.save.frontend.components.basic.cpg.graphLoader
import com.saveourtool.save.frontend.externals.graph.getSigmaContainerSettings
import com.saveourtool.save.frontend.externals.graph.sigma.sigmaContainer
import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.useState
import web.cssom.*

/**
 * Graph visualizer based on Sigma and Graphology libs
 */
val sigmaGraphVisualizer: FC<SigmaGraphVisualizerProps> = FC { props ->
    kotlinext.js.require<dynamic>("@react-sigma/core/lib/react-sigma.min.css")
    val (selectedNodeName, setSelectedNodeName) = useState<String?>(null)
    val graphology: dynamic = kotlinext.js.require("graphology")
    sigmaContainer {
        settings = getSigmaContainerSettings()
        this.graph = graphology.MultiDirectedGraph
        graphEvents {
            shouldHideUnfocusedNodes = true
            setSelectedNode = { newSelectedNodeName ->
                setSelectedNodeName { previousSelectedNodeName ->
                    newSelectedNodeName.takeIf { it != previousSelectedNodeName }
                }
            }
        }
        graphLoader {
            this.cpgGraph = props.cpgGraph
            this.selectedLayout = props.layout
        }
    }
    div {
        id = "collapse"
        val show = selectedNodeName?.let { nodeName ->
            props.cpgGraph
                .nodes
                .find { node -> node.key == nodeName }
                ?.let { node ->
                    displayCpgNodeAdditionalInfo(
                        node.attributes.label,
                        props.query,
                        node.attributes.additionalInfo,
                    ) { setSelectedNodeName(it) }
                }
            "show"
        } ?: "hide"
        className = ClassName("col-6 p-0 position-absolute width overflow-auto $show")
        style = jso {
            top = "0px".unsafeCast<Top>()
            right = "0px".unsafeCast<Right>()
            maxHeight = "100%".unsafeCast<MaxHeight>()
        }
    }
}

/**
 * [Props] for [sigmaGraphVisualizer]
 */
external interface SigmaGraphVisualizerProps : Props {
    /**
     * Graph to render
     */
    var cpgGraph: CpgGraph

    /**
     * Query to neo4J
     */
    var query: String

    /**
     * [SigmaLayout] to apply to graph
     */
    var layout: SigmaLayout
}
