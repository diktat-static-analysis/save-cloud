@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.test.analysis

import com.saveourtool.common.test.analysis.metrics.NoDataAvailable
import com.saveourtool.common.test.analysis.metrics.RegularTestMetrics
import com.saveourtool.common.test.analysis.metrics.TestMetrics

import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import web.cssom.ClassName

/**
 * Displays [TestMetrics].
 *
 * @see TestMetrics
 * @see TestMetricsProps
 */
val testMetricsView: FC<TestMetricsProps> = FC { props ->
    div {
        className = ClassName("testMetrics")

        when (val metrics = props.testMetrics) {
            is NoDataAvailable -> noDataAvailable()
            is RegularTestMetrics -> with(metrics) {
                ul {
                    style = listStyle

                    li {
                        nowrap {
                            +"Test runs: "

                            boldText(runCount)

                            +" total / "

                            boldText(failureCount) {
                                color = failureColor
                            }

                            +" failure(s) / "

                            boldText(ignoredCount)

                            +" ignored"
                        }
                    }

                    li {
                        labelledValue {
                            label = "Failure rate"
                            value = failureRatePercentagePretty()
                        }
                    }

                    li {
                        labelledValue {
                            label = "Flip rate"
                            labelTooltip = FLIP_RATE_DESCRIPTION
                            value = flipRatePercentagePretty()
                        }
                    }

                    averageDurationOrNull?.let { averageDuration ->
                        li {
                            labelledValue {
                                label = "Duration (average)"
                                value = averageDuration
                            }
                        }
                    }

                    medianDurationOrNull?.let { medianDuration ->
                        li {
                            labelledValue {
                                label = "Duration (median)"
                                value = medianDuration
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("CUSTOM_GETTERS_SETTERS")
private val String.percent: String
    get() {
        return when {
            endsWith('%') -> this
            else -> "$this%"
        }
    }

@Suppress("CUSTOM_GETTERS_SETTERS")
private val Any.percent: String
    get() {
        return toString().percent
    }

/**
 * Properties of [testMetricsView].
 *
 * @see testMetricsView
 */
external interface TestMetricsProps : Props {
    /**
     * The test metrics to render.
     */
    var testMetrics: TestMetrics
}

private fun RegularTestMetrics.failureRatePercentagePretty(): String =
        when {
            failureRatePercentage == 0 && failureCount > 0 -> "<1".percent
            else -> failureRatePercentage.percent
        }

private fun RegularTestMetrics.flipRatePercentagePretty(): String =
        when {
            flipRatePercentage == 0 && flipCount > 0 -> "<1".percent
            else -> flipRatePercentage.percent
        }
