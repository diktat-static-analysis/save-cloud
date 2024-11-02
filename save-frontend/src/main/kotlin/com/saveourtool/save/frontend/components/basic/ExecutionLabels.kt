@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "WildcardImport",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.common.utils.calculateRate
import com.saveourtool.common.utils.getPrecisionRate
import com.saveourtool.common.utils.getRecallRate
import com.saveourtool.common.utils.isValidScore
import com.saveourtool.frontend.common.externals.fontawesome.faRedo
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon

import js.core.jso
import react.ChildrenBuilder
import react.dom.aria.AriaRole
import react.dom.aria.ariaValueMax
import react.dom.aria.ariaValueMin
import react.dom.aria.ariaValueNow
import react.dom.events.MouseEvent
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.img
import web.cssom.ClassName
import web.cssom.Width
import web.cssom.rem
import web.html.HTMLAnchorElement

/**
 * Class contains all execution statistics values for rending
 *
 * @param executionDto
 */
internal class ExecutionStatisticsValues(executionDto: ExecutionDto?) {
    /**
     * The style for rending
     */
    val style: String

    /**
     * All tests in the current execution or 0
     */
    val allTests: String

    /**
     * Number of passed tests in the current execution or 0
     */
    val passedTests: String

    /**
     * Number of failed tests in the current execution or 0
     */
    val failedTests: String

    /**
     * Number of running tests in the current execution or 0
     */
    val runningTests: String

    /**
     * Rate of passed tests in the current execution or 0
     */
    val passRate: String

    /**
     * Precision rate in the current execution or 0
     */
    val precisionRate: String

    /**
     * Recall rate in the current execution or 0
     */
    val recallRate: String

    init {
        val isRunning = executionDto?.run { status == ExecutionStatus.RUNNING || status == ExecutionStatus.PENDING } ?: false
        val isSuccess = executionDto?.run { passedTests == allTests } ?: false
        val hasFailingTests = executionDto?.run { failedTests != 0L } ?: false
        this.style = if (hasFailingTests) {
            "danger"
        } else if (isSuccess) {
            "success"
        } else if (isRunning) {
            "info"
        } else {
            "secondary"
        }
        this.allTests = executionDto?.allTests?.toString() ?: "0"
        this.passedTests = executionDto?.passedTests?.toString() ?: "0"
        this.failedTests = executionDto?.failedTests?.toString() ?: "0"
        this.runningTests = executionDto?.runningTests?.toString() ?: "0"
        this.passRate = executionDto
            ?.let { calculateRate(it.passedTests, it.allTests) }
            ?.toString()
            ?: "0"
        this.precisionRate = executionDto
            ?.let {
                val precisionRate = it.getPrecisionRate()
                if (precisionRate.isValidScore()) {
                    precisionRate.toString()
                } else {
                    "N/A"
                }
            }
            ?: "0"
        this.recallRate = executionDto
            ?.let {
                val recallRate = it.getRecallRate()
                if (recallRate.isValidScore()) {
                    recallRate.toString()
                } else {
                    "N/A"
                }
            }
            ?: "0"
    }
}

/**
 * Function that renders Project version label, execution statistics label, pass rate label and rerun button.
 * Rerun button is rendered only if [onRerunExecution] is provided.
 *
 * @param executionDto execution that should be used as data source
 * @param isContest flag that defines whether to use contest styles or not
 * @param classes [ClassName]s that will be applied to highest div
 * @param innerClasses [ClassName]s that will be applied to each label
 * @param height height of label
 * @param onRerunExecution
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
fun ChildrenBuilder.displayExecutionInfoHeader(
    executionDto: ExecutionDto?,
    isContest: Boolean,
    classes: String = "",
    innerClasses: String = "col flex-wrap m-2",
    height: String = "h-100",
    onRerunExecution: ((MouseEvent<HTMLAnchorElement, *>) -> Unit)? = null,
) {
    val relativeWidth = onRerunExecution?.let { "min-vw-25" } ?: "min-vw-33"
    div {
        className = ClassName(classes)
        displayProjectVersion(executionDto, isContest, "$relativeWidth $innerClasses", height)
        displayPassRate(executionDto, isContest, "$relativeWidth $innerClasses", height)
        displayStatistics(executionDto, "$relativeWidth $innerClasses", height)
        displayRerunExecutionButton(executionDto, isContest, "$relativeWidth $innerClasses", height, onRerunExecution)
    }
}

/**
 * Function that renders Rerun execution button
 *
 * @param executionDto execution that should be used as data source
 * @param isContest flag that defines whether to use contest styles or not
 * @param classes [ClassName]s that will be applied to highest div
 * @param height height of label
 * @param onRerunExecution onClick callback
 */
