@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.cosv.frontend.components.topbar

import com.saveourtool.common.validation.FrontendCosvRoutes
import com.saveourtool.frontend.common.externals.i18next.useTranslation

import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.router.dom.Link
import remix.run.router.Location
import web.cssom.ClassName

/**
 * If [Location.pathname] has more slashes then [TOP_BAR_PATH_SEGMENTS_HIGHLIGHT],
 * there is no need to highlight topbar element as we have `/demo` and `/project/.../demo`
 */
private const val TOP_BAR_PATH_SEGMENTS_HIGHLIGHT = 4

/**
 * Displays the static links that do not depend on the url.
 */
@Suppress("LongMethod", "TOO_LONG_FUNCTION")
val topBarLinks: FC<TopBarLinksProps> = FC { props ->
    val (t) = useTranslation("topbar")

    val vulnTopbarLinks = sequenceOf(
        TopBarLink(hrefAnchor = FrontendCosvRoutes.VULN_CREATE.path, text = "Propose vulnerability".t()),
        TopBarLink(hrefAnchor = FrontendCosvRoutes.VULNERABILITIES.path, text = "Vulnerabilities list".t()),
        TopBarLink(hrefAnchor = FrontendCosvRoutes.VULN_TOP_RATING.path, text = "Top Rating".t()),
    )

    ul {
        className = ClassName("navbar-nav mx-auto")
        vulnTopbarLinks
            .forEach { elem ->
                li {
                    className = ClassName("nav-item")
                    if (elem.isExternalLink) {
                        a {
                            className = ClassName("nav-link d-flex align-items-center text-light me-2 active")
                            href = elem.hrefAnchor
                            +elem.text
                        }
                    } else {
                        Link {
                            className = ClassName(
                                "nav-link d-flex align-items-center me-2 ${
                                    textColor(
                                            elem.hrefAnchor,
                                            props.location
                                        )
                                } active mx-2 text-nowrap col-auto"
                            )
                            to = elem.hrefAnchor
                            +elem.text
                        }
                    }
                }
            }
    }
}

/**
 * [Props] of the top bar links component
 */
external interface TopBarLinksProps : Props {
    /**
     * The location is needed to change the color of the text.
     */
    var location: Location<*>
}

/**
 * @property hrefAnchor the link
 * @property text the link text
 * @property isExternalLink
 */
data class TopBarLink(
    val hrefAnchor: String,
    val text: String,
    val isExternalLink: Boolean = false,
)

private fun textColor(
    hrefAnchor: String,
    location: Location<*>,
): String {
    val isMainPage = (location.pathname.count { it == '/' } == 1) && hrefAnchor.isBlank()
    val isNeedToHighlightTopBar = (hrefAnchor.isNotBlank() &&
            location.pathname.endsWith(hrefAnchor) && location.pathname.count { it == '/' } < TOP_BAR_PATH_SEGMENTS_HIGHLIGHT) ||
            isMainPage

    return if (isNeedToHighlightTopBar) {
        "text-warning"
    } else {
        "text-light"
    }
}
