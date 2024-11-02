/**
 * Function component for different demos
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.demo.run

import com.saveourtool.common.demo.DemoDto
import com.saveourtool.common.demo.DemoResult
import com.saveourtool.common.demo.DemoRunRequest
import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.utils.Languages
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.buttonBuilder
import com.saveourtool.frontend.common.utils.post
import com.saveourtool.frontend.common.utils.selectorBuilder
import com.saveourtool.frontend.common.utils.useDeferredRequest
import com.saveourtool.frontend.common.utils.useRequest
import com.saveourtool.save.frontend.components.basic.codeeditor.codeEditorComponent
import com.saveourtool.save.frontend.externals.reactace.AceThemes

import js.core.asList
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.strong
import web.cssom.ClassName
import web.file.FileReader
import web.html.InputType

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DEFAULT_CODE = "\"Your code here\""

/**
 * [FC] to display components for demo
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "KDOC_WITHOUT_RETURN_TAG",
    "LongMethod",
    "TYPE_ALIAS"
)
val demoRunComponent: FC<DemoRunComponentProps> = FC { props ->
    val (demoDto, setDemoDto) = useState(DemoDto.empty)
    val (selectedDemoMode, setSelectedDemoMode) = useState<String?>(null)
    useRequest {
        get(
            url = "$demoApiUrl/manager/${props.projectCoordinates}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<DemoDto>()
            }
            .let {
                setDemoDto(it)
                setSelectedDemoMode(it.runCommands.keys.first())
            }
    }

    val (demoRunRequest, setDemoRunRequest) = useState(DemoRunRequest.empty)
    val (demoResult, setDemoResult) = useState<DemoResult?>(null)
    val (codeLines, setCodeLines) = useState(DEFAULT_CODE)

    val sendRunRequest = useDeferredRequest {
        val result: DemoResult = post(
            "$demoApiUrl/${props.projectCoordinates}/run",
            jsonHeaders,
            Json.encodeToString(
                demoRunRequest.copy(
                    codeLines = codeLines.lines(),
                    mode = selectedDemoMode.orEmpty(),
                )
            ),
            ::loadingHandler,
            ::noopResponseHandler,
        )
            .decodeFromJsonString()
        setDemoResult(result)
    }

    div {
        className = ClassName("")
        div {
            className = ClassName("row")
            div {
                className = ClassName("col-6")
                codeEditorComponent {
                    editorTitle = "Input code"
                    selectedTheme = props.selectedTheme
                    selectedMode = props.selectedMode
                    savedText = codeLines
                    draftText = codeLines
                    onDraftTextUpdate = { code -> setCodeLines(code) }
                    isDisabled = false
                }
            }

            div {
                className = ClassName("col-6")
                codeEditorComponent {
                    editorTitle = "Output code"
                    selectedTheme = props.selectedTheme
                    selectedMode = props.selectedMode
                    savedText = demoResult?.outputText?.joinToString("\n").orEmpty()
                    draftText = demoResult?.outputText?.joinToString("\n").orEmpty()
                    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
                    onDraftTextUpdate = { }
                    isDisabled = true
                }
            }
        }
        div {
            className = ClassName("row mt-2 mb-1 d-flex justify-content-center")
            div {
                className = ClassName("col-2 mr-1")
                selectorBuilder(
                    selectedDemoMode.orEmpty(),
                    demoDto.runCommands.keys.toList(),
                    "custom-select",
                    isDisabled = demoDto.getAvailableMods().size == 1,
                ) { event ->
                    setSelectedDemoMode(event.target.value)
                }
            }
        }
        demoDto.configName?.let { configName ->
            div {
                className = ClassName("mb-1 mt-1 d-flex justify-content-center")
                label {
                    className = ClassName("btn btn-outline-secondary m-0")
                    val reader = FileReader().apply {
                        onload = { event ->
                            setDemoRunRequest { runRequest ->
                                (event.target.asDynamic()["result"] as String?)?.let {
                                    runRequest.copy(
                                        config = it.split("\n")
                                    )
                                } ?: runRequest
                            }
                        }
                    }
                    input {
                        type = InputType.file
                        multiple = false
                        hidden = true
                        onChange = { event ->
                            event.target.files!!.asList()
                                .firstOrNull()
                                ?.let { file ->
                                    reader.readAsText(file, "UTF-8")
                                }
                        }
                    }
                    fontAwesomeIcon(icon = faUpload)
                    val uploadOrReplace = if (demoRunRequest.config.isNullOrEmpty()) {
                        "Upload"
                    } else {
                        "Replace"
                    }
                    strong { +"$uploadOrReplace $configName" }
                }
            }
        }
        div {
            className = ClassName("mb-1 d-flex justify-content-center")
            buttonBuilder("Send run request") {
                sendRunRequest()
            }
        }
        div {
            className = ClassName("ml-1 mr-1")
            demoOutputComponent {
                this.demoResult = demoResult
            }
        }
    }
}

/**
 * DemoComponent [Props]
 */
external interface DemoRunComponentProps : Props {
    /**
     * Theme for Ace Editor
     */
    var selectedTheme: AceThemes

    /**
     * Mode for Ace Editor
     */
    var selectedMode: Languages

    /**
     * saveourtool [ProjectCoordinates]
     */
    var projectCoordinates: ProjectCoordinates
}
