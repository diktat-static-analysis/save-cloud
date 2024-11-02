@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "")

package com.saveourtool.frontend.common.components.inputform

import react.ChildrenBuilder
import react.dom.aria.ariaDescribedBy
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.span
import web.cssom.ClassName
import web.html.InputType

/**
 * @param form
 * @param classes
 * @param name
 * @param inputText
 * @param isRequired
 * @return div with a disabled input form
 */
fun ChildrenBuilder.inputTextDisabled(
    form: InputTypes,
    classes: String,
    name: String,
    inputText: String,
    isRequired: Boolean = true,
) {
    div {
        className = ClassName(classes)
        label {
            className = ClassName("form-label")
            htmlFor = form.name
            +name
            if (isRequired) {
                span {
                    className = ClassName("text-danger text-left")
                    +"*"
                }
            }
        }
        input {
            type = InputType.text
            ariaDescribedBy = "${form.name}Span"
            id = form.name
            required = false
            className = ClassName("form-control")
            disabled = true
            value = inputText
        }
    }
}
