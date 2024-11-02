@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.utils

import com.saveourtool.frontend.common.components.modal.displayModalWithCheckBox

import org.w3c.fetch.Response
import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import web.cssom.ClassName
import web.html.ButtonType
import web.html.InputType

val actionButton: FC<ButtonWithActionProps> = FC { props ->
    val windowOpenness = useWindowOpenness()
    val (displayTitle, setDisplayTitle) = useState(props.title)
    val (displayMessage, setDisplayMessage) = useState(props.message)
    val (isError, setError) = useState(false)
    val (isClickMode, setClickMode) = useState(false)

    val action = useDeferredRequest {
        val response = props.sendRequest(isClickMode)(this)
        if (response.ok) {
            props.onActionSuccess(isClickMode)
        } else {
            setDisplayTitle(props.errorTitle)
            setDisplayMessage(response.unpackMessage())
            setError(true)
            windowOpenness.openWindow()
        }
    }

    div {
        button {
            type = ButtonType.button
            className = ClassName(props.classes)
            props.buttonStyleBuilder(this)
            onClick = {
                setDisplayTitle(props.title)
                setDisplayMessage(props.message)
                setClickMode(false)
                windowOpenness.openWindow()
            }
        }
    }

    displayModalWithCheckBox(
        title = displayTitle,
        message = displayMessage,
        isOpen = windowOpenness.isOpen(),
        onCloseButtonPressed = {
            if (isError) {
                setError(false)
            }
            windowOpenness.closeWindow()
        },
        buttonBuilder = {
            if (isError) {
                buttonBuilder("Ok") {
                    windowOpenness.closeWindow()
                    setError(false)
                }
            } else {
                props.modalButtons(action, windowOpenness.closeWindowAction(), this, isClickMode)
            }
        },
        clickBuilder = {
            if (props.conditionClick && !isError) {
                div {
                    className = ClassName("d-sm-flex justify-content-center form-check")
                    div {
                        className = ClassName("d-sm-flex justify-content-center form-check")
                        div {
                            input {
                                className = ClassName("form-check-input")
                                type = InputType.checkbox
                                value = isClickMode
                                id = "click"
                                checked = isClickMode
                                onChange = {
                                    setClickMode(!isClickMode)
                                }
                            }
                        }
                        div {
                            label {
                                className = ClassName("click")
                                htmlFor = "click"
                                +props.clickMessage
                            }
                        }
                    }
                }
            }
        }
    )
}

/**
 * Button with modal for some action
 *
 * @return noting
 */
external interface ButtonWithActionProps : Props {
    /**
     * Title of the modal
     */
    var title: String

    /**
     * Error title of the modal
     */
    var errorTitle: String

    /**
     * Message of the modal
     */
    var message: String

    /**
     * Message when clicked
     */
    var clickMessage: String

    /**
     * If the action (request) is successful, this is done
     *
     * @param isClickMode is checkBox status
     */
    @Suppress("TYPE_ALIAS")
    var onActionSuccess: (isClickMode: Boolean) -> Unit

    /**
     * Button View
     */
    var buttonStyleBuilder: (ChildrenBuilder) -> Unit

    /**
     * Classname for the button
     */
    var classes: String

    /**
     * Modal buttons
     *
     * @param action is the main action of the buttons
     * @param closeWindow is the action of closing the window and assigning the status false to the checkBox
     * @param ChildrenBuilder
     * @param isClickMode is checkBox status
     * @return buttons
     */
    @Suppress("TYPE_ALIAS")
    var modalButtons: (
        action: () -> Unit,
        closeWindow: () -> Unit,
        ChildrenBuilder,
        isClickMode: Boolean,
    ) -> Unit

    /**
     * Condition for click
     */
    var conditionClick: Boolean

    /**
     * function passes arguments to call the request
     *
     * @param isClickMode is checkBox status
     * @return lazy response
     */
    @Suppress("TYPE_ALIAS")
    var sendRequest: (isClickMode: Boolean) -> DeferredRequestAction<Response>
}
