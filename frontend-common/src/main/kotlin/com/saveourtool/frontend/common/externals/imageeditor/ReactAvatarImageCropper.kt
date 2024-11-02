@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")
@file:JsModule("react-avatar-image-cropper")
@file:JsNonModule

package com.saveourtool.frontend.common.externals.imageeditor

import org.w3c.dom.events.Event
import react.*
import web.file.File

/**
 * External declaration of [reactAvatarImageCropper] react component
 */
@JsName("default")
external val reactAvatarImageCropper: FC<ReactAvatarImageCropperProps>

/**
 * Props of [ReactAvatarImageCropperProps]
 */
@Suppress("TYPE_ALIAS")
external interface ReactAvatarImageCropperProps : Props {
    /**
     * The apply function will get the cropped blob file, you can handle it whatever you want.
     */
    var apply: (File, Event) -> Unit
}
