/**
 * A component for logout modal window
 */

package com.saveourtool.frontend.common.components.modal

import com.saveourtool.frontend.common.externals.modal.ModalProps
import com.saveourtool.frontend.common.utils.buttonBuilder
import com.saveourtool.frontend.common.utils.loadingHandler
import com.saveourtool.frontend.common.utils.post
import com.saveourtool.frontend.common.utils.useDeferredRequest

import org.w3c.fetch.Headers
import react.FC
import react.router.useNavigate

import kotlinx.browser.window

/**
 * @param closeCallback a callback to call to close the modal
 * @return a Component
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun logoutModal(
    closeCallback: () -> Unit
) = FC<ModalProps> { props ->
    val navigate = useNavigate()

    val doLogoutRequest = useDeferredRequest {
        val replyToLogout = post(
            "${window.location.origin}/logout",
            Headers(),
            "ping",
            loadingHandler = ::loadingHandler,
        )
        if (replyToLogout.ok) {
            // logout went good, need either to reload page or to setUserInfo(null) and use redirection like `window.location.href = window.location.origin`
            navigate("/")
            window.location.reload()
        } else {
            // close this modal to allow user to see modal with error description
            closeCallback()
        }
    }

    displayModal(
        props.isOpen,
        "Ready to Leave?",
        "Select \"Logout\" below if you are ready to end your current session.",
        mediumTransparentModalStyle,
        { closeCallback() }
    ) {
        buttonBuilder("Logout", "primary") {
            doLogoutRequest()
        }
        buttonBuilder("Cancel", "secondary") {
            closeCallback()
        }
    }
}
