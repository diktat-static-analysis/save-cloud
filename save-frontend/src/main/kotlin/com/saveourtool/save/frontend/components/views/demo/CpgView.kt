/**
 * View for cpg
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.common.demo.cpg.CpgResult
import com.saveourtool.common.demo.cpg.cytoscape.CytoscapeLayout
import com.saveourtool.common.utils.Languages
import com.saveourtool.frontend.common.components.basic.cardComponent
import com.saveourtool.frontend.common.components.modal.displaySimpleModal
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.loadingHandler
import com.saveourtool.save.frontend.components.basic.demo.graphDemoComponent
import com.saveourtool.save.frontend.components.basic.graph.cytoscapeVisualizer
import com.saveourtool.save.frontend.externals.reactace.AceMarkers

import js.core.jso
import react.*
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.pre
import web.cssom.*

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val CPG_PLACEHOLDER_TEXT = """#include <iostream>

int main() {
    int a;
    std::cin >> a;
    std::cout << (a + a) << std::endl;
    return 0;    
}
"""

private val backgroundCard = cardComponent(hasBg = false, isPaddingBottomNull = true)

@Suppress(
    "EMPTY_BLOCK_STRUCTURE_ERROR",
)
val cpgView: FC<Props> = FC {
    useBackground(Style.SAVE_LIGHT)
    val (cpgResult, setCpgResult) = useState(CpgResult.empty)
    val (isLogs, setIsLogs) = useState(false)
    val (errorMessage, setErrorMessage) = useState("")
    val errorWindowOpenness = useWindowOpenness()
    val (selectedLayout, setSelectedLayout) = useState(CytoscapeLayout.preferredLayout)

    val (aceMarkers, setAceMarkers) = useState<AceMarkers>(emptyArray())

    displaySimpleModal(
        errorWindowOpenness,
        "Error log",
        errorMessage,
    )

    div {
        className = ClassName("d-flex justify-content-center mb-2")
        div {
            className = ClassName("col-12")
            backgroundCard {
                graphDemoComponent {
                    this.selectedLayout = selectedLayout
                    this.setSelectedLayout = { setSelectedLayout(it) }
                    this.placeholderText = CPG_PLACEHOLDER_TEXT
                    this.preselectedLanguage = Languages.CPP
                    this.aceMarkers = aceMarkers
                    this.resultRequest = { demoRequest ->
                        val response = post(
                            "$cpgDemoApiUrl/upload-code",
                            headers = jsonHeaders,
                            body = Json.encodeToString(demoRequest),
                            loadingHandler = ::loadingHandler,
                        )

                        if (response.ok) {
                            val cpgResultNew: CpgResult = response.unsafeMap {
                                it.decodeFromJsonString()
                            }
                            setCpgResult(cpgResultNew)
                            setIsLogs(false)
                        } else {
                            setErrorMessage(response.unpackMessage())
                            errorWindowOpenness.openWindow()
                        }
                    }
                    this.resultBuilder = { builder ->
                        with(builder) {
                            div {
                                className = ClassName("card card-body p-0")
                                style = jso {
                                    height = "90%".unsafeCast<Height>()
                                }
                                cytoscapeVisualizer {
                                    graph = cpgResult.cpgGraph
                                    layout = selectedLayout
                                    aceMarkersStateSetter = setAceMarkers
                                    query = cpgResult.query
                                }
                                div {
                                    val alertStyle = when {
                                        cpgResult.query.isBlank() -> ""
                                        cpgResult.query.startsWith("Error") -> "alert-warning"
                                        else -> "alert-primary"
                                    }
                                    className = ClassName("alert $alertStyle text-sm mt-3 pb-2 pt-2 mb-0")
                                    +cpgResult.query
                                }
                            }
                        }
                    }
                    this.changeLogsVisibility = { setIsLogs { !it } }
                }
            }
        }
    }
    if (isLogs) {
        div {
            val alertStyle = if (cpgResult.logs.isNotEmpty()) {
                cpgResult.logs.forEach { log ->
                    when {
                        log.contains("ERROR") || log.startsWith("Exception:") -> pre {
                            className = ClassName("text-danger")
                            +log
                        }
                        else -> {
                            +log
                            br { }
                        }
                    }
                }

                "alert-primary"
            } else {
                +"No logs provided"

                "alert-secondary"
            }
            className = ClassName("alert $alertStyle text-sm mt-3 pb-2 pt-2 mb-0")
        }
    }
}
