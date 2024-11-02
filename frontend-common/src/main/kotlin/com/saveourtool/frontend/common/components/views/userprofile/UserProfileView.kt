/**
 * View for UserProfile
 */

package com.saveourtool.frontend.common.components.views.userprofile

import com.saveourtool.common.entities.OrganizationDto
import com.saveourtool.common.frontend.TabMenuBar
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.info.UserStatus
import com.saveourtool.common.utils.*
import com.saveourtool.common.validation.FrontendCosvRoutes
import com.saveourtool.frontend.common.components.basic.renderAvatar
import com.saveourtool.frontend.common.components.basic.renderOrganizationAvatar
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.modal.displayModal
import com.saveourtool.frontend.common.components.modal.mediumTransparentModalStyle
import com.saveourtool.frontend.common.components.views.vuln.vulnerabilityTableComponent
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.*
import react.dom.aria.ariaDescribedBy
import react.dom.html.AnchorHTMLAttributes
import react.dom.html.HTMLAttributes
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.textarea
import react.router.dom.Link
import react.router.useNavigate
import web.cssom.*
import web.html.HTMLAnchorElement
import web.html.HTMLHeadingElement
import web.html.InputType

val userProfileView: FC<UserProfileViewProps> = FC { props ->
    useBackground(Style.SAVE_LIGHT)

    val (userName, _) = useStateFromProps(props.userName)
    val (user, setUser) = useState<UserInfo?>(null)
    val (organizations, setOrganizations) = useState<List<OrganizationDto>>(emptyList())
    val (selectedMenu, setSelectedMenu) = useState(UserProfileTab.VULNERABILITIES)
    val (countUsers, setCountUsers) = useState(0)
    val (isError, setIsError) = useState(false)

    useRequest {
        val userNew: UserInfo = get(
            "$apiUrl/users/$userName",
            jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()

        setUser(userNew)

        val organizationsNew: List<OrganizationDto> = get(
            "$apiUrl/organizations/get/list-by-user-name?userName=$userName",
            jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()

        setOrganizations(organizationsNew)

        val count: Int = get(
            url = "$apiUrl/users/new-users-count",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        ).unsafeMap {
            it.decodeFromJsonString()
        }

        setCountUsers(count)
    }

    div {
        className = ClassName("row justify-content-center")

        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-2 mb-4 mt-2")

            renderLeftUserMenu(user, props.currentUserInfo, organizations)
        }

        // ===================== RIGHT COLUMN =======================================================================
        div {
            className = ClassName("col-7 mb-4 mt-2")
            props.currentUserInfo?.globalRole?.let { role ->
                if (role.isSuperAdmin() && props.currentUserInfo?.name == user?.name && countUsers > 0) {
                    @Suppress("MISSING_KDOC_ON_FUNCTION")
                    fun UserProfileTab.toTabName() = if (this == UserProfileTab.USERS) "${this.name} ($countUsers)" else this.name

                    val tabList = UserProfileTab.entries.map { it.toTabName() }

                    tab(selectedMenu.toTabName(), tabList, "nav nav-tabs mt-3") { value ->
                        val newValue = if (value.contains(UserProfileTab.USERS.name)) UserProfileTab.USERS.name else value
                        setSelectedMenu { UserProfileTab.valueOf(newValue) }
                    }
                }
            }

            @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
            when (selectedMenu) {
                UserProfileTab.VULNERABILITIES -> vulnerabilityTableComponent {
                    this.currentUserInfo = props.currentUserInfo
                    this.userName = userName
                }
                UserProfileTab.USERS -> renderNewUsersTableForProfileView {}
            }
        }
    }
}

/**
 * [Props] of user profile view component
 */
@Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "TYPE_ALIAS",
)
external interface UserProfileViewProps : Props {
    /**
     * User name
     */
    var userName: String

    /**
     * Current logged-in user
     */
    var currentUserInfo: UserInfo?
}