fun ChildrenBuilder.displayRerunExecutionButton(
    executionDto: ExecutionDto?,
    isContest: Boolean,
    classes: String = "",
    height: String = "h-100",
    onRerunExecution: ((MouseEvent<HTMLAnchorElement, *>) -> Unit)?,
) {
    onRerunExecution?.let {
        val borderColor = when {
            !isContest -> "info"
            executionDto == null -> "secondary"
            executionDto.status == ExecutionStatus.ERROR || executionDto.failedTests != 0L -> "danger"
            executionDto.status == ExecutionStatus.RUNNING || executionDto.status == ExecutionStatus.PENDING -> "info"
            executionDto.status == ExecutionStatus.FINISHED -> "success"
            else -> "secondary"
        }
        div {
            className = ClassName(classes)
            div {
                className = ClassName("card border-left-$borderColor shadow $height py-2")
                div {
                    className = ClassName("card-body d-flex justify-content-start align-items-center")
                    div {
                        className = ClassName("row no-gutters mx-auto justify-content-start")
                        a {
                            href = ""
                            +"Rerun execution"
                            fontAwesomeIcon(icon = faRedo, classes = "ml-2")
                            onClick = onRerunExecution
                        }
                    }
                }
            }
        }
    }
}

/**
 * Function that renders label with project version
 *
 * @param executionDto execution that should be used as data source
 * @param isContest flag that defines whether to use contest styles or not
 * @param classes [ClassName]s that will be applied to highest div
 * @param height height of label
 */
fun ChildrenBuilder.displayProjectVersion(
    executionDto: ExecutionDto?,
    isContest: Boolean,
    classes: String = "",
    height: String = "h-100",
) {
    val statusColor = when {
        !isContest -> "bg-info"
        executionDto == null -> "bg-secondary"
        executionDto.status == ExecutionStatus.ERROR || executionDto.failedTests != 0L -> "bg-danger"
        executionDto.status == ExecutionStatus.RUNNING || executionDto.status == ExecutionStatus.PENDING -> "bg-info"
        executionDto.status == ExecutionStatus.FINISHED -> "bg-success"
        else -> "bg-secondary"
    }
    div {
        className = ClassName(classes)
        div {
            className = ClassName("card $statusColor text-white $height shadow py-2")
            div {
                className = ClassName("card-body")
                +(executionDto?.status?.name ?: "N/A")
                div {
                    className = ClassName("text-white-50 small")
                    +"Tests version: ${executionDto?.version ?: "N/A"}"
                }
            }
        }
    }
}

/**
 * A function that displays a GIF if tests not found
 *
 * @param executionDto execution that should be used as data source
 */
fun ChildrenBuilder.displayTestNotFound(executionDto: ExecutionDto?) {
    val count = executionDto?.allTests
    val status = executionDto?.status
    if (count == 0L && status != ExecutionStatus.PENDING) {
        div {
            className = ClassName("d-flex justify-content-center")
            img {
                src = "/img/sad_cat.png"
                @Suppress("MAGIC_NUMBER")
                style = jso {
                    width = 7.rem
                }
            }
        }
        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mb-4 mt-2")
            h1 {
                className = ClassName("h3 mb-0 text-gray-800")
                +"Tests not found!"
            }
        }
    } else if (count == 0L && status == ExecutionStatus.PENDING) {
        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mb-4 mt-2")
            h1 {
                className = ClassName("h3 mb-0 text-gray-800")
                +"Execution is starting..."
            }
        }
    }
}

/**
 * Function that renders pass rate label
 *
 * @param executionDto execution that should be used as data source
 * @param isContest flag that defines whether to use contest styles or not
 * @param classes [ClassName]s that will be applied to highest div
 * @param height height of label
 */
