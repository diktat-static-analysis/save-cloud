/**
 * Current demo status
 */

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.common.demo.DemoStatus

import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import web.cssom.ClassName

private const val HIDE_TIMEOUT_DEFAULT_MILLIS = 100
private const val HIDE_TIMEOUT_ON_ERROR_MILLIS = 800

/**
 * Display current demo status wrapped into bordered colorful pill
 *
 * @param demoStatus current status of demo e.g. [DemoStatus.STOPPED], [DemoStatus.RUNNING] etc.
 */
internal fun ChildrenBuilder.renderStatusLabel(demoStatus: DemoStatus) {
    div {
        className = ClassName("col-6 d-flex justify-content-center")
        div {
            val borderStyle = when (demoStatus) {
                DemoStatus.NOT_CREATED -> "border-dark"
                DemoStatus.STARTING, DemoStatus.STOPPING -> "border-warning"
                DemoStatus.RUNNING -> "border-success"
                DemoStatus.STOPPED -> "border-secondary"
                DemoStatus.ERROR -> "border-danger"
            }
            id = "status-label"
            className = ClassName("border $borderStyle d-flex align-items-center justify-content-between rounded-pill m-3")
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "top"
            asDynamic()["data-html"] = true
            asDynamic()["data-original-title"] = demoStatus.statusTooltip
            asDynamic()["data-hide-timeout"] = if (demoStatus == DemoStatus.ERROR) {
                HIDE_TIMEOUT_ON_ERROR_MILLIS
            } else {
                HIDE_TIMEOUT_DEFAULT_MILLIS
            }
            div {
                className = ClassName("col m-3 flex-wrap")
                label {
                    className = ClassName("m-0")
                    +"Status"
                }
            }
            div {
                className = ClassName("col m-3 flex-wrap")
                label {
                    className = ClassName("m-0")
                    +demoStatus.name
                }
            }
        }
    }
}
