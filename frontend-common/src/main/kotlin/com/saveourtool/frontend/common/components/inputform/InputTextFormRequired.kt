/**
 * Optional form FC
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.inputform

import com.saveourtool.frontend.common.utils.useTooltip

import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.events.ChangeEvent
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.span
import web.cssom.ClassName
import web.html.HTMLInputElement
import web.html.InputType

val inputTextFormRequired = inputTextFormRequiredWrapper()

/**
 * Properties for a [inputTextFormRequired]
 */
external interface InputTextFormRequiredProps : Props {
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
    var name: String

    /**
     * Flag that indicates if current [textValue] is valid or not
     */
    var validInput: Boolean

    /**
     * Conflict error message received from backend
     */
    var conflictMessage: String?

    /**
     * Callback invoked when a form is clicked
     */
    var onClickFun: () -> Unit

    /**
     * Callback invoked when [textValue] changed
     */
    var onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit
}

/**
 * @param form
 * @param validInput
 * @param classes
 * @param name
 * @param onChangeFun
 * @param textValue
 * @param onClickFun
 * @return div with an input form
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
)
private fun ChildrenBuilder.inputTextFormRequired(
    form: InputTypes,
    textValue: String?,
    validInput: Boolean,
    classes: String,
    name: String,
    conflictMessage: String?,
    onClickFun: () -> Unit = { },
    onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit = { }
) {
    div {
        className = ClassName(classes)
        label {
            className = ClassName("form-label mb-0")
            htmlFor = form.name
            +name
            span {
                className = ClassName("text-danger text-left")
                +"*"
            }
        }

        div {
            val inputType = if (form == InputTypes.PASSWORD) InputType.password else InputType.text
            input {
                type = inputType
                onChange = onChangeFun
                onClick = { onClickFun() }
                id = form.name
                required = true
                value = textValue
                placeholder = form.placeholder
                className = if (textValue.isNullOrEmpty()) {
                    ClassName("form-control")
                } else if (validInput) {
                    ClassName("form-control is-valid")
                } else {
                    ClassName("form-control is-invalid")
                }
                asDynamic()["data-toggle"] = "tooltip"
                asDynamic()["data-placement"] = "right"
                title = conflictMessage ?: form.tooltip
            }
            if (conflictMessage == null && !validInput && !textValue.isNullOrEmpty()) {
                div {
                    className = ClassName("invalid-feedback d-block")
                    +(form.errorMessage ?: "Input a valid ${form.str}, please.")
                }
            }
        }
    }
}

private fun inputTextFormRequiredWrapper() = FC<InputTextFormRequiredProps> { props ->
    useTooltip()
    inputTextFormRequired(
        props.form,
        props.textValue,
        props.validInput,
        props.classes,
        props.name,
        props.conflictMessage,
        props.onClickFun,
        props.onChangeFun,
    )
}
