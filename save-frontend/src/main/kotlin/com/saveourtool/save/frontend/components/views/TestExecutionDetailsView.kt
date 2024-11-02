@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.common.domain.TestResultDebugInfo
import com.saveourtool.frontend.common.http.getDebugInfoFor
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.decodeFromJsonString
import com.saveourtool.frontend.common.utils.multilineText
import com.saveourtool.frontend.common.utils.multilineTextWithIndices
import com.saveourtool.save.core.result.Crash
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.Pass

import react.*
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.samp
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.tr
import react.router.useParams
import web.cssom.ClassName

/**
 * A component to display details about test execution
 *
 * @return a function component
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION", "TOO_LONG_FUNCTION")
val testExecutionDetailsView: FC<Props> = FC {
    val (status, setStatus) = useState("Loading...")
    val (testResultDebugInfo, setTestResultDebugInfo) = useState<TestResultDebugInfo?>(null)

    // fixme: after https://github.com/saveourtool/save-cloud/issues/364 can be passed via history state to avoid requests
    useRequest {
        val testResultDebugInfoResponse = getDebugInfoFor(useParams()["testId"]!!.toLong())
        if (testResultDebugInfoResponse.ok) {
            setTestResultDebugInfo(testResultDebugInfoResponse.decodeFromJsonString<TestResultDebugInfo>())
        } else {
            setStatus("Additional test info is not available (code ${testResultDebugInfoResponse.status})")
        }
    }

    testResultDebugInfo?.let {
        displayTestResultDebugInfo(testResultDebugInfo)
    }
        ?: fallback(status)
}

/**
 * Display status of [TestResultDebugInfo]
 *
 * @param testResultDebugInfo
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun ChildrenBuilder.displayTestResultDebugInfoStatus(testResultDebugInfo: TestResultDebugInfo) {
    val status = testResultDebugInfo.testStatus
    val longMessage: String = when (status) {
        is Pass -> (status.message ?: "").ifBlank { "Completed successfully without additional information" }
        is Fail -> status.reason
        is Ignored -> status.reason
        is Crash -> status.description
    }
    +"${status::class.simpleName} with message:"
    br { }
    multilineText(longMessage)
}

/**
 * Display [TestResultDebugInfo]
 *
 * @param testResultDebugInfo
 */
@Suppress("TOO_LONG_FUNCTION")
fun ChildrenBuilder.displayTestResultDebugInfo(testResultDebugInfo: TestResultDebugInfo) {
    table {
        className = ClassName("table table-bordered")
        tbody {
            tr {
                td {
                    +"Command"
                }
                td {
                    small {
                        samp {
                            +(testResultDebugInfo.debugInfo?.execCmd ?: "N/A")
                        }
                    }
                }
            }
            tr {
                td {
                    +"Test status"
                }
                td {
                    displayTestResultDebugInfoStatus(testResultDebugInfo)
                }
            }
            with(testResultDebugInfo.debugInfo!!) {
                listOf(
                    "stdout" to ::stdout,
                    "stderr" to ::stderr
                )
            }.forEach { (title, getContent) ->
                tr {
                    td {
                        +title
                    }
                    td {
                        small {
                            samp {
                                getContent()?.let(::multilineTextWithIndices)
                                    ?: +"N/A"
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ChildrenBuilder.fallback(status: String) = div {
    +status
}
