/**
 * Utility methods used for rendering of contest list view
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.frontend.common.externals.fontawesome.FontAwesomeIconModule
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p
import web.cssom.*

/**
 * @param title
 * @param icon
 */
fun ChildrenBuilder.title(title: String, icon: FontAwesomeIconModule) {
    div {
        className = ClassName("row justify-content-center")
        h4 {
            style = jso {
                color = "#5a5c69".unsafeCast<Color>()
            }
            fontAwesomeIcon(icon = icon)

            className = ClassName("mt-3 mb-4")
            +title
        }
    }
}

/**
 * @param selectedTab
 * @param tabsList
 * @param setSelectedTab
 * @param navClassName
 */
fun ChildrenBuilder.tab(
    selectedTab: String,
    tabsList: List<String>,
    navClassName: String = "nav nav-tabs mb-4",
    setSelectedTab: (String) -> Unit
) {
    div {
        className = ClassName("row justify-content-center")

        nav {
            className = ClassName(navClassName)
            tabsList.forEachIndexed { i, value ->
                li {
                    key = i.toString()
                    className = ClassName("nav-item")
                    val classVal =
                            if (selectedTab == value) {
                                " active font-weight-bold"
                            } else {
                                ""
                            }
                    p {
                        className = ClassName("nav-link $classVal text-gray-800")
                        onClick = {
                            if (selectedTab != value) {
                                setSelectedTab(value)
                            }
                        }
                        style = jso {
                            cursor = "pointer".unsafeCast<Cursor>()
                        }

                        +value
                    }
                }
            }
        }
    }
}
