/**
 * Utilities for kotlin-js interop
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.utils

import js.core.Object
import react.ChildrenBuilder

import kotlinx.browser.document
import kotlinx.browser.window

private const val SUPER_ADMIN_MESSAGE = "Keep in mind that you are super admin, so you are able to manage organization regardless of your organization permissions."

const val SAVE_DARK_GRADIENT = "-webkit-linear-gradient(270deg, rgb(0,20,73), rgb(13,71,161))"

const val VULN_DARK_GRADIENT = "-webkit-linear-gradient(270deg, #3e295e, #662cbd)"

const val SAVE_LIGHT_GRADIENT = "-webkit-linear-gradient(270deg, rgb(209, 229, 235),  rgb(217, 215, 235))"

const val VULN_DARK_REVERSED_GRADIENT = "-webkit-linear-gradient(90deg, #3e295e, #662cbd)"

/**
 * @property globalBackground
 * @property topBarBgColor
 * @property topBarTransparency
 * @property borderForContainer
 * @property marginBottomForTopBar
 */
enum class Style(
    val globalBackground: String,
    val topBarBgColor: String,
    val topBarTransparency: String,
    val borderForContainer: String,
    val marginBottomForTopBar: String,
) {
    INDEX(
        "-webkit-linear-gradient(0deg, rgb(0,20,73), #662cbd)",
        "",
        "transparent",
        "px-0",
        "",
    ),
    SAVE_DARK(
        SAVE_DARK_GRADIENT,
        "",
        "transparent",
        "px-0",
        "",
    ),
    SAVE_LIGHT(
        "bg-light",
        "bg-dark",
        "bg-dark",
        "",
        "mb-3",
    ),
    VULN_DARK(
        VULN_DARK_GRADIENT,
        "",
        "transparent",
        "px-0",
        "",
    ),
    VULN_LIGHT(
        "bg-light",
        "#563d7c",
        "#563d7c",
        "",
        "mb-3",
    ),
    ;
}

/**
 * Shortcut for
 * ```kotlin
 * child(MyComponent::class) {
 *     spread(props) { key, value ->
 *         attrs[key] = value
 *     }
 * }
 * ```
 *
 * Allows writing `<MyComponent ...props/>` as
 * ```kotlin
 * child(MyComponent::class) {
 *     spread(props)
 * }
 * ```
 *
 * @param jsObject a JS object properties of which will be used
 */
fun ChildrenBuilder.spread(jsObject: Any) {
    spread(jsObject) { key, value ->
        asDynamic()[key] = value
    }
}

/**
 * Attempt to mimic `...` operator from ES6.
 * For example, equivalent of `<MyComponent ...props/>` would be
 * ```kotlin
 * child(MyComponent::class) {
 *     spread(props) { key, value ->
 *         attrs[key] = value
 *     }
 * }
 * ```
 *
 * @param jsObject a JS object which properties will be used
 * @param handler a handler for [jsObject]'s property names and values
 */
@Suppress("TYPE_ALIAS")
fun spread(jsObject: Any, handler: (key: String, value: Any) -> Unit) {
    Object.keys(jsObject).map {
        it to jsObject.asDynamic()[it] as Any
    }
        .forEach { (key, value) ->
            handler(key, value)
        }
}

/**
 * External function to JS
 *
 * @param str
 * @return encoded [str]
 */
@Suppress("FUNCTION_NAME_INCORRECT_CASE")
external fun encodeURIComponent(str: String): String

/**
 * Function invoked when super admin might change something because of global role
 *
 * @return [Unit]
 */
fun showGlobalRoleConfirmation() = window.alert(SUPER_ADMIN_MESSAGE)

/**
 * JS code lines to enable tooltip.
 *
 * @return dynamic
 * @see [useTooltip]
 */
// language=js
fun enableTooltip() {
    js("""
    var jQuery = require("jquery")
    require("popper.js")
    require("bootstrap")
    jQuery('[data-toggle="tooltip"]').each(function() {
        jQuery(this).tooltip({
            delay: {
                "show": jQuery(this).attr("data-show-timeout") || 100,
                "hide": jQuery(this).attr("data-hide-timeout") || 100
            }
        })
    })
""")
}

/**
 * JS code lines to enable tooltip and popover.
 *
 * @return dynamic
 */
// language=JS
fun enableTooltipAndPopover() = js("""
    var jQuery = require("jquery")
    require("popper.js")
    require("bootstrap")
    jQuery('.popover').each(function() {
        jQuery(this).popover({
            placement: jQuery(this).attr("popover-placement"),
            title: jQuery(this).attr("popover-title"),
            content: jQuery(this).attr("popover-content"),
            html: true
        }).on('show.bs.popover', function() {
            jQuery(this).tooltip('hide')
        }).on('hide.bs.popover', function() {
            jQuery(this).tooltip('show')
        })
    })
""")

/**
 * @param style
 */
internal fun configureTopBar(style: Style) {
    val topBar = document.getElementById("navigation-top-bar")
    topBar?.setAttribute(
        "class",
        "navbar navbar-expand ${style.topBarBgColor} navbar-dark topbar ${style.marginBottomForTopBar} " +
                "static-top shadow mr-1 ml-1 rounded"
    )

    topBar?.setAttribute(
        "style",
        "background: ${style.topBarTransparency}"
    )

    val container = document.getElementById("common-save-container")
    container?.setAttribute(
        "class",
        "container-fluid ${style.borderForContainer}"
    )
}
