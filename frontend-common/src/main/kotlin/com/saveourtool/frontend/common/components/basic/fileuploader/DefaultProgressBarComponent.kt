/**
 * File containing progress bar functional component
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.basic.fileuploader

import com.saveourtool.frontend.common.utils.useTooltip
import js.core.jso
import react.FC
import react.Props
import react.dom.aria.AriaRole
import react.dom.html.ReactHTML.div
import react.useEffect
import web.cssom.ClassName
import web.cssom.Width

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Progress Bar [FC]
 */
@Suppress("MAGIC_NUMBER")
val defaultProgressBarComponent: FC<DefaultProgressBarComponent> = FC { props ->
    val scope = CoroutineScope(Dispatchers.Main)
    useTooltip()
    useEffect(props.currentProgress) {
        if (props.currentProgress == 100) {
            scope.launch {
                delay(1500.milliseconds)
                props.reset()
            }
        }
    }

    if (props.currentProgress >= 0L) {
        div {
            className = ClassName("progress text-center")
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "bottom"
            asDynamic()["data-original-title"] = props.currentProgressMessage
            title = props.currentProgressMessage
            if (props.currentProgress == 100) {
                className = ClassName("progress-bar bg-success")
                role = "progressbar".unsafeCast<AriaRole>()
                style = jso { width = "100%".unsafeCast<Width>() }
                +props.currentProgressMessage
            } else if (props.currentProgress >= 0) {
                div {
                    className = ClassName("progress-bar progress-bar-striped progress-bar-animated")
                    role = "progressbar".unsafeCast<AriaRole>()
                    style = jso { width = "${props.currentProgress}%".unsafeCast<Width>() }
                    +props.currentProgressMessage
                }
            }
        }
    }
}

/**
 * [Props] for [defaultProgressBarComponent]
 */
external interface DefaultProgressBarComponent : Props {
    /**
     * Current progress
     */
    var currentProgress: Int

    /**
     * Current labelText
     */
    var currentProgressMessage: String

    /**
     * Callback invoked to reset [currentProgress]
     */
    var reset: () -> Unit
}
