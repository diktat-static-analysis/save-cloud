/**
 * Beautiful logos with effects for SAVE and VULN services
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.save.frontend.externals.i18next.useTranslation

import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.span
import react.router.dom.Link
import web.cssom.*

val logoButtons: FC<Props> = FC {
    val (t) = useTranslation("index")
    div {
        className = ClassName("row logo-parent mb-5 d-flex justify-content-center")
        @Suppress("MAGIC_NUMBER")
        style = jso {
            // mt-5 is not enough in my case, but frontend is already flexible, so it should work
            marginTop = 4.rem
        }
        div {
            className = ClassName("col-2 text-center")
        }

        logo(
            "SAVE",
            "/${FrontendRoutes.SAVE}",
            "Cloud Platform for CI and Benchmarking of Code Analyzers".t(),
            "/img/save-logo-bg.jpg"
        )

        logo(
            "VULN",
            "https://cosv.gitlink.org.cn",
            "Archive of 1-Day Vulnerabilities Aggregated from Various Sources".t(),
            "/img/vuln-logo-bg.png",

        )

        div {
            className = ClassName("col-2 text-center")
        }
    }
}

private fun ChildrenBuilder.logo(
    input: String,
    url: String,
    labelText: String,
    img: String
) {
    div {
        className = ClassName("col-4 text-center logo-main")
        imgLogo(
            url,
            img,
        )

        neonLightingText(
            input,
            url,
            labelText
        )
    }
}

private fun ChildrenBuilder.imgLogo(url: String, img: String) {
    div {
        className = ClassName("col")
        Link {
            to = url
            @Suppress("MAGIC_NUMBER")
            (img {
                src = img
                style = jso {
                    width = 15.rem
                    border = "0.1rem solid hsl(186 100% 69%)".unsafeCast<Border>()
                    borderRadius = 1.rem
                }
            })
        }
    }
}

private fun ChildrenBuilder.neonLightingText(input: String, url: String, labelText: String) {
    Link {
        to = url
        div {
            className = ClassName("row mb-4")
            div {
                className = ClassName("col text-center")
                button {
                    className = ClassName("glowing-btn")
                    span {
                        className = ClassName("glowing-txt")
                        +input[0]
                        span {
                            className = ClassName("faulty-letter")
                            +input[1]
                        }
                        +input.substring(2)
                    }
                }
            }
        }

        h4 {
            className = ClassName("mt-5 mx-3 text-white")
            +labelText
        }
    }
}
