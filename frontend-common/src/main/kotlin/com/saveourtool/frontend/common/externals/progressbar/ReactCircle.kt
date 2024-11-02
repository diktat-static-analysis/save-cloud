@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:JsModule("react-circle")
@file:JsNonModule

package com.saveourtool.frontend.common.externals.progressbar

import react.*

/**
 * External declaration of [ReactCircleProps] react component
 */
@JsName("default")
external class ReactCircle : Component<ReactCircleProps, State> {
    override fun render(): ReactElement<ReactCircleProps>?
}

/**
 * Props of [ReactCircleProps]
 */
external interface ReactCircleProps : PropsWithChildren {
    /**
     * Defines the size of the circle.
     */
    var size: String

    /**
     * Defines the thickness of the circle's stroke.
     */
    var lineWidth: String

    /**
     * Update to change the progress and percentage.
     */
    var progress: String

    /**
     * Color of "progress" portion of circle (example: "#ecedf0").
     */
    var progressColor: String

    /**
     * Color of "empty" portion of circle (example: "#ecedf0").
     */
    var bgColor: String

    /**
     * Color of percentage text color (example: "#ecedf0").
     */
    var textColor: String

    /**
     * Adjust spacing of "%" symbol and number.
     */
    var percentSpacing: Long

    /**
     * Show/hide percentage value inside the circle.
     */
    var showPercentage: Boolean

    /**
     * Show/hide only the "%" symbol.
     */
    var showPercentageSymbol: Boolean

    /**
     * Custom styling for text.
     */
    var textStyle: CSSProperties
}
