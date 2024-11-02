@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.cosv.frontend.components.topbar

import com.saveourtool.common.utils.URL_PATH_DELIMITER
import com.saveourtool.common.validation.FrontendCosvRoutes
import com.saveourtool.frontend.common.utils.TopBarUrl

import react.FC
import react.Props
import react.dom.aria.AriaCurrent
import react.dom.aria.ariaCurrent
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.ol
import react.router.dom.Link
import remix.run.router.Location
import web.cssom.ClassName

/**
 * Displays the URL split with "/".
 */
val topBarUrlSplits: FC<TopBarUrlSplitsProps> = FC { props ->
    nav {
        className = ClassName("navbar-nav mr-auto w-100")
        ariaLabel = "breadcrumb"
        ol {
            className = ClassName("breadcrumb mb-0")
            li {
                className = ClassName("breadcrumb-item")
                ariaCurrent = "page".unsafeCast<AriaCurrent>()
                Link {
                    to = "/"
                    // if we are on welcome page right now - need to highlight SAVE in menu
                    val textColor = if (props.location.pathname == "/") "text-warning" else "text-light"
                    className = ClassName(textColor)
                    +"COSV"
                }
            }
            props.location.pathname
                // workaround for avoiding invalid routing to /vuln/list/:param from /vuln/collection/vulnName
                .replace("${FrontendCosvRoutes.VULNERABILITY_SINGLE}", "${FrontendCosvRoutes.VULNERABILITIES}")
                .substringBeforeLast("?")
                .split(URL_PATH_DELIMITER)
                .filterNot { it.isBlank() }
                .apply {
                    val url = TopBarUrl(
                        props.location.pathname.substringBeforeLast("?")
                    )
                    forEachIndexed { index: Int, pathPart: String ->
                        url.changeUrlBeforeButton(pathPart)
                        if (url.shouldDisplayPathFragment(index)) {
                            li {
                                className = ClassName("breadcrumb-item")
                                ariaCurrent = "page".unsafeCast<AriaCurrent>()
                                if (index == size - 1) {
                                    a {
                                        className = ClassName("text-warning")
                                        +pathPart
                                    }
                                } else {
                                    Link {
                                        // removePrefix - to remove the # at the beginning and not rewrite the logic with the construction of the url
                                        to = url.currentPath.removePrefix("#")
                                        className = ClassName("text-light")
                                        +pathPart
                                    }
                                }
                            }
                        }
                        url.changeUrlAfterButton(pathPart)
                    }
                }
        }
    }
}

/**
 * [Props] of the top bar url splits component
 */
external interface TopBarUrlSplitsProps : Props {
    /**
     * User location for url analysis.
     */
    var location: Location<*>
}
