/**
 * Support rendering something as a fallback
 */

package com.saveourtool.frontend.common.components.views

import com.saveourtool.frontend.common.utils.Style
import com.saveourtool.frontend.common.utils.buttonBuilder

import js.core.jso
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.router.Navigate
import web.cssom.*

import kotlinx.browser.document
import kotlinx.browser.window

/**
 * Props of fallback component
 */
external interface FallbackViewProps : Props {
    /**
     * Text displayed in big letters
     */
    var bigText: String?

    /**
     * Small text for more vebose description
     */
    var smallText: String?

    /**
     * Whether link to the start page should be a `<a href=>` (if false) or react-routers `Link` (if true).
     * If this component is placed outside react-router's Router, then `Link` will be inaccessible.
     */
    var withRouterLink: Boolean?
}

/**
 * A Component representing fallback page with 404 error
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class FallbackView : AbstractView<FallbackViewProps, State>(Style.SAVE_LIGHT) {
    @Suppress("ForbiddenComment")
    override fun ChildrenBuilder.render() {
        // FixMe: not able to use "remove()" here due to some internal problem
        // FixMe: or fix links
        // so removing top bar for fallback view with a small hack
        document.getElementById("navigation-top-bar")
            ?.setAttribute("style", "opacity: 0; cursor: default")

        div {
            className = ClassName("text-center")
            style = jso {
                height = 40.rem
            }

            div {
                className = ClassName("error mx-auto mt-5")
                props.bigText?.let {
                    asDynamic()["data-text"] = it
                }
                +"${props.bigText}"
            }

            p {
                className = ClassName("lead text-gray-800 mb-3")
                +"${props.smallText}"
            }

            if (props.withRouterLink == true) {
                Navigate {
                    to = "/"
                    this@render.buttonBuilder("Back to the main page", style = "info") { }
                }
            } else {
                a {
                    href = "${window.location.origin}/"
                    buttonBuilder("Back to the main page", style = "info") { }
                }
            }

            div {
                className = ClassName("row mt-3 justify-content-center")
                div {
                    className = ClassName("col-4")
                    p {
                        className = ClassName("lead text-gray-800")
                        +"Report a problem:"
                    }

                    a {
                        className = ClassName("mt-3")
                        href = "https://github.com/saveourtool/save-cloud"
                        img {
                            src = "/img/github.svg"
                            style = jso {
                                width = 5.rem
                                height = 5.rem
                                cursor = "pointer".unsafeCast<Cursor>()
                            }
                        }
                    }
                }
            }
        }
    }
}