/**
 * Enum that contains values for vulnerability
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class UserProfileTab {
    VULNERABILITIES,
    USERS,
    ;

    companion object : TabMenuBar<UserProfileTab> {
        override val nameOfTheHeadUrlSection = ""
        override val defaultTab: UserProfileTab = VULNERABILITIES
        override val regexForUrlClassification = "/${FrontendCosvRoutes.PROFILE}"
        override fun valueOf(elem: String): UserProfileTab = UserProfileTab.valueOf(elem)
        override fun values(): Array<UserProfileTab> = entries.toTypedArray()
    }
}

/**
 * @param user
 * @param organizations
 * @param currentUser
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "CyclomaticComplexMethod",
)
fun ChildrenBuilder.renderLeftUserMenu(
    user: UserInfo?,
    currentUser: UserInfo?,
    organizations: List<OrganizationDto>,
) {
    val navigate = useNavigate()
    val banUserWindowOpenness = useWindowOpenness()
    val (errorAvatars, setErrorAvatars) = useState(emptySet<String>())

    val banUser = useDeferredRequest {
        user?.name?.let {
            val response = get(
                url = "$apiUrl/users/ban",
                params = jso<dynamic> {
                    userName = it
                },
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
                responseHandler = ::noopResponseHandler,
            )
            if (response.ok) {
                navigate(to = "/")
            }
        }
    }

    val approveUser = useDeferredRequest {
        user?.name?.let {
            val response = get(
                url = "$apiUrl/users/approve",
                params = jso<dynamic> {
                    userName = it
                },
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
                responseHandler = ::noopResponseHandler,
            )
            if (response.ok) {
                navigate(to = "/")
            }
        }
    }

    // FixMe: Comment and cause is not used anywhere. Need to send email notification
    displayModal(
        banUserWindowOpenness.isOpen(),
        "User profile ban",
        bodyBuilder = {
            div {
                div {
                    className = ClassName("col-12 form-check-inline mb-2")
                    input {
                        className = ClassName("form-check-input")
                        defaultChecked = true
                        name = "cause"
                        type = InputType.radio
                        value = "public"
                    }
                    label {
                        className = ClassName("form-check-label")
                        htmlFor = "cause"
                        +"Violation of the rules for using the service"
                    }
                }
                div {
                    className = ClassName("col-12 form-check-inline mb-3")
                    input {
                        className = ClassName("form-check-input")
                        defaultChecked = false
                        name = "cause"
                        type = InputType.radio
                        value = "public"
                    }
                    label {
                        className = ClassName("form-check-label")
                        htmlFor = "cause"
                        +"Other"
                    }
                }
                textarea {
                    className = ClassName("border-secondary form-control p-3 border-1")
                    ariaDescribedBy = "${InputTypes.COMMENT.name}Span"
                    rows = 5
                    id = InputTypes.COMMENT.name
                    required = true
                    placeholder = "Write a comment"
                }
            }
        },
        modalStyle = mediumTransparentModalStyle,
        onCloseButtonPressed = banUserWindowOpenness.closeWindowAction(),
    ) {
        buttonBuilder("Ok", "danger") {
            banUser()
            banUserWindowOpenness.closeWindow()
        }
        buttonBuilder("Close", "secondary") {
            banUserWindowOpenness.closeWindow()
        }
    }

    div {
        className = ClassName("row justify-content-center")
        renderAvatar(user, "mb-4", isLinkActive = false) {
            height = 15.rem
            width = 15.rem
        }
    }

    h3 {
        className = ClassName("mb-0 text-gray-900 text-center")
        shortenLoginWithTooltipIfNecessary(user?.name, this)
    }

    h5 {
        className = ClassName("mb-0 text-gray-600 text-center")
        shortenRealNameWithTooltipIfNecessary(user?.realName, this)
    }

    div {
        className = ClassName("col text-center mt-2")
        Link {
            to = "/${FrontendCosvRoutes.VULN_TOP_RATING}"
            className = ClassName("row text-xs font-weight-bold text-info justify-content-center text-uppercase mb-1")
            +"Rating"
        }
        div {
            className = ClassName("row h5 font-weight-bold justify-content-center text-gray-800 my-1")
            +user?.rating.toString()
        }

        if (currentUser?.name == user?.name) {
            div {
                className = ClassName("row h5 font-weight-bold justify-content-center text-gray-800 my-3")

                buttonBuilder(label = "Customize profile", isOutline = true, style = "primary btn-sm") {
                    navigate(to = "/${FrontendCosvRoutes.SETTINGS_PROFILE}")
                }
            }
        }

        if (currentUser?.isSuperAdmin() == true && currentUser.name != user?.name && user?.status != UserStatus.BANNED) {
            div {
                className = ClassName("row h5 font-weight-bold justify-content-center text-gray-800 my-3")

                buttonBuilder(label = "Ban user", isOutline = true, style = "danger btn-sm") {
                    banUserWindowOpenness.openWindow()
                }
            }
            if (user?.status == UserStatus.NOT_APPROVED) {
                div {
                    className = ClassName("row h5 font-weight-bold justify-content-center text-gray-800 my-3")

                    buttonBuilder(label = "Approve user", isOutline = true, style = "success btn-sm") {
                        approveUser()
                    }
                }
            }
        }
    }

    user?.freeText?.let { freeText(it) }

    user?.company?.let { company ->
        div {
            className = ClassName("my-2")
            fontAwesomeIcon(icon = faCity) {
                it.className = "fas fa-sm fa-fw mr-2"
            }
            +company
        }
    }

    user?.location?.let { location ->
        div {
            className = ClassName("mb-2")
            fontAwesomeIcon(icon = faGlobe) {
                it.className = "fas fa-sm fa-fw mr-2"
            }
            +location
        }
    }

    user?.gitHub?.let { extraLinks(faGithub, it, listOf(UsefulUrls.GITHUB, UsefulUrls.GITEE)) }

    user?.twitter?.let { extraLinks(faTwitter, it, listOf(UsefulUrls.TWITTER, UsefulUrls.XCOM)) }

    user?.linkedin?.let { extraLinks(faLinkedIn, it, listOf(UsefulUrls.LINKEDIN)) }

    user?.website?.let { extraLinks(faLink, it, listOf(UsefulUrls.WEBSITE)) }

    if (organizations.isNotEmpty()) {
        div {
            className = ClassName("separator")
            style = jso {
                borderBottom = "0.07rem #000000".unsafeCast<BorderBottom>()
            }
            +"Organizations"
        }

        div {
            className = ClassName("latest-photos mt-3")
            div {
                className = ClassName("row")
                organizations.forEach { organization ->
                    renderOrganizationAvatar(
                        organization,
                        errorAvatars = errorAvatars,
                        setErrorAvatars = { name -> setErrorAvatars(errorAvatars.plus(name)) }
                    ) {
                        width = 4.rem
                        height = 4.rem
                    }
                }
            }
        }
    }
}

/**
 * @param login
 * @param htmlAttribute
 */
