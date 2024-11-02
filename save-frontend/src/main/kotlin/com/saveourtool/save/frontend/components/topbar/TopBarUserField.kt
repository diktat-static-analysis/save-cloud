/**
 * FC user's topbar
 */

package com.saveourtool.save.frontend.components.topbar

import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.basic.avatarRenderer
import com.saveourtool.frontend.common.components.modal.logoutModal
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.save.frontend.externals.i18next.useTranslation

import js.core.jso
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
import web.cssom.ClassName
import web.cssom.rem

@Suppress("MAGIC_NUMBER")
val logoSize: CSSProperties = jso {
    height = 2.5.rem
    width = 2.5.rem
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
val topBarUserField: FC<UserInfoAwareProps> = FC { props ->
    val (t) = useTranslation("topbar")
    val navigate = useNavigate()
    var isLogoutModalOpen by useState(false)
    var isAriaExpanded by useState(false)

    ul {
        className = ClassName("navbar-nav ml-auto")
        div {
            className = ClassName("topbar-divider d-none d-sm-block")
        }
        // Nav Item - User Information
        li {
            className = ClassName("nav-item dropdown no-arrow")
            onClickCapture = { isAriaExpanded = !isAriaExpanded }
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
                            className = ClassName("mr-2 text-white-400")
                            +(props.userInfo?.name.orEmpty())
                        }
                        small {
                            className = ClassName("text-gray-400 text-justify")
                            props.userInfo?.let {
                                if (props.userInfo.isSuperAdmin()) {
                                    +"Super user".t()
                                } else {
                                    +"User settings".t()
                                }
                            }
                        }
                    }
                    props.userInfo?.let { userInfo ->
                        img {
                            className =
                                    ClassName("ml-2 align-self-center avatar avatar-user width-full border color-bg-default rounded-circle fas mr-2")
                            src = props.userInfo?.avatar?.avatarRenderer() ?: AVATAR_PROFILE_PLACEHOLDER
                            style = logoSize
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
                    dropdownEntry(faUser, "Profile".t()) { attrs ->
                        attrs.onClick = {
                            navigate(to = "/${FrontendRoutes.PROFILE}/$name")
                        }
                    }
                    dropdownEntry(faCog, "Settings".t()) { attrs ->
                        attrs.onClick = {
                            navigate(to = "/${FrontendRoutes.SETTINGS_PROFILE}")
                        }
                    }
                    dropdownEntry(
                        faCity,
                        "Manage organizations".t()
                    ) { attrs ->
                        attrs.onClick = {
                            navigate(to = "/${FrontendRoutes.SETTINGS_ORGANIZATIONS}")
                        }
                    }
                    dropdownEntry(faSignOutAlt, "Log out".t()) { attrs ->
                        attrs.onClick = {
                            isLogoutModalOpen = true
                        }
                    }
                } ?: run {
                    dropdownEntry(faSignInAlt, "Log in".t()) { attrs ->
                        attrs.onClick = {
                            navigate(to = "/")
                        }
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
