@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.externals.graph.cytoscape

import com.saveourtool.common.demo.cpg.cytoscape.CytoscapeGraph
import com.saveourtool.common.demo.cpg.cytoscape.CytoscapeLayout

import react.*
import web.cssom.*
import web.html.HTMLDivElement

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @param graph data in [CytoscapeGraph] format
 * @param layout [CytoscapeLayout] that should be applied to graph
 * @param divRef reference to div where a graph should be rendered in
 * @param selectionType type of node selection
 * @return cytoscape graph as dynamic
 */
@Suppress(
    "UNUSED_VARIABLE",
    "TOO_LONG_FUNCTION",
    "UNUSED_PARAMETER",
    "LongMethod"
)
fun cytoscape(
    graph: CytoscapeGraph,
    layout: CytoscapeLayout,
    divRef: MutableRefObject<HTMLDivElement>,
    selectionType: String = "single",
): dynamic {
    val cytoscapeGraphJsonString = Json.encodeToString(graph)
    val cytoscapeGraphJsonStringJs = js("JSON.parse(cytoscapeGraphJsonString);")
    // language=json
    val graphStyle = js("""
        [
            {
                "selector": "node",
                "style": {
                    "background-color": "#666",
                    "label": "data(label)",
                    "height": 20,
                    "width": 20,
                    "font-size": 7
                }
            },
            {
                "selector": "edge",
                "style": {
                    "width": 2,
                    "height": 2,
                    "line-color": "#ccc",
                    "target-arrow-color": "#ccc",
                    "target-arrow-shape": "triangle",
                    "curve-style": "bezier",
                    "label": "data(label)",
                    "font-size": 5,
                    "arrow-scale": 0.5
                }
            }
        ]
    """)
    val layoutName = layout.layoutName
    val graphLayout = js("""
        {
            "name": layoutName,
            "padding": 5
        }
    """)
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    val options = divRef.current?.let { containerRefCurrent ->
        js("""
            {
                "container" : containerRefCurrent,
                "elements" : cytoscapeGraphJsonStringJs,
                "layout" : graphLayout,
                "style" : graphStyle,
                "selectionType": selectionType
            }
        """)
    }

    val cytoscapeJs: dynamic = kotlinext.js.require("cytoscape")
    return cytoscapeJs(options)
}
