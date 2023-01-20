@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.topbar

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.components.modal.logoutModal
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import react.*
import react.dom.aria.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.ul
import react.router.useNavigate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

val topBarUserField = topBarUserField()

/**
 * [Props] of the top bar user field component
 */
external interface TopBarUserFieldProps : Props {
    /**
     * Currently logged in user or `null`.
     */
    var userInfo: UserInfo?
}

/**
 * Displays the user's field.
 */
@Suppress(
    "MAGIC_NUMBER",
    "LongMethod",
    "TOO_LONG_FUNCTION",
    "LOCAL_VARIABLE_EARLY_DECLARATION"
)
private fun topBarUserField() = FC<TopBarUserFieldProps> { props ->
    val scope = CoroutineScope(Dispatchers.Default)
    val navigate = useNavigate()
    var isLogoutModalOpen by useState(false)
    var isAriaExpanded by useState(false)
    val (avatar, setAvatar) = useState(props.userInfo?.avatar?.let { "/api/$v1/avatar$it" })
    useEffect {
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }
    ul {
        className = ClassName("navbar-nav ml-auto")
        div {
            className = ClassName("topbar-divider d-none d-sm-block")
        }
        // Nav Item - User Information
        li {
            className = ClassName("nav-item dropdown no-arrow")
            onClickCapture = {
                isAriaExpanded = !isAriaExpanded
            }
            a {
                href = "#"
                className = ClassName("nav-link dropdown-toggle")
                id = "userDropdown"
                role = "button".unsafeCast<AriaRole>()
                ariaExpanded = isAriaExpanded
                ariaHasPopup = true.unsafeCast<AriaHasPopup>()
                asDynamic()["data-toggle"] = "dropdown"

                div {
                    className = ClassName("d-flex flex-row")
                    div {
                        className = ClassName("d-flex flex-column")
                        span {
                            className = ClassName("mr-2 d-none d-lg-inline text-gray-600")
                            +(props.userInfo?.name.orEmpty())
                        }
                        val globalRole = props.userInfo?.globalRole ?: Role.VIEWER
                        if (globalRole.isHigherOrEqualThan(Role.ADMIN)) {
                            small {
                                className = ClassName("text-gray-400 text-justify")
                                +globalRole.formattedName
                            }
                        }
                    }
                    avatar?.let { avatar ->
                        img {
                            className =
                                    ClassName("ml-2 align-self-center avatar avatar-user width-full border color-bg-default rounded-circle fas mr-2")
                            src = avatar
                            height = 45.0
                            width = 45.0
                            onError = {
                                setAvatar { "img/undraw_image_not_found.png" }
                            }
                        }
                    } ?: fontAwesomeIcon(icon = faUser) {
                        it.className = "m-2 align-self-center fas fa-lg fa-fw mr-2 text-gray-400"
                    }
                }
            }
            // Dropdown - User Information
            div {
                className = ClassName("dropdown-menu dropdown-menu-right shadow animated--grow-in${if (isAriaExpanded) " show" else ""}")
                ariaLabelledBy = "userDropdown"
                props.userInfo?.name?.let { name ->
                    dropdownEntry(faCog, "Settings") { attrs ->
                        attrs.onClick = {
                            navigate(to = "/$name/${FrontendRoutes.SETTINGS_EMAIL.path}")
                        }
                    }
                    dropdownEntry(
                        faCity,
                        "My organizations"
                    ) { attrs ->
                        attrs.onClick = {
                            navigate(to = "/$name/${FrontendRoutes.SETTINGS_ORGANIZATIONS.path}")
                        }
                    }
                }
                dropdownEntry(faSignOutAlt, "Log out") { attrs ->
                    attrs.onClick = {
                        isLogoutModalOpen = true
                    }
                }
            }
        }
    }

    logoutModal {
        isLogoutModalOpen = false
    }() {
        isOpen = isLogoutModalOpen
    }
}
