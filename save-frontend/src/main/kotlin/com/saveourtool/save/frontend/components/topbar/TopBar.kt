/**
 * Top bar of web page
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "WildcardImport")

package com.saveourtool.save.frontend.components.topbar

import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.info.UserInfo
import js.core.jso

import react.*
import react.dom.html.ButtonHTMLAttributes
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.nav
import react.router.useLocation
import web.cssom.ClassName
import web.cssom.Width
import web.cssom.vw
import web.html.ButtonType
import web.html.HTMLButtonElement

/**
 * [Props] of the top bar component
 */
external interface TopBarProps : PropsWithChildren {
    /**
     * Currently logged in user, or `null`.
     */
    var userInfo: UserInfo?
}

/**
 * @param faIcon
 * @param text
 * @param isSelected
 * @param handler
 * @return button
 */
fun ChildrenBuilder.dropdownEntry(
    faIcon: FontAwesomeIconModule?,
    text: String,
    isSelected: Boolean = false,
    handler: ChildrenBuilder.(ButtonHTMLAttributes<HTMLButtonElement>) -> Unit = { },
) = button {
    type = ButtonType.button
    val active = if (isSelected) "active" else ""
    className = ClassName("btn btn-no-outline dropdown-item rounded-0 shadow-none $active")
    faIcon?.let {
        fontAwesomeIcon(icon = faIcon) {
            it.className = "fas fa-sm fa-fw mr-2 text-gray-400"
        }
    }
    +text
    handler(this)
}

/**
 * A component for web page top bar.
 *
 * @return a function component
 */
fun topBar() = FC<TopBarProps> { props ->
    val location = useLocation()
    nav {
        className =
                ClassName("navbar navbar-expand navbar-dark bg-dark topbar mb-3 static-top shadow mr-1 ml-1 rounded")
        style = jso {
            width = 100.vw
        }
        id = "navigation-top-bar"
        topBarUrlSplits {
            this.location = location
        }
        topBarLinks {
            this.location = location
        }
        topBarUserField {
            userInfo = props.userInfo
        }
    }
}
