/**
 * This card provides a global information about SaveOurTool
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.frontend.common.components.basic.markdown
import com.saveourtool.save.frontend.externals.i18next.useTranslation
import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.p
import web.cssom.ClassName
import web.cssom.TextAlign

val cardServiceInfo: FC<Props> = FC {
    val (t) = useTranslation("index")
    div {
        className = ClassName("col-3 shadow mx-3 mt-2")
        div {
            className = ClassName("row d-flex justify-content-center")
            cardImage("/img/icon2.png")
        }

        div {
            className = ClassName("row d-flex justify-content-center text-gray-900 mt-2")

            h5 {
                style = jso {
                    textAlign = TextAlign.center
                }
                +"Multiple different services".t()
            }
        }

        div {
            className = ClassName("row")
            div {
                className = ClassName("col-12")
                p {
                    b {
                        +"SaveOurTool "
                    }
                    +"provides Intelligent Services for developers of code analysis tools. Our two main directions:".t()
                }
                p {
                    +"1. "
                    b {
                        i {
                            +"SAVE "
                        }
                    }
                    +"- a platform for a distributed Cloud CI of code analyzers with a special test framework. With SAVE you can:".t()
                }
                p {
                    markdown("quickly establish testing and CI of your analyzer".t().trimMargin())
                }
                p {
                    +"2. "
                    b {
                        i {
                            +"VULN "
                        }
                    }
                    +"- a platform for reporting, aggregation and deduplication of 1-day Vulnerabilities.".t()
                }
                p {
                    className = ClassName("text-gray-700")
                    +"Also we establish contests in the area of code analysis.".t()
                }
            }
        }
    }
}
