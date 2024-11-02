@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")
@file:JsModule("multi-range-slider-react")
@file:JsNonModule

package com.saveourtool.frontend.common.externals.slider

import react.CSSProperties
import react.FC
import react.Props

/**
 * External declaration of [multiRangeSlider] react component
 */
@JsName("default")
external val multiRangeSlider: FC<MultiRangeSliderProps>

/**
 * Props of [MultiRangeSliderProps]
 */
external interface MultiRangeSliderProps : Props {
    /**
     * Slider minimum value.
     */
    var min: Float

    /**
     * Slider maximum value.
     */
    var max: Float

    /**
     * Selected with thumb minimum value.
     */
    var minValue: Float

    /**
     * Selected with thumb maximum value.
     */
    var maxValue: Float

    /**
     * Value change on step change when bar clicked or keyboard arrow key pressed.
     */
    var step: Float

    /**
     * True then slider value change with only rounded step values.
     */
    var stepOnly: Boolean

    /**
     * True then it not accept mouse wheel to change its value.
     */
    var preventWheel: Boolean

    /**
     * 	Is ruler visible or not.
     */
    var ruler: Boolean

    /**
     * 	Is label visible or not.
     */
    var label: Boolean

    /**
     * Caption on min thumb when sliding - can set on onChange/onInput event.
     */
    var minCaption: Boolean

    /**
     * Caption on max thumb when sliding - can set on onChange/onInput event.
     */
    var maxCaption: Boolean

    /**
     * Specify/override additional style.
     */
    var style: CSSProperties

    /**
     * Specify slider left part background color.
     */
    var barLeftColor: String

    /**
     * Specify slider right part background color.
     */
    var barRightColor: String

    /**
     * Specify slider inner part background color.
     */
    var barInnerColor: String

    /**
     * Specify slider left thumb background color.
     */
    var thumbLeftColor: String

    /**
     * Specify slider right thumb background color.
     */
    var thumbRightColor: String

    /**
     * Trigger when slider value changing.
     */
    var onInput: (ChangeResult) -> Unit

    /**
     * Trigger when slider value change done.
     */
    var onChange: (ChangeResult) -> Unit
}

/**
 * Result of slider value change
 */
@Suppress("USE_DATA_CLASS")
external class ChangeResult {
    /**
     * Slider minimum value.
     */
    var min: Float

    /**
     * Slider maximum value.
     */
    var max: Float

    /**
     * Changed selected with thumb minimum value.
     */
    var minValue: Float

    /**
     * Changed selected with thumb maximum value.
     */
    var maxValue: Float
}
