@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.cosv.frontend.components.views.vuln.toprating

import com.saveourtool.frontend.common.externals.fontawesome.faTrophy
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.td
import web.cssom.ClassName
import web.cssom.Color

/**
 * @param currentItem current item to render its top position
 * @param items [Array] of items sorted by wanted field
 */
internal fun <T : Any> ChildrenBuilder.renderRatingPosition(
    currentItem: T,
    items: Array<T>,
) {
    td {
        className = ClassName("align-middle")
        val index = items.indexOf(currentItem) + 1
        val (isTrophy, newColor) = when (index) {
            1 -> true to "#ebcc36"
            2 -> true to "#7d7d7d"
            3 -> true to "#a15703"
            else -> false to ""
        }
        if (isTrophy) {
            style = jso {
                color = newColor.unsafeCast<Color>()
            }
            fontAwesomeIcon(icon = faTrophy)
        }
        +" $index"
    }
}
