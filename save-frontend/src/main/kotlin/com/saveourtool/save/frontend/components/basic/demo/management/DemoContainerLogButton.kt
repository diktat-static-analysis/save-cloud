/**
 * Clickable demo mode label
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.utils.*
import com.saveourtool.frontend.common.components.modal.*
import com.saveourtool.frontend.common.externals.fontawesome.faArrowLeft
import com.saveourtool.frontend.common.externals.fontawesome.faArrowRight
import com.saveourtool.frontend.common.externals.fontawesome.faHistory
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.textarea
import react.useState
import web.cssom.ClassName

import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.*

private const val DEFAULT_LIMIT = 1000
private const val ROWS_TEXTAREA = 15
@Suppress("MAGIC_NUMBER")
private val stepSize = 10.minutes

/**
 * Button that fetches logs and displays it in modal
 */
val demoContainerLogButton: FC<DemoContainerLogButtonProps> = FC { props ->
    val (toTime, setToTime) = useState(getCurrentLocalDateTime())
    val (isOpen, setIsOpen) = useState(false)
    val (logs, setLogs) = useState<StringList>(emptyList())
    useRequest(arrayOf(toTime)) {
        val response = get(
            "$apiUrl/demo/${props.projectCoordinates}/logs",
            params = jso<dynamic> {
                version = "manual"
                from = (toTime - stepSize).toString()
                to = toTime.toString()
                limit = DEFAULT_LIMIT
            },
            jsonHeaders,
            ::loadingHandler,
            ::noopResponseHandler,
        )
        if (response.ok) {
            val fetchedLogs: StringList = response.decodeFromJsonString()
            setLogs(fetchedLogs)
        }
    }

    div {
        className = ClassName(props.classes)
        asDynamic()["data-toggle"] = "tooltip"
        asDynamic()["data-placement"] = "bottom"
        title = "Show container logs"
        buttonBuilder(faHistory, "secondary", isDisabled = props.isDisabled) {
            setIsOpen(true)
        }
    }

    modal { modalProps ->
        modalProps.isOpen = isOpen
        modalProps.style = largeTransparentModalStyle
        modalBuilder(
            "${props.projectCoordinates} demo logs",
            classes = "modal-lg",
            onCloseButtonPressed = { setIsOpen(false) },
            buttonBuilder = null,
            bodyBuilder = {
                div {
                    className = ClassName("d-flex justify-content-center")
                    textarea {
                        className = ClassName("form-control")
                        rows = ROWS_TEXTAREA
                        value = logs.joinToString("\n").ifEmpty { "No logs found" }
                    }
                }
                @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
                hr { }
                div {
                    className = ClassName("flex-nowrap d-flex justify-content-around align-items-center")
                    buttonBuilder(faArrowLeft, isOutline = true) { setToTime { it - stepSize } }
                    label {
                        className = ClassName("ml-3 mr-3")
                        val fromTime = toTime - stepSize
                        +"${fromTime.prettyPrint(TimeZone.currentSystemDefault())} - ${toTime.prettyPrint(TimeZone.currentSystemDefault())}"
                    }
                    buttonBuilder(faArrowRight, isOutline = true) { setToTime { minOf(it + stepSize, getCurrentLocalDateTime()) } }
                }
            },
        )
    }
}

/**
 * [Props] of [demoModeLabel]
 */
external interface DemoContainerLogButtonProps : Props {
    /**
     * [ClassName] that should be applied to outer [div] block
     */
    var classes: String

    /**
     * saveourtool [ProjectCoordinates]
     */
    var projectCoordinates: ProjectCoordinates

    /**
     * Flag that disables the button
     */
    var isDisabled: Boolean
}
