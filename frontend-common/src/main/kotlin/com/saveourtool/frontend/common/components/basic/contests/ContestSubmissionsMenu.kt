@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.frontend.common.components.basic.contests

import com.saveourtool.common.entities.contest.ContestResult
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.frontend.common.components.tables.TableProps
import com.saveourtool.frontend.common.components.tables.columns
import com.saveourtool.frontend.common.components.tables.tableComponent
import com.saveourtool.frontend.common.components.tables.value
import com.saveourtool.frontend.common.utils.*

import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import web.cssom.*

/**
 * SUBMISSIONS tab in ContestView
 */
val contestSubmissionsMenu = contestSubmissionsMenu()

@Suppress(
    "MAGIC_NUMBER",
    "TYPE_ALIAS",
)
private val myProjectsTable: FC<TableProps<ContestResult>> = tableComponent(
    columns = {
        columns {
            column(id = "project_name", header = "Project Name", { this }) { cellContext ->
                Fragment.create {
                    td {
                        Link {
                            cellContext.value.let {
                                to = "/contests/${it.contestName}/${it.organizationName}/${it.projectName}"
                                +"${it.organizationName}/${it.projectName}"
                            }
                        }
                    }
                }
            }
            column(id = "sdk", header = "SDK", { this }) { cellCtx ->
                Fragment.create {
                    td {
                        +cellCtx.value.sdk
                    }
                }
            }
            column(id = "submission_time", header = "Last submission time", { this }) { cellCtx ->
                Fragment.create {
                    td {
                        +(cellCtx.value.submissionTime?.toString()?.replace("T", " ") ?: "No data")
                    }
                }
            }
            column(id = "status", header = "Last submission status", { this }) { cellCtx ->
                Fragment.create {
                    td {
                        cellCtx.value.let { displayStatus(it.submissionStatus, it.hasFailedTest, it.score) }
                    }
                }
            }
        }
    },
    initialPageSize = 10,
    useServerPaging = false,
)

/**
 * ContestSubmissionsMenu component [Props]
 */
external interface ContestSubmissionsMenuProps : Props {
    /**
     * Name of a current contest
     */
    var contestName: String
}

private fun ChildrenBuilder.displayStatus(status: ExecutionStatus, hasFailedTests: Boolean, score: Double?) {
    span {
        className = when (status) {
            ExecutionStatus.INITIALIZATION, ExecutionStatus.PENDING -> ClassName("")
            ExecutionStatus.RUNNING -> ClassName("")
            ExecutionStatus.ERROR -> ClassName("text-danger")
            ExecutionStatus.OBSOLETE -> ClassName("text-secondary")
            ExecutionStatus.FINISHED -> if (hasFailedTests) {
                ClassName("text-danger")
            } else {
                ClassName("text-success")
            }
        }
        +"${status.name} "
    }
    displayScore(status, score)
}

private fun ChildrenBuilder.displayScore(status: ExecutionStatus, score: Double?) {
    if (status == ExecutionStatus.FINISHED) {
        span {
            +"${score?.let { ("${it.toFixed(2)}/100") }}"
        }
    }
}

private fun contestSubmissionsMenu(
) = FC<ContestSubmissionsMenuProps> { props ->
    div {
        className = ClassName("d-flex justify-content-center")
        div {
            className = ClassName("col-8")
            myProjectsTable {
                tableHeader = "My Submissions"
                getData = { _, _ ->
                    get(
                        url = "$apiUrl/contests/${props.contestName}/my-results",
                        headers = jsonHeaders,
                        ::loadingHandler,
                    )
                        .decodeFromJsonString<Array<ContestResult>>()
                }
                getPageCount = null
            }
        }
    }
}
