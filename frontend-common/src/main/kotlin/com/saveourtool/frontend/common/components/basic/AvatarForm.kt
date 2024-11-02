/**
 * Function component for avatar form
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.basic

import com.saveourtool.frontend.common.components.modal.modalAvatarBuilder
import com.saveourtool.frontend.common.externals.imageeditor.reactAvatarImageCropper

import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import web.cssom.ClassName
import web.cssom.rem
import web.file.File

val avatarForm: FC<AvatarFormProps> = FC { props ->
    modalAvatarBuilder(
        isOpen = props.isOpen,
        title = props.title,
        onCloseButtonPressed = {
            props.onCloseWindow()
        }
    ) {
        div {
            className = ClassName("shadow")
            style = jso {
                height = 18.rem
                width = 18.rem
            }
            reactAvatarImageCropper {
                apply = { file, _ ->
                    props.imageUpload(file)
                    props.onCloseWindow()
                }
            }
        }
    }
}

/**
 * AvatarForm component props
 */
external interface AvatarFormProps : Props {
    /**
     * Flag to handle avatar Window
     */
    var isOpen: Boolean

    /**
     * Title of window
     */
    var title: String

    /**
     * Callback to update state for close window.
     */
    var onCloseWindow: () -> Unit

    /**
     * Callback to upload avatar.
     */
    var imageUpload: (File) -> Unit
}