@Suppress("MAGIC_NUMBER", "TOO_LONG_FUNCTION")
fun ChildrenBuilder.displayPassRate(
    executionDto: ExecutionDto?,
    isContest: Boolean,
    classes: String = "",
    height: String = "h-100",
) {
    val values = ExecutionStatisticsValues(executionDto)
    div {
        className = ClassName(classes)
        val styles = if (isContest) {
            values.style
        } else {
            "info"
        }
        div {
            className = ClassName("card border-left-$styles shadow $height py-2")
            div {
                className = ClassName("card-body")
                div {
                    className = ClassName("row no-gutters align-items-center")
                    div {
                        className = ClassName("col mr-2")
                        div {
                            className = ClassName("text-xs font-weight-bold text-info text-uppercase mb-1")
                            +"Pass Rate"
                        }
                        div {
                            className = ClassName("row no-gutters align-items-center")
                            div {
                                className = ClassName("col-auto")
                                div {
                                    className = ClassName("h5 mb-0 mr-3 font-weight-bold text-gray-800")
                                    +"${values.passRate}%"
                                }
                            }
                            div {
                                className = ClassName("col")
                                div {
                                    className = ClassName("progress progress-sm mr-2")
                                    div {
                                        className = ClassName("progress-bar bg-info")
                                        role = "progressbar".unsafeCast<AriaRole>()
                                        style = jso {
                                            width = "${values.passRate}%".unsafeCast<Width>()
                                        }
                                        ariaValueMin = 0.0
                                        ariaValueNow = values.passRate.toDouble()
                                        ariaValueMax = 100.0
                                    }
                                }
                            }
                        }
                    }
                    div {
                        className = ClassName("col-auto")
                        i {
                            className = ClassName("fas fa-clipboard-list fa-2x text-gray-300")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Function that renders execution statistics label
 *
 * @param executionDto execution that should be used as data source
 * @param classes [ClassName]s that will be applied to highest div
 * @param height height of label
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun ChildrenBuilder.displayStatistics(
    executionDto: ExecutionDto?,
    classes: String = "",
    height: String = "h-100",
) {
    val values = ExecutionStatisticsValues(executionDto)
    div {
        className = ClassName(classes)
        div {
            className = ClassName("card border-left-${values.style} shadow $height py-2")
            div {
                className = ClassName("card-body")
                div {
                    className = ClassName("row no-gutters align-items-center")
                    div {
                        className = ClassName("col mr-2")
                        div {
                            className = ClassName("text-xs font-weight-bold text-info text-uppercase mb-1")
                            +"Tests"
                        }
                        div {
                            className = ClassName("h5 mb-0 font-weight-bold text-gray-800")
                            +values.allTests
                        }
                    }
                    div {
                        className = ClassName("col mr-2")
                        div {
                            className = ClassName("text-xs font-weight-bold text-info text-uppercase mb-1")
                            +"Running"
                        }
                        div {
                            className = ClassName("h5 mb-0 font-weight-bold text-gray-800")
                            +values.runningTests
                        }
                    }
                    div {
                        className = ClassName("col mr-2")
                        div {
                            className = ClassName("text-xs font-weight-bold text-danger text-uppercase mb-1")
                            +"Failed"
                        }
                        div {
                            className = ClassName("h5 mb-0 font-weight-bold text-gray-800")
                            +values.failedTests
                        }
                    }
                    div {
                        className = ClassName("col mr-2")
                        div {
                            className = ClassName("text-xs font-weight-bold text-success text-uppercase mb-1")
                            +"Passed"
                        }
                        div {
                            className = ClassName("h5 mb-0 font-weight-bold text-gray-800")
                            +values.passedTests
                        }
                    }
                    div {
                        className = ClassName("col mr-2")
                        div {
                            className = ClassName("text-xs font-weight-bold text-info text-uppercase mb-1")
                            +"Precision"
                        }
                        div {
                            className = ClassName("h5 mb-0 font-weight-bold text-gray-800")
                            +values.precisionRate
                        }
                    }
                    div {
                        className = ClassName("col mr-2")
                        div {
                            className = ClassName("text-xs font-weight-bold text-info text-uppercase mb-1")
                            +"Recall"
                        }
                        div {
                            className = ClassName("h5 mb-0 font-weight-bold text-gray-800")
                            +values.recallRate
                        }
                    }
                }
            }
        }
    }
}
