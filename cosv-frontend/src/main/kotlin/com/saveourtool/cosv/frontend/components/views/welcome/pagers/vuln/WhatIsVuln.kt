/**
 * [1 page] Main information about SAVE-cloud
 */

package com.saveourtool.cosv.frontend.components.views.welcome.pagers.vuln

import com.saveourtool.common.validation.FrontendCosvRoutes
import com.saveourtool.cosv.frontend.components.views.welcome.BIG_FONT_SIZE
import com.saveourtool.cosv.frontend.components.views.welcome.FIRST_RAW_HEIGHT
import com.saveourtool.cosv.frontend.components.views.welcome.SECOND_RAW_HEIGHT
import com.saveourtool.frontend.common.externals.i18next.TranslationFunction

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.pre
import react.dom.html.ReactHTML.strong
import react.router.dom.Link
import web.cssom.*

/**
 * rendering 3 paragraphs with info about VULN
 *
 * @param t [TranslationFunction] received from [com.saveourtool.save.frontend.externals.i18next.useTranslation] hook
 */
@Suppress("IDENTIFIER_LENGTH")
fun ChildrenBuilder.renderVulnerabilityGeneralInfo(t: TranslationFunction) {
    div {
        style = jso { color = "rgb(6, 7, 89)".unsafeCast<Color>() }

        div {
            className = ClassName("row justify-content-between mt-5")
            textCard(
                "COSV Platform".t(),
                "/img/vuln-logo-bg.png",
                "Archive importance".t(),
                "mr-3 px-4",
            )
            textCard(
                "What is vulnerability?".t(),
                "/img/undraw_question.svg",
                "Vulnerability is a weakness or flaw in a system, network, software, or hardware.".t(),
                "ml-3 px-4",
            )
        }

        div {
            className = ClassName("row mt-4 align-middle")
            div {
                className = ClassName("col-6 pl-0")
                linksCard(t)
            }
            div {
                className = ClassName("col-6 pr-0")
                cosvSchemaCard(t)
            }
        }
    }
}

private fun ChildrenBuilder.textCard(
    title: String,
    imageUrl: String,
    textStr: String,
    classes: String,
) {
    div {
        className = ClassName("card border border-primary rounded rounded-pill col $classes")
        style = jso {
            height = FIRST_RAW_HEIGHT.rem
        }
        div {
            className = ClassName("d-flex justify-content-center")
            img {
                className = ClassName("m-3")
                src = imageUrl
                style = jso {
                    @Suppress("MAGIC_NUMBER")
                    borderRadius = "50%".unsafeCast<BorderRadius>()
                    height = 9.rem
                }
                alt = "Avatar"
            }
        }
        h5 {
            style = jso {
                textAlign = TextAlign.center
            }
            +title
        }
        p {
            +textStr
        }
    }
}

@Suppress("IDENTIFIER_LENGTH")
private fun ChildrenBuilder.linksCard(
    t: TranslationFunction,
) {
    div {
        className = ClassName("card border border-primary rounded rounded-pill col")
        style = jso {
            height = SECOND_RAW_HEIGHT.rem
        }
        div {
            className = ClassName("row")

            div {
                className = ClassName("col-12 align-middle pr-4 m-3")
                pre {
                    Link {
                        +"OSV Schema "
                        to = "https://ossf.github.io/osv-schema/"
                    }
                    +"OSV Schema".t()
                    Link {
                        +"COSV Schema 1.0 "
                        to = "https://www.gitlink.org.cn/zone/CCF-ODC/source/7"
                    }
                    +"COSV Schema".t()
                    Link {
                        +"cosv4k "
                        to = "https://github.com/saveourtool/cosv4k"
                    }
                    +"cosv4k".t()
                }
            }
        }
    }
}

@Suppress("IDENTIFIER_LENGTH")
private fun ChildrenBuilder.cosvSchemaCard(
    t: TranslationFunction,
) {
    Link {
        to = "/${FrontendCosvRoutes.VULN_COSV_SCHEMA}"
        div {
            className =
                    ClassName("card button_animated_card rounded rounded-pill col justify-content-center")
            style = jso {
                height = SECOND_RAW_HEIGHT.rem
            }
            div {
                className = ClassName("row justify-content-center")

                h1 {
                    +"COSV"
                    style = jso {
                        fontSize = BIG_FONT_SIZE.rem
                    }
                }
            }
            div {
                className = ClassName("row justify-content-center")
                strong {
                    className = ClassName("d-inline-block mb-2 card-text")
                    +"Schema".t()
                }
            }
        }
    }
}
