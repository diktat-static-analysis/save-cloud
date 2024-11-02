@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.frontend.common.components.modal

import com.saveourtool.frontend.common.externals.fontawesome.faTimesCircle
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.externals.modal.Styles
import com.saveourtool.frontend.common.utils.WindowOpenness
import com.saveourtool.frontend.common.utils.buttonBuilder
import js.core.jso
import react.CSSProperties

import react.ChildrenBuilder
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.pre
import react.dom.html.ReactHTML.span
import web.cssom.ClassName
import web.cssom.rem
import web.html.ButtonType

/**
 * Universal function to create modals with bootstrap styles inside react modals.
 *
 * @param isOpen modal openness indicator - should be in state
 * @param title title of the modal that will be shown in top-left corner
 * @param bodyBuilder callback that defined modal body content
 * @param classes classes that will be applied to bootstrap modal div
 * @param modalStyle [Styles] that will be applied to react modal
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS", "LAMBDA_IS_NOT_LAST_PARAMETER")
fun ChildrenBuilder.displayModal(
    isOpen: Boolean,
    title: String,
    bodyBuilder: ChildrenBuilder.() -> Unit,
    classes: String = "",
    modalStyle: Styles = mediumTransparentModalStyle,
    onCloseButtonPressed: (() -> Unit)? = null,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = modalStyle
        modalBuilder(title, classes, null, onCloseButtonPressed, bodyBuilder, buttonBuilder)
    }
}

/**
 * Universal function to create modals with bootstrap styles inside react modals.
 *
 * @param isOpen modal openness indicator - should be in state
 * @param title title of the modal that will be shown in top-left corner
 * @param bodyBuilder callback that defined modal body content
 * @param classes classes that will be applied to bootstrap modal div
 * @param modalStyle [Styles] that will be applied to react modal
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 * @param customWidth
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS", "LAMBDA_IS_NOT_LAST_PARAMETER")
fun ChildrenBuilder.displayModal(
    isOpen: Boolean,
    title: String,
    bodyBuilder: ChildrenBuilder.() -> Unit,
    classes: String = "",
    modalStyle: Styles = mediumTransparentModalStyle,
    onCloseButtonPressed: (() -> Unit)? = null,
    customWidth: CSSProperties? = null,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = modalStyle
        modalBuilder(title, classes, customWidth, onCloseButtonPressed, bodyBuilder, buttonBuilder)
    }
}

/**
 * Universal function to create modals with bootstrap styles inside react modals.
 *
 * @param isOpen modal openness indicator - should be in state
 * @param title title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param modalStyle [Styles] that will be applied to react modal
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 * @return modal
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.displayModal(
    isOpen: Boolean,
    title: String,
    message: String,
    modalStyle: Styles = mediumTransparentModalStyle,
    onCloseButtonPressed: (() -> Unit)? = null,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) = doCreateDisplayModal(false, isOpen, title, message, modalStyle, onCloseButtonPressed, buttonBuilder)

/**
 * Universal function to create modals with click condition styles inside react modals
 *
 * @param isOpen modal openness indicator - should be in state
 * @param modalStyle that will be applied to react modal
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 * @param title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param clickBuilder lambda that generates several click in modal
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.displayModalWithCheckBox(
    title: String,
    message: String,
    isOpen: Boolean,
    modalStyle: Styles = mediumTransparentModalStyle,
    onCloseButtonPressed: (() -> Unit)? = null,
    buttonBuilder: ChildrenBuilder.() -> Unit,
    clickBuilder: ChildrenBuilder.() -> Unit
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = modalStyle
        modalBuilder(
            title = title,
            onCloseButtonPressed = onCloseButtonPressed,
            bodyBuilder = {
                h2 {
                    className = ClassName("h6 text-gray-800 mb-2")
                    +message
                }
                div {
                    className = ClassName("d-sm-flex justify-content-center form-check")
                    clickBuilder()
                }
            },
            buttonBuilder = buttonBuilder
        )
    }
}

/**
 * Universal function to create modals with bootstrap styles inside react modals.
 *
 * @param opener [WindowOpenness]
 * @param title title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param modalStyle [Styles] that will be applied to react modal
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.displayModal(
    opener: WindowOpenness,
    title: String,
    message: String,
    modalStyle: Styles = mediumTransparentModalStyle,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    displayModal(opener.isOpen(), title, message, modalStyle, opener.closeWindowAction(), buttonBuilder)
}

/**
 * Universal function to create modals for confirmation.
 *
 * @param windowOpenness
 * @param title title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param modalStyle [Styles] that will be applied to react modal
 */
