@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.modal.displayModalWithCheckBox
import csstype.ClassName
import org.w3c.fetch.Response
import react.*
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label

val actionButton: FC<ButtonWithActionProps> = FC { props ->
    val windowOpenness = useWindowOpenness()
    val (displayTitle, setDisplayTitle) = useState(props.title)
    val (displayMessage, setDisplayMessage) = useState(props.message)
    val (isError, setError) = useState(false)
    val (isClickMode, setClickMode) = useState(false)
    val (numberOfButton, setNumberOfButton) = useState(0)

    val closeCheckBoxWindow = {
        setClickMode(false)
        windowOpenness.closeWindow()
    }

    val setButtonNumber = { number: Int ->
        setNumberOfButton(number)
    }

    val action = useDeferredRequest {
        val response = props.sendRequest(isClickMode, numberOfButton)(this)
        if (response.ok) {
            props.onActionSuccess(isClickMode, numberOfButton)
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
                windowOpenness.openWindow()
            }
        }
    }

    displayModalWithCheckBox(
        title = displayTitle,
        message = displayMessage,
        isOpen = windowOpenness.isOpen(),
        onCloseButtonPressed = windowOpenness.closeWindowAction(),
        buttonBuilder = {
            if (isError) {
                buttonBuilder("Ok") {
                    windowOpenness.closeWindow()
                    setError(false)
                }
            } else {
                props.modalButtons(action, closeCheckBoxWindow, this, isClickMode, setButtonNumber)
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
     * @param buttonNumber is number of the button pressed
     */
    @Suppress("TYPE_ALIAS")
    var onActionSuccess: (isClickMode: Boolean, buttonNumber: Int) -> Unit

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
     * @param setButtonNumber with a large number of buttons, it allows to identify the button using a number
     * @return buttons
     */
    @Suppress("TYPE_ALIAS")
    var modalButtons: (
        action: () -> Unit,
        closeWindow: () -> Unit,
        ChildrenBuilder,
        isClickMode: Boolean,
        setButtonNumber: (Int) -> Unit,
    ) -> Unit

    /**
     * Condition for click
     */
    var conditionClick: Boolean

    /**
     * function passes arguments to call the request
     *
     * @param isClickMode is checkBox status
     * @param buttonNumber is number of the button pressed
     * @return lazy response
     */
    @Suppress("TYPE_ALIAS")
    var sendRequest: (isClickMode: Boolean, buttonNumber: Int) -> DeferredRequestAction<Response>
}
