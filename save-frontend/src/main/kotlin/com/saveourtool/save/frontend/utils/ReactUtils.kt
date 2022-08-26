/**
 * Contains utils method for React
 */

package com.saveourtool.save.frontend.utils

import react.useEffect
import react.useState

/**
 * Runs the provided [action] only once of first render
 *
 * @param action
 */
fun useOnce(action: () -> Unit) {
    val (isFirstRender, setFirstRender) = useState(true)
    if (isFirstRender) {
        action()
        setFirstRender(false)
    }
}

/**
 * Can only be called from functional components
 *
 * @param updateNotificationMessage callback to show notification message
 * @return current value and callback for showGlobalRoleWarning
 */
fun useGlobalRoleWarningCallback(updateNotificationMessage: (String, String) -> Unit): Pair<Boolean, () -> Unit> {
    val (wasConfirmationModalShown, setWasConfirmationModalShown) = useState(false)
    val showGlobalRoleWarning = {
        updateNotificationMessage(
            "Super admin message",
            "Keep in mind that you are super admin, so you are able to manage organization regardless of your organization permissions.",
        )
        setWasConfirmationModalShown(true)
    }
    return wasConfirmationModalShown to showGlobalRoleWarning
}

/**
 * Custom hook to enable tooltips.
 */
fun useTooltip() {
    useEffect {
        enableTooltip()
        return@useEffect
    }
}

/**
 * Custom hook to enable tooltips and popovers.
 */
fun useTooltipAndPopover() {
    useEffect {
        enableTooltipAndPopover()
        return@useEffect
    }
}

/**
 * JS code lines to enable tooltip.
 *
 * @return dynamic
 */
// language=js
fun enableTooltip() = js("""
    var jQuery = require("jquery")
    require("popper.js")
    require("bootstrap")
    jQuery('[data-toggle="tooltip"]').tooltip()
""")

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
