/**
 * View, which represents the COSV schema
 */

package com.saveourtool.cosv.frontend.components.views.vuln

import com.saveourtool.cosv.frontend.components.views.vuln.utils.COSV_SCHEMA_JSON
import com.saveourtool.cosv.frontend.components.views.vuln.utils.cosvFieldsDescriptionsMap
import com.saveourtool.cosv.frontend.components.views.vuln.utils.keysOnlyFromCosv
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.Style
import com.saveourtool.frontend.common.utils.buttonBuilder
import com.saveourtool.frontend.common.utils.particles
import com.saveourtool.frontend.common.utils.useBackground

import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.pre
import react.useState
import web.cssom.*

@Suppress("TOO_MANY_LINES_IN_LAMBDA", "PARAMETER_NAME_IN_OUTER_LAMBDA")
val cosvSchemaView: FC<Props> = FC {
    particles()
    useBackground(Style.VULN_DARK)
    val (textInModal, setTextInModal) = useState<Pair<String, String>>()

    div {
        className = ClassName("row justify-content-center")
        div {
            className = ClassName("col-3 mt-3 mb-5")
            div {
                className = ClassName("card card-body bg-light border-dark text-gray-800 shadow")
                div {
                    className = ClassName("col-3 d-flex align-items-center")
                    img {
                        className = ClassName("rounded pr-4 mx-5 my-3")
                        src = "/img/undraw_important.svg"
                        style = jso {
                            @Suppress("MAGIC_NUMBER")
                            height = 8.rem
                        }
                    }
                    h5 {
                        style = jso { textAlign = TextAlign.center }
                        +title
                    }
                }

                // FixMe: JSON.stringify not working in kotlin js
                COSV_SCHEMA_JSON.drop(1)
                    .dropLast(1)
                    .split("\n")
                    .forEach {
                        val str = it.replace("\\", "")
                        // extract key from schema
                        val key = str.takeWhile { it != ':' }.replace("\"", "").trim()

                        div {
                            className = ClassName("row")

                            // match key with description
                            val cosvKey = cosvFieldsDescriptionsMap.firstOrNull { (k, _) ->
                                k == key
                            }

                            // show schema, where each key is the button, which will show description about this key vid `onClick`
                            pre {
                                className = ClassName("mb-2")
                                cosvKey?.let { (key, value) ->
                                    // hold the tabulations
                                    +str.takeWhile { it != '\"' }
                                    // make from key the button
                                    buttonBuilder(
                                        key.substringAfter("."),
                                        style = if (key in keysOnlyFromCosv) "info" else "primary",
                                        classes = "btn-sm"
                                    ) {
                                        setTextInModal(key to value)
                                    }
                                    +" :"
                                    // print the type, i.e. value
                                    +str.dropWhile { it != ':' }.drop(1)
                                } ?: run {
                                    // just print raw string, if no key with description matched
                                    +str
                                }
                            }
                        }
                    }
            }
        }
        div {
            className = ClassName("col-4 mt-3")
            div {
                className = ClassName("card card-body bg-light border-dark text-gray-800 shadow")
                style = jso {
                    width = "50rem".unsafeCast<Width>()
                    height = "36rem".unsafeCast<Height>()
                }
                id = "sticky-sidebar"

                div {
                    className = ClassName("row justify-content-center mt-1")
                    img {
                        src = "/img/schema.png"
                        style = jso {
                            height = 10.rem
                            width = 10.rem
                        }
                    }
                }

                div {
                    className = ClassName("row justify-content-center")
                    div {
                        className = ClassName("col-2 text-right")
                        a {
                            href = "https://www.gitlink.org.cn/zone/CCF-ODC/source/7"
                            buttonBuilder(
                                "Schema",
                                style = "outline-primary",
                                classes = "rounded-pill btn",
                                isOutline = false
                            ) { }
                        }
                    }
                    div {
                        className = ClassName("col-2 text-left")
                        a {
                            href = "https://www.gitlink.org.cn/zone/CCF-ODC/source/7"
                            buttonBuilder(
                                "More",
                                style = "outline-primary",
                                classes = "rounded-pill btn",
                                isOutline = false
                            ) { }
                        }
                    }
                }
                div {
                    className = ClassName("row justify-content-center mt-4 mx-3")
                    h3 {
                        className = ClassName("text-gray-800")
                        +(textInModal
                            ?.first
                            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            ?: "<- Press on the JSON key to get details")
                    }
                }
                div {
                    className = ClassName("row justify-content-center mt-2 mx-3")
                    h5 {
                        +(textInModal?.second ?: "")
                    }
                }
            }
        }
    }
}
