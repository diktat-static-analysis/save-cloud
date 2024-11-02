@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.frontend.common.components.modal

import com.saveourtool.frontend.common.externals.modal.Styles
import js.core.jso
import react.CSSProperties
import web.cssom.BackgroundColor
import web.cssom.ZIndex
import kotlin.js.json

/**
 * Maximum zIndex in the project, should be only used in modal windows
 */
const val MAX_Z_INDEX = 1000

private val defaultOverlayProperties: CSSProperties = jso {
    zIndex = MAX_Z_INDEX.unsafeCast<ZIndex>()
    backgroundColor = "rgba(255, 255, 255, 0.8)".unsafeCast<BackgroundColor>()
}

val defaultModalStyle = Styles(
    // make modal window occupy center of the screen
    content = json(
        "top" to "25%",
        "left" to "35%",
        "right" to "35%",
        "bottom" to "auto",
        "overflow" to "hide"
    ).unsafeCast<CSSProperties>(),
    overlay = defaultOverlayProperties,
)

val smallTransparentModalStyle = Styles(
    content = json(
        "top" to "10%",
        "left" to "35%",
        "right" to "35%",
        "bottom" to "2%",
        "overflow" to "hide",
        "backgroundColor" to "transparent",
        "border" to "1px solid rgba(255, 255, 255, 0.01)"
    ).unsafeCast<CSSProperties>(),
    overlay = defaultOverlayProperties,
)

val mediumTransparentModalStyle = Styles(
    content = json(
        "top" to "5%",
        "left" to "20%",
        "right" to "20%",
        "bottom" to "2%",
        "overflow" to "hide",
        "backgroundColor" to "transparent",
        "border" to "1px solid rgba(255, 255, 255, 0.01)"
    ).unsafeCast<CSSProperties>(),
    overlay = defaultOverlayProperties,
)

val largeTransparentModalStyle = Styles(
    content = json(
        "top" to "5%",
        "left" to "5%",
        "right" to "5%",
        "bottom" to "2%",
        "overflow" to "hide",
        "backgroundColor" to "transparent",
        "border" to "1px solid rgba(255, 255, 255, 0.01)"
    ).unsafeCast<CSSProperties>(),
    overlay = defaultOverlayProperties,
)

val calculatorModalStyle = Styles(
    content = json(
        "top" to "5%",
        "left" to "5%",
        "right" to "16%",
        "bottom" to "2%",
        "overflow" to "hide",
        "backgroundColor" to "transparent",
        "border" to "1px solid rgba(255, 255, 255, 0.01)"
    ).unsafeCast<CSSProperties>(),
    overlay = defaultOverlayProperties,
)

val loaderModalStyle = Styles(
    content = json(
        "top" to "25%",
        "left" to "35%",
        "right" to "35%",
        "bottom" to "45%",
        "overflow" to "hide",
        "backgroundColor" to "transparent",
        // small hack to remove modal border and make loader prettier
        "border" to "1px solid rgba(255, 255, 255, 0.01)"
    ).unsafeCast<CSSProperties>(),
    overlay = jso {
        zIndex = MAX_Z_INDEX.unsafeCast<ZIndex>()
        backgroundColor = "rgba(255, 255, 255, 1)".unsafeCast<BackgroundColor>()
    },
)
