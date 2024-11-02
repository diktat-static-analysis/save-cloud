@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.common.agent.TestExecutionDto
import com.saveourtool.common.agent.TestExecutionExtDto
import com.saveourtool.common.domain.TestResultDebugInfo
import com.saveourtool.frontend.common.components.tables.visibleColumnsCount
import com.saveourtool.frontend.common.externals.fontawesome.faExternalLinkAlt
import com.saveourtool.frontend.common.utils.buttonBuilder
import com.saveourtool.save.core.result.Crash
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.result.TestStatus

import react.FC
import react.Props
import react.dom.html.ReactHTML.samp
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.tr
import react.router.useNavigate
import tanstack.table.core.Table
import web.cssom.ClassName

const val EXTRA_INFO_COLUMN_WIDTH = 3

/**
 * A function component that renders info about [TestResultDebugInfo] into a table [tableInstance]
 *
 * @param testResultDebugInfo data that should be displayed
 * @param tableInstance a table, into which this data is added
 * @param organizationProjectPath
 * @param testExecutionDto
 * @return a function component
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun testStatusComponent(
    organizationProjectPath: String,
    testResultDebugInfo: TestResultDebugInfo,
    tableInstance: Table<TestExecutionExtDto>,
    testExecutionDto: TestExecutionDto,
): FC<Props> = FC {
    val shortMessage: String = when (val status: TestStatus = testResultDebugInfo.testStatus) {
        is Pass -> (status.shortMessage ?: "").ifBlank { "Completed successfully without additional information" }
        is Fail -> status.shortReason
        is Ignored -> status.reason
        is Crash -> status.message
    }
    val numColumns = tableInstance.visibleColumnsCount()
    val useNavigate = useNavigate()

    tr {
        className = ClassName("table-sm")
        td {
            colSpan = EXTRA_INFO_COLUMN_WIDTH
            +"Container ID"
        }
        td {
            colSpan = numColumns - EXTRA_INFO_COLUMN_WIDTH
            small {
                samp {
                    +"${testExecutionDto.agentContainerId} : ${testExecutionDto.agentContainerName}"
                }
            }
        }
    }
    tr {
        className = ClassName("table-sm")
        td {
            colSpan = EXTRA_INFO_COLUMN_WIDTH
            +"Executed command"
        }
        td {
            colSpan = numColumns - EXTRA_INFO_COLUMN_WIDTH
            small {
                samp {
                    +(testResultDebugInfo.debugInfo?.execCmd ?: "N/A")
                }
            }
        }
    }
    tr {
        className = ClassName("table-sm")
        td {
            className = ClassName("align-middle")
            colSpan = EXTRA_INFO_COLUMN_WIDTH
            +"Reason (additional info: "
            buttonBuilder(
                icon = faExternalLinkAlt,
                classes = "text-primary pl-0 pt-0 btn-sm",
                style = ""
            ) {
                useNavigate(
                    to = "/$organizationProjectPath/history/" +
                            "execution/${testExecutionDto.executionId}/" +
                            "test/${testExecutionDto.requiredId()}"
                )
            }
            +" )"
        }
        td {
            colSpan = numColumns - EXTRA_INFO_COLUMN_WIDTH
            small {
                samp {
                    +shortMessage
                }
            }
        }
    }
}

/**
 * A function component that renders info about execution [failReason] into a table [tableInstance]
 *
 * @param failReason reason of execution fail
 * @param tableInstance a table, into which this data is added
 * @return a function component
 */
fun <D : Any> executionStatusComponent(
    failReason: String,
    tableInstance: Table<D>
): FC<Props> = FC {
    tr {
        td {
            colSpan = 2
            +"Execution fail reason:"
        }
        td {
            colSpan = tableInstance.visibleColumnsCount() - 2
            small {
                samp {
                    +failReason
                }
            }
        }
    }
}
