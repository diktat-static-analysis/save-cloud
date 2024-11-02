@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.externals.progressbar

import com.saveourtool.frontend.common.themes.Colors
import react.ChildrenBuilder
import react.react

/**
 * @param progress progress and percentage
 * @param size of the circle
 * @param lineWidth of the circle's stroke
 * @param color of percentage text and "progress" portion of circle
 * @param handler
 * @param showPercentageSymbol
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.progressBar(
    progress: Float,
    size: String = "10rem",
    lineWidth: String = "5rem",
    color: String = Colors.SUCCESS.value,
    showPercentageSymbol: Boolean = false,
    handler: ChildrenBuilder.(ReactCircleProps) -> Unit = {},
) {
    // FixMe: setting textStyle as jso this does not work in Circle, investigate why
    ReactCircle::class.react {
        this.size = size
        this.lineWidth = lineWidth
        this.progress = progress.toString()
        this.progressColor = color
        this.showPercentageSymbol = showPercentageSymbol
        this.textColor = color
        handler(this)
    }
}