fun ChildrenBuilder.displaySimpleModal(
    windowOpenness: WindowOpenness,
    title: String,
    message: String,
    modalStyle: Styles = mediumTransparentModalStyle,
) {
    displayModal(
        isOpen = windowOpenness.isOpen(),
        title = title,
        message = message,
        modalStyle = modalStyle,
        onCloseButtonPressed = windowOpenness.closeWindowAction()
    ) {
        buttonBuilder("Ok") {
            windowOpenness.closeWindow()
        }
    }
}

/**
 * Universal function to create modals with bootstrap styles.
 *
 * @param usePreTag whether to use `ReactHTML.pre` tag
 * @param title title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 */
fun ChildrenBuilder.modalBuilder(
    usePreTag: Boolean,
    title: String,
    message: String,
    onCloseButtonPressed: (() -> Unit)?,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    val customWidth: CSSProperties? = if (usePreTag) jso { width = 40.rem } else null
    modalBuilder(
        title = title,
        onCloseButtonPressed = onCloseButtonPressed,
        bodyBuilder = {
            if (!usePreTag) {
                h2 {
                    className = ClassName("h6 text-gray-800 mb-2")
                    +message
                }
            } else {
                pre {
                    className = ClassName("text-gray-800 mb-2 overflow-x:hidden")
                    +message
                }
            }
        },
        buttonBuilder = buttonBuilder,
        customWidth = customWidth
    )
}

/**
 * Universal function to create modals with bootstrap styles.
 *
 * @param title title of the modal that will be shown in top-left corner
 * @param classes
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param bodyBuilder lambda that generates body of modal
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 * @param customWidth
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
fun ChildrenBuilder.modalBuilder(
    title: String,
    classes: String = "",
    customWidth: CSSProperties? = null,
    onCloseButtonPressed: (() -> Unit)?,
    bodyBuilder: ChildrenBuilder.() -> Unit,
    buttonBuilder: (ChildrenBuilder.() -> Unit)?,
) {
    div {
        className = ClassName("modal-dialog $classes")
        div {
            className = ClassName("modal-content")
            customWidth.let {
                style = it
            }
            onCloseFun(
                title,
                onCloseButtonPressed,
            )
            div {
                className = ClassName("modal-body")
                bodyBuilder()
            }
            buttonBuilder?.let {
                div {
                    className = ClassName("modal-footer")
                    it()
                }
            }
        }
    }
}

/**
 * Creates modals with bootstrap styles for uploading avatars.
 *
 * @param title title of the modal that will be shown in top-left corner
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 * @param isOpen
 */
fun ChildrenBuilder.modalAvatarBuilder(
    isOpen: Boolean,
    title: String,
    onCloseButtonPressed: (() -> Unit),
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = mediumTransparentModalStyle
        div {
            className = ClassName("modal-dialog")
            div {
                className = ClassName("modal-content")
                onCloseFun(
                    title,
                    onCloseButtonPressed,
                )
                div {
                    className = ClassName("justify-content-center modal-footer")
                    buttonBuilder()
                }
                div {
                    className = ClassName("modal-body")
                }
            }
        }
    }
}

/**
 * @param title
 * @param onCloseButtonPressed
 */
fun ChildrenBuilder.onCloseFun(
    title: String,
    onCloseButtonPressed: (() -> Unit)?,
) {
    div {
        className = ClassName("modal-header")
        h5 {
            className = ClassName("modal-title")
            +title
        }
        onCloseButtonPressed?.let {
            button {
                type = ButtonType.button
                className = ClassName("close")
                asDynamic()["data-dismiss"] = "modal"
                ariaLabel = "Close"
                span {
                    fontAwesomeIcon(icon = faTimesCircle)
                    onClick = { onCloseButtonPressed() }
                }
            }
        }
    }
}

@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
private fun ChildrenBuilder.doCreateDisplayModal(
    usePreTag: Boolean,
    isOpen: Boolean,
    title: String,
    message: String,
    modalStyle: Styles = mediumTransparentModalStyle,
    onCloseButtonPressed: (() -> Unit)? = null,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = modalStyle
        modalBuilder(usePreTag, title, message, onCloseButtonPressed, buttonBuilder)
    }
}