fun ChildrenBuilder.shortenLoginWithTooltipIfNecessary(login: String?, htmlAttribute: HTMLAttributes<HTMLHeadingElement>) {
    login?.let {
        if (it.length > LOGIN_MAX_LENGTH) {
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "top"
            htmlAttribute.title = it

            +(it.shortenLogin())
        } else {
            +it
        }
    } ?: +"N/A"
}

/**
 * @param realName
 * @param htmlAttribute
 */
fun ChildrenBuilder.shortenRealNameWithTooltipIfNecessary(realName: String?, htmlAttribute: HTMLAttributes<HTMLHeadingElement>) {
    realName?.let { name ->
        if (name.split(" ").any { it.length > REAL_NAME_PART_MAX_LENGTH }) {
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "bottom"
            htmlAttribute.title = name

            +(name.shortenRealName())
        } else {
            +name
        }
    } ?: +"N/A"
}

/**
 * @param url
 * @param tooltipUrl
 * @param htmlAttribute
 */
fun ChildrenBuilder.shortenUrlWithTooltipIfNecessary(url: String, tooltipUrl: String, htmlAttribute: AnchorHTMLAttributes<HTMLAnchorElement>) {
    if (url.length > URL_MAX_LENGTH) {
        asDynamic()["data-toggle"] = "tooltip"
        asDynamic()["data-placement"] = "top"
        htmlAttribute.title = tooltipUrl

        +(url.shortenUrl())
    } else {
        +url
    }
}

/**
 * @param icon
 * @param info
 * @param patterns
 */
fun ChildrenBuilder.extraLinks(icon: FontAwesomeIconModule, info: String, patterns: List<UsefulUrls>) {
    val foundPattern = patterns.find { info.matches(it.regex) }
    foundPattern?.let {
        val trimmedLink = if (foundPattern != UsefulUrls.WEBSITE) {
            info.substringAfterLast(".com/")
        } else {
            info.substringAfter("://")
        }
        if (trimmedLink.isNotBlank()) {
            div {
                className = ClassName("mb-2")
                fontAwesomeIcon(icon = icon) {
                    it.className = "fas fa-sm fa-fw mr-2 text-gray-900"
                }
                a {
                    href = info
                    shortenUrlWithTooltipIfNecessary(trimmedLink, info, this)
                }
            }
        }
    }
}

/**
 * @param text
 */
fun ChildrenBuilder.freeText(text: String) {
    if (text.isNotEmpty()) {
        div {
            className = ClassName("separator")
            style = jso {
                borderBottom = "0.07rem #000000".unsafeCast<BorderBottom>()
            }
            +"About"
        }

        div {
            className = ClassName("row justify-content-center")
            p {
                className = ClassName("mb-0")
                style = jso {
                    textAlign = TextAlign.justify
                }
                +text
            }
        }
        @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
        hr {}
    }
}
