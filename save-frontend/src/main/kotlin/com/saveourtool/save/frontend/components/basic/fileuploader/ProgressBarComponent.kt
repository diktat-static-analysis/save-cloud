/**
 * File containing progress bar functional component
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.common.utils.toKilobytes
import com.saveourtool.frontend.common.components.basic.fileuploader.defaultProgressBarComponent

import react.FC
import react.Props

/**
 * Progress Bar [FC]
 */
@Suppress("MAGIC_NUMBER")
val progressBarComponent: FC<ProgressBarComponentProps> = FC { props ->
    defaultProgressBarComponent {
        currentProgress = if (props.total == props.current && props.total == 0L) {
            -1
        } else {
            (100 * props.current / props.total).toInt()
        }
        currentProgressMessage = if (props.current == props.total && props.total != 0L) {
            "Successfully uploaded ${props.total.toKilobytes()} KB."
        } else {
            "${props.current.toKilobytes()} / ${props.total.toKilobytes()} KB"
        }
        reset = props.flushCounters
    }
}

/**
 * [Props] for [progressBarComponent]
 */
external interface ProgressBarComponentProps : Props {
    /**
     * Amount of entity that is already marked as done (uploaded, downloaded, fixed, etc.)
     */
    var current: Long

    /**
     * Total amount of entity
     */
    var total: Long

    /**
     * Callback invoked to flush [current] and [total]
     */
    var flushCounters: () -> Unit
}
