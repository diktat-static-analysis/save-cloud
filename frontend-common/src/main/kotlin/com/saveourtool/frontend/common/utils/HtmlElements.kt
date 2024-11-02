/**
 * File that contains html elements that are used multiple times in the project
 */

package com.saveourtool.frontend.common.utils

import com.saveourtool.common.entities.*
import com.saveourtool.frontend.common.externals.fontawesome.FontAwesomeIconModule
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon

import js.core.jso
import react.ChildrenBuilder
import react.StateInstance
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.span
import web.cssom.BorderRadius
import web.cssom.ClassName
import web.html.ButtonType

/**
 * @param project
 */
fun ChildrenBuilder.privacySpan(project: ProjectDto) {
    span {
        className = ClassName("border ml-2 pr-1 pl-1 text-xs text-muted ")
        style = jso {
            borderRadius = "2em".unsafeCast<BorderRadius>()
        }
        +if (project.isPublic) "public" else "private"
    }
}

/**
 * @param classes
 * @param text
 */
fun ChildrenBuilder.spanWithClassesAndText(classes: String, text: String) {
    span {
        className = ClassName("border ml-2 pr-1 pl-1 text-xs $classes")
        style = jso {
            borderRadius = "2em".unsafeCast<BorderRadius>()
        }
        +text
    }
}

/**
 * @param icon
 * @param isActive
 * @param tooltipText
 * @param onClickFun
 */
fun ChildrenBuilder.buttonWithIcon(
    icon: FontAwesomeIconModule,
    isActive: Boolean,
    tooltipText: String,
    onClickFun: () -> Unit
) {
    button {
        type = ButtonType.button
        title = tooltipText
        val active = if (isActive) {
            "active"
        } else {
            ""
        }
        className = ClassName("btn btn-outline-secondary $active")
        fontAwesomeIcon(icon = icon)
        onClick = {
            onClickFun()
        }
        asDynamic()["data-toggle"] = "tooltip"
        asDynamic()["data-placement"] = "bottom"
    }
}

/**
 * @param icon
 * @param tooltipText
 * @param buttonMode
 * @param currentModeState
 * @param onClickFun
 */
fun <T : Enum<T>> ChildrenBuilder.buttonWithIcon(
    icon: FontAwesomeIconModule,
    tooltipText: String,
    buttonMode: T,
    currentModeState: StateInstance<T>,
    onClickFun: () -> Unit,
) {
    val (currentMode, setCurrentMode) = currentModeState
    buttonWithIcon(
        icon = icon,
        isActive = buttonMode == currentMode,
        tooltipText = tooltipText,
    ) {
        setCurrentMode(buttonMode)
        onClickFun()
    }
}
