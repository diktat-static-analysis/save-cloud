/**
 * Just info about us on welcome page
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.externals.fontawesome.faGithub
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.buttonBuilder
import com.saveourtool.save.frontend.externals.i18next.useTranslation

import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.p
import react.router.dom.Link
import react.router.useNavigate
import web.cssom.ClassName
import web.cssom.TextAlign

val cardAboutUs: FC<Props> = FC {
    val (t) = useTranslation("index")
    val navigate = useNavigate()

    div {
        className = ClassName("col-3 shadow mx-3 mt-2")
        div {
            className = ClassName("row d-flex justify-content-center")
            cardImage("/img/icon3.png")
        }

        div {
            className = ClassName("row d-flex justify-content-center text-gray-900 mt-2")

            h5 {
                style = jso {
                    textAlign = TextAlign.center
                }
                +"About Us".t()
            }
        }

        div {
            className = ClassName("row d-flex justify-content-center")
            div {
                className = ClassName("col-6")
                p {
                    +"We are just a group of several developers working on this community project.".t()
                }
                div {
                    className = ClassName("row d-flex justify-content-center mt-1")
                    buttonBuilder(
                        "About Us".t(),
                        style = "secondary rounded-pill",
                        isOutline = false
                    ) {
                        navigate(to = "/${FrontendRoutes.ABOUT_US}")
                    }
                }
            }

            div {
                className = ClassName("col-6")
                p {
                    +"We kindly ask you not to break this service and report any problems that you will find to our Github.".t()
                }
                a {
                    className = ClassName("btn btn-secondary rounded-pill")
                    href = "https://github.com/saveourtool/save-cloud"
                    fontAwesomeIcon(icon = faGithub, classes = "mr-2")
                    +"  Save-cloud"
                }
                a {
                    className = ClassName("btn btn-secondary rounded-pill mt-2 mb-3")
                    href = "https://github.com/saveourtool/save-cli"
                    fontAwesomeIcon(icon = faGithub, classes = "mr-2")
                    +"  Save-cli  "
                }
                p {
                    +"Please also read our".t()
                    Link {
                        +" ${"Terms of Usage".t()}"
                        to = "/${FrontendRoutes.TERMS_OF_USE}"
                    }
                    +" ${"and".t()} "
                    Link {
                        +"Cookie policy".t()
                        to = "/${FrontendRoutes.COOKIE}"
                    }
                    +"."
                }
            }
        }
    }
}
