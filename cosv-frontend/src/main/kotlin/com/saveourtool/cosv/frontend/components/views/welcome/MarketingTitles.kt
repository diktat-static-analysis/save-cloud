/**
 * Utility methods for beautiful titles/slogans on welcome view
 */

package com.saveourtool.cosv.frontend.components.views.welcome

import com.saveourtool.frontend.common.externals.fontawesome.faChevronDown
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h3
import web.cssom.*

/**
 * @param textColor
 * @param isDark
 */
fun ChildrenBuilder.vulnWelcomeMarketingTitle(textColor: String, isDark: Boolean = false) {
    div {
        className = ClassName("col-4 text-left mt-5 mx-5 $textColor")
        marketingTitle("Vulnerability", isDark)
        marketingTitle("Database", isDark)
        marketingTitle(" and", isDark)
        marketingTitle("Benchmark", isDark)
        marketingTitle("Archive", isDark)
        h3 {
            if (isDark) {
                style = jso {
                    color = "rgb(6, 7, 89)".unsafeCast<Color>()
                }
            }
            className = ClassName("mt-4")
            +"A huge storage of known vulnerabilities."
        }
    }
}

/**
 * @param str
 * @param isDark
 */
fun ChildrenBuilder.marketingTitle(str: String, isDark: Boolean) {
    div {
        if (isDark) {
            style = jso {
                color = "rgb(6, 7, 89)".unsafeCast<Color>()
            }
        }
        className = ClassName("mb-0 mt-0")
        h1Bold(str[0].toString())
        h1Normal(str.substring(1, str.length))
    }
}

/**
 * @param col
 */
@Suppress("MAGIC_NUMBER")
fun ChildrenBuilder.chevron(col: String) {
    div {
        className = ClassName("mt-5 row justify-content-center")
        h1 {
            className = ClassName("mt-5 animate__animated animate__pulse animate__infinite")
            style = jso {
                fontSize = 5.rem
                color = col.unsafeCast<Color>()
            }
            fontAwesomeIcon(faChevronDown)
        }
    }
}

private fun ChildrenBuilder.h1Bold(str: String) = h1 {
    +str
    style = jso {
        fontWeight = "bold".unsafeCast<FontWeight>()
        display = Display.inline
        fontSize = "4.5rem".unsafeCast<FontSize>()
    }
}

private fun ChildrenBuilder.h1Normal(str: String) = h1 {
    +str
    style = jso {
        display = Display.inline
    }
}
