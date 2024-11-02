/**
 * Scroll-to-top button component
 */

package com.saveourtool.frontend.common.components.basic

import com.saveourtool.frontend.common.externals.fontawesome.faAngleUp
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon

import org.w3c.dom.SMOOTH
import org.w3c.dom.ScrollBehavior
import org.w3c.dom.ScrollToOptions
import react.*
import react.dom.html.ReactHTML.a
import web.cssom.ClassName

import kotlinx.browser.document
import kotlinx.browser.window

/**
 * Renders scroll to top button
 */
val scrollToTopButton = scrollToTopButton()

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
private fun scrollToTopButton() = FC<PropsWithChildren> {
    val (isVisible, setIsVisible) = useState(false)

    useEffect {
        document.addEventListener("scroll", callback = {
            setIsVisible(window.pageYOffset > 100)
        })
    }

    if (isVisible) {
        a {
            className = ClassName("scroll-to-top rounded")
            onClick = {
                window.scrollTo(ScrollToOptions(top = 0.0, behavior = ScrollBehavior.SMOOTH))
            }
            fontAwesomeIcon(icon = faAngleUp)
        }
    }
}
