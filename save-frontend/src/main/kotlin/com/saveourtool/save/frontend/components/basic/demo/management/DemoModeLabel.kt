/**
 * Clickable demo mode label
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.common.demo.RunCommandPair
import com.saveourtool.frontend.common.utils.buttonBuilder

import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

/**
 * Clickable demo mode label
 */
val demoModeLabel: FC<DemoModeLabelProps> = FC { props ->
    div {
        className = ClassName(props.classes)
        asDynamic()["data-toggle"] = "tooltip"
        asDynamic()["data-placement"] = "bottom"
        title = props.runCommand
        buttonBuilder(props.modeName, "secondary", classes = "badge badge-pill badge-secondary") {
            props.onClickCallback(props.modeName to props.runCommand)
        }
    }
}

/**
 * [Props] of [demoModeLabel]
 */
external interface DemoModeLabelProps : Props {
    /**
     * Name of possible mode that the demo can be run with
     */
    var modeName: String

    /**
     * Run command that should be executed in order to run demo with mode with name [modeName]
     */
    var runCommand: String

    /**
     * [ClassName] that should be applied to outer [div] block
     */
    var classes: String

    /**
     * Callback that is invoked when label is clicked
     */
    var onClickCallback: (RunCommandPair) -> Unit
}
