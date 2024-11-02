@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.frontend.common.components.basic

import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.utils.WithRequestStatusContext
import com.saveourtool.frontend.common.utils.useRequest

import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.span
import react.useState
import web.cssom.ClassName

/**
 * SelectFormRequired component props
 */
external interface SelectFormRequiredProps<D : Any> : Props {
    /**
     * Type of 'select'
     */
    var formType: InputTypes

    /**
     * Flag to valid select
     */
    var validInput: Boolean?

    /**
     * classes of 'select'
     */
    var classes: String

    /**
     * select name
     */
    var formName: String?

    /**
     * lambda invoked once to fetch data for selection
     */
    var getData: suspend (WithRequestStatusContext) -> List<D>

    /**
     * Currently chosen field
     */
    var selectedValue: String

    /**
     * Method to get string that should be shown
     */
    var dataToString: (D) -> String

    /**
     * Message shown on invalid input
     */
    var errorMessage: String?

    /**
     * Message shown on no options fetched
     */
    var notFoundErrorMessage: String?

    /**
     * Flag that disables the form
     */
    var disabled: Boolean?

    /**
     * Add custom elements under the form label in order to create new item.
     */
    var addNewItemChildrenBuilder: ((ChildrenBuilder) -> Unit)?

    /**
     * Array of dependencies of [getData] [useRequest]
     */
    var getDataRequestDependencies: Array<dynamic>

    /**
     * Extra classes that should be under [select] tag
     */
    var selectClasses: String

    /**
     * Callback invoked when form is changed
     */
    @Suppress("TYPE_ALIAS")
    var onChangeFun: (D?) -> Unit
}

/**
 * @return [FC] of required selection input form
 */
@Suppress(
    "TOO_MANY_PARAMETERS",
    "TOO_LONG_FUNCTION",
    "LongParameterList",
    "TYPE_ALIAS",
    "LongMethod",
    "ComplexMethod"
)
fun <D : Any> selectFormRequired() = FC<SelectFormRequiredProps<D>> { props ->
    val (elements, setElements) = useState(listOf<D>())

    useRequest(props.getDataRequestDependencies) {
        setElements((props.getData)(this))
    }

    div {
        className = ClassName("${props.classes} mt-1")
        props.formName?.let { formName ->
            div {
                className = ClassName("d-flex justify-content-between")
                label {
                    className = ClassName("form-label")
                    htmlFor = props.formType.name
                    +formName
                    span {
                        className = ClassName("text-danger")
                        id = "${props.formType.name}Span"
                        +"*"
                    }
                }
            }
            props.addNewItemChildrenBuilder?.let { addNewItemBuilder ->
                small {
                    className = ClassName("text-right")
                    addNewItemBuilder(this)
                }
            }
        }

        form {
            className = ClassName("input-group needs-validation")
            select {
                id = props.formType.name
                required = true
                disabled = props.disabled
                elements.find {
                    props.dataToString(it) == props.selectedValue
                } ?: run {
                    option {
                        disabled = true
                        +""
                    }
                }
                value = props.selectedValue
                elements.forEach { element ->
                    option {
                        +props.dataToString(element)
                    }
                }
                className = when {
                    value == "" || value == null -> ClassName("form-control ${props.selectClasses}")
                    props.validInput == true -> ClassName("form-control ${props.selectClasses} is-valid")
                    props.validInput == false -> ClassName("form-control ${props.selectClasses} is-invalid")
                    else -> ClassName("form-control ${props.selectClasses}")
                }
                onChange = { event ->
                    elements.find {
                        props.dataToString(it) == event.target.value
                    }?.let {
                        props.onChangeFun(it)
                    }
                }
            }

            if (elements.isEmpty()) {
                props.notFoundErrorMessage?.let { notFoundErrorMessage ->
                    div {
                        className = ClassName("invalid-feedback d-block")
                        +notFoundErrorMessage
                    }
                }
            } else if (props.validInput == false) {
                div {
                    className = ClassName("invalid-feedback d-block")
                    +(props.errorMessage ?: "Please input a valid ${props.formType.str}")
                }
            }
        }
    }
}
