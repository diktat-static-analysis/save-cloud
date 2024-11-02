@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.basic

import com.saveourtool.frontend.common.utils.toFixedStr

import js.core.jso
import react.FC
import react.Props
import react.dom.aria.AriaRole
import react.dom.aria.ariaValueMax
import react.dom.aria.ariaValueMin
import react.dom.aria.ariaValueNow
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.router.dom.Link
import web.cssom.*

/**
 * Functional component for project score demonstration
 *
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
val scoreCard: FC<ScoreCardProps> = FC { props ->
    div {
        className = ClassName("card border-left-info shadow h-70 py-2")
        div {
            className = ClassName("card-body")
            div {
                className = ClassName("row no-gutters align-items-center")
                div {
                    className = ClassName("col-12 row mr-2")
                    style = jso {
                        justifyContent = JustifyContent.spaceAround
                        display = Display.flex
                        alignItems = AlignItems.center
                    }
                    div {
                        className = ClassName("col-1")
                        div {
                            className = ClassName("text-xs font-weight-bold text-info text-uppercase mb-1 ml-2 justify-content-center")
                            style = jso {
                                display = Display.flex
                                alignItems = AlignItems.center
                                alignSelf = AlignSelf.start
                            }
                            +"Rating"
                        }
                        div {
                            className = ClassName("text-center h5 mb-0 font-weight-bold text-gray-800 mt-1 ml-2")
                            style = jso {
                                justifyContent = JustifyContent.center
                                display = Display.flex
                                alignItems = AlignItems.center
                                alignSelf = AlignSelf.start
                            }
                            +props.contestScore.toFixedStr(2)
                        }
                    }
                    div {
                        className = ClassName("col-10")
                        div {
                            h6 {
                                style = jso {
                                    justifyContent = JustifyContent.center
                                    display = Display.flex
                                    alignItems = AlignItems.center
                                    alignSelf = AlignSelf.center
                                }

                                props.url?.let { link ->
                                    Link {
                                        to = link
                                        +props.name
                                    }
                                } ?: run {
                                    +props.name
                                }
                            }
                        }
                        div {
                            className = ClassName("progress progress-sm mr-2")
                            div {
                                className = ClassName("progress-bar bg-info")
                                role = "progressbar".unsafeCast<AriaRole>()
                                style = jso {
                                    width = "${props.contestScore}%".unsafeCast<Width>()
                                }
                                ariaValueNow = props.contestScore
                                ariaValueMin = 0.0
                                ariaValueMax = 100.0
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ProjectScoreCardProps component props
 */
external interface ScoreCardProps : Props {
    /**
     * Name of a current project or contest (acts as a card header)
     */
    var name: String

    /**
     * Score of a project in a contest
     */
    var contestScore: Double

    /**
     * Url to the project
     */
    var url: String?
}
