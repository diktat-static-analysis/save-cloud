/**
 * Class for multiple cards from a project collection view
 */

package com.saveourtool.save.frontend.components.views.projectcollection

import com.saveourtool.common.validation.FrontendRoutes

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.router.dom.Link
import web.cssom.*

/**
 * Buttons for creation and deletion of new projects and organizations
 */
internal fun ChildrenBuilder.topRightCard() {
    creationCard("/img/undraw_for_review_eqxk.svg", "organization", "/${FrontendRoutes.CREATE_ORGANIZATION}/")
    creationCard("/img/undraw_selecting_team_re_ndkb.svg", "project", "/${FrontendRoutes.CREATE_PROJECT}/")
}

/**
 * General info card
 */
internal fun ChildrenBuilder.topLeftCard() {
    div {
        className = ClassName("col-6")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                @Suppress("MAGIC_NUMBER")
                height = 12.5.rem
            }

            div {
                className = ClassName("row")

                div {
                    className = ClassName("col-3 mt-3")

                    img {
                        src = "/img/save-logo-no-bg.png"
                        @Suppress("MAGIC_NUMBER")
                        style = jso {
                            width = 8.rem
                        }
                    }
                }

                div {
                    className = ClassName("col-8 mt-3 text-left")
                    div {
                        className = ClassName("row")

                        h4 {
                            className = ClassName("text-info")
                            +"Welcome to SAVE!"
                        }
                    }
                    div {
                        className = ClassName("row")
                        p {
                            +(
                                    "This page contains a " +
                                            "list with all public and your private projects created in SAVE. " +
                                            "Your new added tools will also be shown below. To create a " +
                                            "new project and to evaluate your tool with benchmarks you will need to have " +
                                            "an organization created. "
                            )
                            b {
                                +"To participate in contests you also need to have a public project."
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ChildrenBuilder.creationCard(image: String, text: String, url: String) {
    div {
        className = ClassName("col-3")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow bordered-div")
            @Suppress("MAGIC_NUMBER")
            style = jso {
                borderWidth = 0.2.rem
                borderColor = "#0275d8".unsafeCast<BorderColor>()
                @Suppress("MAGIC_NUMBER")
                height = 12.5.rem
            }

            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-6 link-container")
                    Link {
                        className = ClassName("link-three ml-5")
                        to = url
                        style = jso {
                            textDecoration = "none".unsafeCast<TextDecoration>()
                        }
                        h4 {
                            +"Create"
                        }
                        h4 {
                            +"new"
                        }
                        h4 {
                            +text
                        }
                    }
                }

                div {
                    className = ClassName("col-6 text-center")
                    img {
                        src = image
                        style = jso {
                            width = 8.rem
                        }
                    }
                }
            }
        }
    }
}
