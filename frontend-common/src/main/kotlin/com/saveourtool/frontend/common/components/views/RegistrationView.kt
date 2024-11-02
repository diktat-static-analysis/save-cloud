/**
 * A view for registration
 */

@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.views

import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.info.UserStatus
import com.saveourtool.common.utils.AVATARS_PACKS_DIR
import com.saveourtool.common.utils.AvatarType
import com.saveourtool.common.utils.CONTENT_LENGTH_CUSTOM
import com.saveourtool.common.utils.FILE_PART_NAME
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.common.validation.isValidLengthName
import com.saveourtool.common.validation.isValidName
import com.saveourtool.common.validation.isValidUrl
import com.saveourtool.frontend.common.components.basic.avatarForm
import com.saveourtool.frontend.common.components.basic.avatarRenderer
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.inputform.inputTextFormOptional
import com.saveourtool.frontend.common.components.inputform.inputTextFormRequired
import com.saveourtool.frontend.common.components.modal.MAX_Z_INDEX
import com.saveourtool.frontend.common.components.views.usersettings.AVATAR_TITLE
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.UserInfoAwareMutablePropsWithChildren

import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.textarea
import react.router.dom.Link
import react.router.useNavigate
import web.cssom.*
import web.file.File
import web.html.InputType
import web.http.FormData
import web.window.WindowTarget

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A Component for registration view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
val registrationView: FC<UserInfoAwareMutablePropsWithChildren> = FC { props ->
    useBackground(Style.INDEX)
    particles()
    val useNavigate = useNavigate()

    if (props.userInfo?.status == UserStatus.ACTIVE) {
        useNavigate(to = "/")
    }

    useRedirectToIndexIf(props.userInfo?.status) {
        // life hack ot be sure that props are loaded
        props.key != null && props.userInfo?.status != UserStatus.CREATED
    }

    val avatarWindowOpen = useWindowOpenness()
    val (selectedAvatar, setSelectedAvatar) = useState(props.userInfo?.avatar)
    val (avatar, setAvatar) = useState<File?>(null)

    val (isTermsOfUseOk, setIsTermsOfUseOk) = useState(false)
    val (conflictErrorMessage, setConflictErrorMessage) = useState<String?>(null)
    val (userInfo, setUserInfo) = useStateFromProps(props.userInfo ?: UserInfo(name = "")) { userInfo ->
        // weed to process usernames, as some authorization providers pass emails instead of names
        val atIndex = userInfo.name.indexOf('@')
        val processedName = if (atIndex >= 0) userInfo.name.substring(0, atIndex) else userInfo.name
        userInfo.copy(name = processedName)
    }

    val saveUser = useDeferredRequest {
        val newUserInfo = userInfo.copy(
            status = UserStatus.ACTIVE,
        )
        val response = post(
            "$apiUrl/users/save",
            jsonHeaders,
            Json.encodeToString(newUserInfo),
            loadingHandler = ::loadingHandler,
            responseHandler = ::responseHandlerWithValidation,
        )
        if (response.ok) {
            props.userInfoSetter(userInfo)
            useNavigate(to = "/${FrontendRoutes.THANKS_FOR_REGISTRATION}")
            window.location.reload()
        } else if (response.isConflict()) {
            setConflictErrorMessage(response.unpackMessage())
        }
    }

    val logOut = useDeferredRequest {
        val replyToLogout = post(
            "${window.location.origin}/logout",
            Headers(),
            "ping",
            loadingHandler = ::loadingHandler,
        )
        if (replyToLogout.ok) {
            useNavigate(to = "/")
            window.location.reload()
        }
    }

    val saveAvatar = useDeferredRequest {
        avatar?.let {
            val response = post(
                url = "$apiUrl/avatar/upload",
                params = jso<dynamic> {
                    owner = props.userInfo?.name
                    this.type = AvatarType.USER
                },
                Headers().apply { append(CONTENT_LENGTH_CUSTOM, avatar.size.toString()) },
                FormData().apply { set(FILE_PART_NAME, avatar) },
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )

            if (response.ok) {
                window.location.reload()
            }
        }
    }

    val setAvatarFromResources = useDeferredRequest {
        get(
            url = "$apiUrl/avatar/avatar-update",
            params = jso<dynamic> {
                this.type = AvatarType.USER
                this.resource = selectedAvatar
            },
            jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
    }

    val isWebsiteValid = userInfo.website?.isValidUrl() ?: true

    avatarForm {
        isOpen = avatarWindowOpen.isOpen()
        title = AVATAR_TITLE
        onCloseWindow = {
            saveAvatar()
            avatarWindowOpen.closeWindow()
        }
        imageUpload = { file ->
            setAvatar(file)
        }
    }

    main {
        className = ClassName("main-content mt-0 ps")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            span {
                className = ClassName("mask bg-gradient-dark opacity-6")
            }
            div {
                className = ClassName("row justify-content-center")
                div {
                    className = ClassName("col-sm-4")
                    div {
                        className = ClassName("container card o-hidden border-0 shadow-lg my-2 card-body p-0")
                        style = jso {
                            zIndex = (MAX_Z_INDEX - 1).unsafeCast<ZIndex>()
                        }
                        div {
                            className = ClassName("p-5 text-center")

                            h1 {
                                className = ClassName("h4 text-gray-900 mb-4")
                                +"Set your user name and avatar"
                            }

                            div {
                                className = ClassName("row")

                                div {
                                    className = ClassName("col-3")
                                    div {
                                        className = ClassName("row d-flex justify-content-center")
                                        renderPreparedAvatars(
                                            1..3,
                                            setSelectedAvatar,
                                            setAvatarFromResources,
                                        )
                                    }
                                }

                                div {
                                    className = ClassName("col-6")
                                    renderAvatar(avatarWindowOpen, selectedAvatar)
                                }

                                div {
                                    className = ClassName("col-3")
                                    div {
                                        className = ClassName("row d-flex justify-content-center")
                                        renderPreparedAvatars(
                                            4..6,
                                            setSelectedAvatar,
                                            setAvatarFromResources,
                                        )
                                    }
                                }
                            }

                            form {
                                div {
                                    inputTextFormRequired {
                                        form = InputTypes.LOGIN
                                        textValue = userInfo.name
                                        validInput = userInfo.name.isNotEmpty() && userInfo.name.isValidName() && userInfo.name.isValidLengthName()
                                        classes = ""
                                        name = "User name"
                                        conflictMessage = conflictErrorMessage
                                        onChangeFun = { event ->
                                            setUserInfo { previousUserInfo -> previousUserInfo.copy(name = event.target.value) }
                                            setConflictErrorMessage(null)
                                        }
                                    }
                                }

                                div {
                                    className = ClassName("pt-3 font-weight-bold")
                                    +"Please enter some information about yourself so that it would be easier for us to approve."
                                }

                                div {
                                    className = ClassName("pt-3")
                                    inputTextFormOptional {
                                        form = InputTypes.GITHUB
                                        textValue = userInfo.gitHub
                                        classes = ""
                                        validInput = null
                                        onChangeFun = { event ->
                                            setUserInfo { previousUserInfo ->
                                                previousUserInfo.copy(gitHub = event.target.value.takeIf { it.isNotBlank() })
                                            }
                                        }
                                    }
                                }

                                div {
                                    className = ClassName("pt-3")
                                    inputTextFormOptional {
                                        form = InputTypes.WEBSITE
                                        textValue = userInfo.website
                                        classes = ""
                                        validInput = userInfo.website?.isValidUrl()
                                        onChangeFun = { event ->
                                            setUserInfo { previousUserInfo ->
                                                previousUserInfo.copy(website = event.target.value.takeIf { it.isNotBlank() })
                                            }
                                        }
                                    }
                                }

                                div {
                                    className = ClassName("pt-3")
                                    textarea {
                                        className = ClassName("form-control")
                                        value = userInfo.freeText
                                        placeholder = "Additional info"
                                        onChange = { event -> setUserInfo { previousUserInfo -> previousUserInfo.copy(freeText = event.target.value) } }
                                    }
                                }

                                div {
                                    className = ClassName("mt-2 form-check row")
                                    input {
                                        className = ClassName("form-check-input")
                                        type = "checkbox".unsafeCast<InputType>()
                                        value = ""
                                        id = "terms-of-use"
                                        onChange = { setIsTermsOfUseOk(it.target.checked) }
                                    }
                                    label {
                                        className = ClassName("form-check-label")
                                        +" I agree with "
                                        Link {
                                            to = "/${FrontendRoutes.TERMS_OF_USE}"
                                            target = "_blank".unsafeCast<WindowTarget>()
                                            +"terms of use"
                                        }
                                    }
                                }

                                buttonBuilder(
                                    "Sign up",
                                    "info",
                                    classes = "mt-4 mr-4",
                                    isDisabled = !isTermsOfUseOk || !isWebsiteValid,
                                ) { saveUser() }

                                buttonBuilder(
                                    "Log out",
                                    "danger",
                                    classes = "mt-4",
                                ) { logOut() }

                                conflictErrorMessage?.let {
                                    div {
                                        className = ClassName("invalid-feedback d-block")
                                        +it
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * @param avatarWindowOpen
 * @param avatar
 */
fun ChildrenBuilder.renderAvatar(
    avatarWindowOpen: WindowOpenness,
    avatar: String?,
) {
    div {
        className = ClassName("animated-provider")
        Link {
            className = ClassName("btn px-0 pt-0")
            title = AVATAR_TITLE
            onClick = {
                avatarWindowOpen.openWindow()
            }
            img {
                className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                src = avatar?.avatarRenderer() ?: AVATAR_PROFILE_PLACEHOLDER
                style = jso {
                    height = 16.rem
                    width = 16.rem
                }
            }
        }
    }
}

private fun ChildrenBuilder.renderPreparedAvatars(
    avatarsRange: IntRange,
    setSelectedAvatar: StateSetter<String?>,
    setAvatarFromResources: () -> Unit = { },
) {
    for (i in avatarsRange) {
        val avatar = "$AVATARS_PACKS_DIR/avatar$i.png"
        div {
            className = ClassName("animated-provider")
            img {
                className =
                        ClassName("avatar avatar-user width-full border color-bg-default rounded-circle mt-1")
                src = avatar
                style = jso {
                    height = 5.1.rem
                    width = 5.1.rem
                    cursor = Cursor.pointer
                }
                onClick = {
                    setSelectedAvatar(avatar)
                    setAvatarFromResources()
                }
            }
        }
    }
}
