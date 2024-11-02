@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.common.agent.TestSuiteExecutionStatisticDto
import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.frontend.common.components.tables.TableProps
import com.saveourtool.frontend.common.components.tables.columns
import com.saveourtool.frontend.common.components.tables.tableComponent
import com.saveourtool.frontend.common.components.tables.value
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.save.frontend.externals.chart.DataPieChart
import com.saveourtool.save.frontend.externals.chart.pieChart
import com.saveourtool.save.frontend.externals.chart.randomColor

import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.td
import web.cssom.ClassName

@Suppress(
    "MAGIC_NUMBER",
    "TYPE_ALIAS",
)
private val executionDetailsTable: FC<TableProps<TestSuiteExecutionStatisticDto>> = tableComponent(
    columns = {
        columns {
            column(id = "name", header = "Test suite", { testSuiteName }) {
                Fragment.create {
                    td {
                        className = ClassName("align-middle text-center")
                        +it.value
                    }
                }
            }
            column(id = "tests", header = "Number of tests", { countTest }) {
                Fragment.create {
                    td {
                        className = ClassName("align-middle text-center")
                        +"${it.value}"
                    }
                }
            }
            column(id = "rate", header = "Passed tests", { countWithStatusTest }) {
                Fragment.create {
                    td {
                        className = ClassName("align-middle text-center")
                        +"${it.value}"
                    }
                }
            }
        }
    },
)

/**
 * STATISTIC tab in ProjectView
 */
val projectStatisticMenu = projectStatisticMenu()

/**
 * ProjectStatisticMenu component props
 */
external interface ProjectStatisticMenuProps : Props {
    /**
     * Id of execution
     */
    var executionId: Long?

    /**
     * list of tests
     */
    var latestExecutionStatisticDtos: List<TestSuiteExecutionStatisticDto>?

    /**
     * Flag to open Menu
     */
    var isOpen: Boolean?
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "AVOID_NULL_CHECKS"
)
private fun projectStatisticMenu() = FC<ProjectStatisticMenuProps> { props ->
    val (latestExecutionStatisticDtos, setLatestExecutionStatisticDtos) = useState(props.latestExecutionStatisticDtos)

    useRequest(arrayOf(props.executionId, props.latestExecutionStatisticDtos, props.isOpen)) {
        if (props.isOpen != true && props.executionId != null) {
            val testLatestExecutions = get(
                url = "$apiUrl/testLatestExecutions?executionId=${props.executionId}&status=${TestResultStatus.PASSED}",
                headers = Headers().also {
                    it.set("Accept", "application/json")
                },
                loadingHandler = ::loadingHandler,
            )
                .unsafeMap {
                    it.decodeFromJsonString<List<TestSuiteExecutionStatisticDto>>()
                }
            setLatestExecutionStatisticDtos(testLatestExecutions)
        }
    }

    div {
        className = ClassName("row justify-content-center")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-2 mr-3")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Total number of tests by test suite"
            }
            div {
                className = ClassName("col-xl col-6 mb-4")
                val data = latestExecutionStatisticDtos?.map {
                    DataPieChart(it.testSuiteName, it.countTest, randomColor())
                }.orEmpty()
                pieChart(
                    data.toTypedArray()
                ) {
                    it.animate = true
                    it.segmentsShift = 2
                    it.radius = 47
                }
            }
        }
        // ===================== RIGHT COLUMN =======================================================================
        div {
            className = ClassName("col-6")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Latest execution"
            }

            if (props.executionId != null && latestExecutionStatisticDtos?.isNotEmpty() == true) {
                executionDetailsTable {
                    getData = { page, size ->
                        get(
                            url = "$apiUrl/testLatestExecutions?executionId=${props.executionId}&status=${TestResultStatus.PASSED}&page=$page&size=$size",
                            headers = Headers().also {
                                it.set("Accept", "application/json")
                            },
                            loadingHandler = ::loadingHandler,
                        )
                            .unsafeMap {
                                it.decodeFromJsonString<Array<TestSuiteExecutionStatisticDto>>()
                            }
                    }
                    getPageCount = null
                }
            } else {
                div {
                    className = ClassName("card shadow mb-4")
                    div {
                        className = ClassName("card-header py-3")
                        h6 {
                            className = ClassName("m-0 font-weight-bold text-primary text-center")
                            +"No executions yet"
                        }
                    }
                }
            }
        }
    }
}
