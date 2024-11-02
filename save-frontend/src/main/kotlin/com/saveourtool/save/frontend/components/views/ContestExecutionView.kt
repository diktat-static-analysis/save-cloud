/**
 * View for tests execution history
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.common.info.UserInfo
import com.saveourtool.frontend.common.components.tables.TableProps
import com.saveourtool.frontend.common.components.tables.columns
import com.saveourtool.frontend.common.components.tables.enableExpanding
import com.saveourtool.frontend.common.components.tables.tableComponent
import com.saveourtool.frontend.common.components.tables.value
import com.saveourtool.frontend.common.components.tables.visibleColumnsCount
import com.saveourtool.frontend.common.components.views.AbstractView
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.themes.Colors
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.Style
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.externals.chart.DataPieChart
import com.saveourtool.save.frontend.externals.chart.PieChartColors
import com.saveourtool.save.frontend.externals.chart.pieChart

import js.core.jso
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.tr
import web.cssom.*

import kotlinx.datetime.Instant

/**
 * [Props] for [ContestExecutionView]
 */
external interface ContestExecutionViewProps : PropsWithChildren {
    /**
     * Info about current user
     */
    var currentUserInfo: UserInfo?

    /**
     * Name of a contest
     */
    var contestName: String

    /**
     * Name of an organization
     */
    var organizationName: String

    /**
     * Name of a project
     */
    var projectName: String
}

/**
 * A table to display execution results of a project in a contest.
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ContestExecutionView : AbstractView<ContestExecutionViewProps, State>(Style.SAVE_LIGHT) {
    @Suppress(
        "MAGIC_NUMBER",
        "TYPE_ALIAS",
    )
    private val executionsTable: FC<TableProps<ExecutionDto>> = tableComponent(
        columns = {
            columns {
                column("result", "", { status }) { cellProps ->
                    val (resColor, resIcon) = when (cellProps.row.original.status) {
                        ExecutionStatus.ERROR -> "text-danger" to faExclamationTriangle
                        ExecutionStatus.OBSOLETE -> "text-secondary" to faExclamationTriangle
                        ExecutionStatus.INITIALIZATION, ExecutionStatus.PENDING -> "text-success" to faSpinner
                        ExecutionStatus.RUNNING -> "text-success" to faSpinner
                        ExecutionStatus.FINISHED -> if (cellProps.row.original.failedTests != 0L) {
                            "text-danger" to faExclamationTriangle
                        } else {
                            "text-success" to faCheck
                        }
                    }
                    Fragment.create {
                        td {
                            fontAwesomeIcon(resIcon, classes = resColor)
                        }
                    }
                }
                column("status", "Status", { this }) { cellProps ->
                    Fragment.create {
                        td {
                            style = jso {
                                textDecoration = "underline".unsafeCast<TextDecoration>()
                                color = "blue".unsafeCast<Color>()
                                cursor = "pointer".unsafeCast<Cursor>()
                            }
                            onClick = {
                                cellProps.row.toggleExpanded(null)
                            }

                            +"${cellProps.value.status}"
                        }
                    }
                }
                column("startDate", "Start time", { startTime }) { cellProps ->
                    Fragment.create {
                        td {
                            a {
                                +(formattingDate(cellProps.value) ?: "Starting")
                            }
                        }
                    }
                }
                column("endDate", "End time", { endTime }) { cellProps ->
                    Fragment.create {
                        td {
                            a {
                                +(formattingDate(cellProps.value) ?: "Starting")
                            }
                        }
                    }
                }
                column("running", "Running", { runningTests }) { cellProps ->
                    Fragment.create {
                        td {
                            a {
                                +"${cellProps.value}"
                            }
                        }
                    }
                }
                column("passed", "Passed", { passedTests }) { cellProps ->
                    Fragment.create {
                        td {
                            a {
                                +"${cellProps.value}"
                            }
                        }
                    }
                }
                column("failed", "Failed", { failedTests }) { cellProps ->
                    Fragment.create {
                        td {
                            a {
                                +"${cellProps.value}"
                            }
                        }
                    }
                }
                column("skipped", "Skipped", { skippedTests }) { cellProps ->
                    Fragment.create {
                        td {
                            a {
                                +"${cellProps.value}"
                            }
                        }
                    }
                }
            }
        },
        tableOptionsCustomizer = {
            enableExpanding(it)
        },
        getRowProps = { row ->
            val color = when (row.original.status) {
                ExecutionStatus.ERROR -> Colors.RED
                ExecutionStatus.OBSOLETE, ExecutionStatus.PENDING, ExecutionStatus.INITIALIZATION -> Colors.GREY
                ExecutionStatus.RUNNING -> if (row.original.failedTests != 0L) Colors.DARK_RED else Colors.GREY
                ExecutionStatus.FINISHED -> if (row.original.failedTests != 0L) Colors.DARK_RED else Colors.GREEN
            }
            jso {
                style = jso {
                    background = color.value.unsafeCast<Background>()
                }
            }
        },
        renderExpandedRow = { tableInstance, row ->
            tr {
                td {
                    colSpan = tableInstance.visibleColumnsCount()
                    div {
                        className = ClassName("row")
                        displayExecutionInfoHeader(row.original, true, "row col-11")
                        div {
                            className = ClassName("col-1")
                            pieChart(
                                getPieChartData(row.original),
                            ) { pieProps ->
                                pieProps.animate = true
                                pieProps.segmentsShift = 2
                                pieProps.radius = 47
                            }
                        }
                    }
                }
            }
        }
    )

    private fun getPieChartData(execution: ExecutionDto) = execution
        .run {
            arrayOf(
                DataPieChart("Running tests", runningTests.toInt(), PieChartColors.GREY.hex),
                DataPieChart("Failed tests", failedTests.toInt(), PieChartColors.RED.hex),
                DataPieChart("Passed tests", passedTests.toInt(), PieChartColors.GREEN.hex),
            )
        }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "ForbiddenComment",
        "LongMethod",
    )
    override fun ChildrenBuilder.render() {
        executionsTable {
            tableHeader = "Executions details for contest ${props.contestName}"
            getData = { _, _ ->
                get(
                    url = "$apiUrl/contests/${props.contestName}/executions/${props.organizationName}/${props.projectName}",
                    headers = jsonHeaders,
                    loadingHandler = ::loadingHandler
                )
                    .unsafeMap {
                        it.decodeFromJsonString<Array<ExecutionDto>>()
                    }
            }
            getPageCount = null
        }
    }

    private fun formattingDate(date: Long?) = date?.let {
        Instant.fromEpochSeconds(date, 0)
            .toString()
            .replace("[TZ]".toRegex(), " ")
    }

    companion object : RStatics<ContestExecutionViewProps, State, ContestExecutionView, Context<RequestStatusContext?>>(ContestExecutionView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
