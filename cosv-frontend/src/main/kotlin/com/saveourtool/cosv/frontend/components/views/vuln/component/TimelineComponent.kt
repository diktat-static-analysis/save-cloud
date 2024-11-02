@file:Suppress("FILE_NAME_INCORRECT", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.cosv.frontend.components.views.vuln.component

import com.saveourtool.common.entities.cosv.VulnerabilityExt
import com.saveourtool.common.entities.vulnerability.VulnerabilityDateDto
import com.saveourtool.frontend.common.externals.i18next.useTranslation
import com.saveourtool.frontend.common.utils.buttonBuilder

import react.*
import react.dom.html.ReactHTML.div
import web.cssom.*

import kotlinx.datetime.LocalDateTime

const val HOVERABLE_CONST = "hoverable"

val timelineComponent: FC<TimelineComponentProps> = FC { props ->
    val (t) = useTranslation("dates")
    val hoverable = props.onNodeClick?.let { HOVERABLE_CONST }.orEmpty()

    div {
        className = ClassName("mb-3")
        props.title?.let { title ->
            div {
                className = ClassName("mt-3 mb-3 text-xs text-center font-weight-bold text-primary text-uppercase")
                +title
            }
        }

        props.onAddClick?.let { onClickCallback ->
            buttonBuilder(
                label = "Add date".t(),
                style = "secondary",
                isOutline = true,
                classes = "btn btn-sm btn-primary"
            ) {
                onClickCallback()
            }
        }

        div {
            className = ClassName("p-0 timeline-container")
            div {
                className = ClassName("steps-container")
                div {
                    className = ClassName("line")
                }

                // <published> is nullable field in schema, so if it is null we should not be showing it
                props.dates
                    .toList()
                    .sortedBy { it.date }
                    .forEach { (dateTime, label) ->
                        div {
                            className =
                                    ClassName(if (!label.isSystemDateType()) "step $hoverable" else "step-non-editable")
                            if (!label.isSystemDateType()) {
                                props.onNodeClick?.let { onClickCallback ->
                                    onClick = { onClickCallback(dateTime, label.value) }
                                }
                            }
                            div {
                                className = ClassName("text-label")
                                +label.value.t()
                            }
                            div {
                                className = ClassName("date-label")
                                +dateTime.date.toString()
                            }
                        }
                        div {
                            className = ClassName("line")
                        }
                    }
                div {
                    className = ClassName("line-end")
                }
            }
        }
    }
}

/**
 * [Props] of [timelineComponent]
 */
external interface TimelineComponentProps : Props {
    /**
     * Timeline title
     */
    var title: String?

    /**
     * Map with dates where key is [LocalDateTime] and value is label
     */
    var dates: List<VulnerabilityDateDto>

    /**
     * Callback that should be invoked on add button click
     */
    var onAddClick: (() -> Unit)?

    /**
     * Callback that should be invoked on timeline node click
     */
    @Suppress("TYPE_ALIAS")
    var onNodeClick: ((LocalDateTime, String) -> Unit)?

    /**
     * Vulnerability dto of vulnerability
     */
    var vulnerability: VulnerabilityExt
}
