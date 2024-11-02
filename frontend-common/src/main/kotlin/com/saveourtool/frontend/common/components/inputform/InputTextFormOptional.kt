/**
 * Optional form FC
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.inputform

import com.saveourtool.frontend.common.externals.fontawesome.faQuestionCircle
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.useTooltipAndPopover

import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.aria.ariaDescribedBy
import react.dom.events.ChangeEvent
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.sup
import web.cssom.ClassName
import web.html.HTMLInputElement
import web.html.InputType

/**
 * constant FC to avoid re-creation
 */
val inputTextFormOptional = inputTextFormOptionalWrapper()

/**
 * Properties for a [inputTextFormOptional]
 */
external interface InputTextFormOptionalProps : Props {
    /**
     * Type of form
     */
    var form: InputTypes

    /**
     * Current input value
     */
    var textValue: String?

    /**
     * HTML bootstrap classes (className)
     */
    var classes: String

    /**
     * Label that will be displayed as an input title
     */
    var name: String?

    /**
     * Flag that indicates if current [textValue] is valid or not
     */
    var validInput: Boolean?

    /**
     * Conflict error message received from backend
     */
    var conflictMessage: String?

    /**
     * Callback invoked when [textValue] changed
     */
    var onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit
}

/**
 * Unfortunately we had to wrap temporary this method with FC because of a useEffect statement
 *
 * @param form
 * @param textValue
 * @param classes
 * @param name
 * @param validInput
 * @param onChangeFun
 * @return div with an input form
 */
@Suppress(
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
    "TOO_LONG_FUNCTION",
    "LongMethod"
)
private fun ChildrenBuilder.inputTextFormOptional(
    form: InputTypes,
    textValue: String?,
    classes: String,
    name: String?,
    validInput: Boolean?,
    conflictErrorMessage: String?,
    onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit,
) {
    div {
        className = ClassName(classes)

        name?.let { name ->
            label {
                className = ClassName("form-label mb-0")
                htmlFor = form.name
                +name
            }
        }

        form.popover?.let {
            sup {
                className = ClassName("form-popover")
                fontAwesomeIcon(icon = faQuestionCircle)
                tabIndex = 0
                asDynamic()["popover-placement"] = "left"
                asDynamic()["popover-content"] = it.content
                asDynamic()["popover-title"] = it.title
                asDynamic()["data-trigger"] = "focus"
            }
        }
        input {
            type = InputType.text
            onChange = onChangeFun
            ariaDescribedBy = "${form.name}Span"
            id = form.name
            required = false
            value = textValue
            placeholder = form.placeholder
            form.tooltip?.let {
                title = it
                asDynamic()["data-toggle"] = "tooltip"
                asDynamic()["data-placement"] = "bottom"
            }
            className = if (textValue.isNullOrEmpty()) {
                ClassName("form-control")
            } else if (validInput != false) {
                ClassName("form-control is-valid")
            } else {
                ClassName("form-control is-invalid")
            }
        }
        if (conflictErrorMessage == null && validInput == false && !textValue.isNullOrEmpty()) {
            div {
                className = ClassName("invalid-feedback d-block")
                +(form.errorMessage ?: "Please input a valid ${form.str}")
            }
        }
    }
}

/**
 * @return functional component (wrapper for ChildrenBuilder)
 */
private fun inputTextFormOptionalWrapper() = FC<InputTextFormOptionalProps> { props ->
    useTooltipAndPopover()
    inputTextFormOptional(
        props.form,
        props.textValue,
        props.classes,
        props.name,
        props.validInput,
        props.conflictMessage,
        props.onChangeFun,
    )
}
