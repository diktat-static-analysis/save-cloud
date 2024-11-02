/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.frontend.common.components.basic

import com.saveourtool.frontend.common.externals.fontawesome.FontAwesomeIconModule
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon

import js.core.jso
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div
import web.cssom.ClassName
import web.cssom.Height
import web.cssom.Width

/**
 * Props for card component
 */
external interface CardProps : PropsWithChildren {
    /**
     * font-awesome class to be used as an icon
     */
    var faIcon: FontAwesomeIconModule
}

/**
 * A functional `Component` for a card.
 *
 * @param isBordered adds a border to the card
 * @param hasBg adds a white background
 * @param isPaddingBottomNull disables bottom padding (pb-0)
 * @param isNoPadding if true - removes all remaining padding (pt-0 pr-0 pl-0)
 * @param isFilling
 * @return a functional component representing a card
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun cardComponent(
    isBordered: Boolean = false,
    hasBg: Boolean = false,
    isPaddingBottomNull: Boolean = false,
    isNoPadding: Boolean = true,
    isFilling: Boolean = false,
) = FC<CardProps> { props ->
    val boarder = if (isBordered) "border-secondary" else ""
    val card = if (hasBg) "card" else ""
    val pb = if (isPaddingBottomNull) "pb-0" else ""
    val paddingInside = if (isNoPadding) "pt-0 pr-0 pl-0" else ""
    div {
        className = ClassName("$card card-body mt-0 $paddingInside $pb $boarder")
        if (isFilling) {
            style = jso {
                width = "100%".unsafeCast<Width>()
                height = "100%".unsafeCast<Height>()
            }
        }
        div {
            className = ClassName("col pr-0 pl-0")
            div {
                className = ClassName("mb-0 text-gray-800")
                props.children?.let { +it }
            }
        }
        if (props.faIcon != undefined) {
            div {
                className = ClassName("col-auto")
                fontAwesomeIcon(icon = props.faIcon, classes = "fas fa-2x text-gray-300")
            }
        }
    }
}
