/**
 * A view for registration
 */

@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.frontend.components.basic.avatarForm
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.inputform.inputTextFormRequired
import com.saveourtool.save.frontend.components.modal.MAX_Z_INDEX
import com.saveourtool.save.frontend.components.views.usersettings.AVATAR_TITLE
import com.saveourtool.save.frontend.http.postImageUpload
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.info.UserStatus
import com.saveourtool.save.utils.AVATARS_PACKS_DIR
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.CONTENT_LENGTH_CUSTOM
import com.saveourtool.save.utils.FILE_PART_NAME
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes
import com.saveourtool.save.validation.isValidLengthName
import com.saveourtool.save.validation.isValidName

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
val registrationView: FC<RegistrationProps> = FC { props ->
    useBackground(Style.INDEX)
    particles()
    val avatarWindowOpen = useWindowOpenness()
    val (selectedAvatar, setSelectedAvatar) = useState<String?>(null)
    val (avatar, setAvatar) = useState<File?>(null)

    val (isTermsOfUseOk, setIsTermsOfUseOk) = useState(false)
    val (conflictErrorMessage, setConflictErrorMessage) = useState<String?>(null)
    val (userInfo, setUserInfo) = useStateFromProps(props.userInfo ?: UserInfo(name = "")) { userInfo ->
        // weed to process user names, as some authorization providers pass emails instead of names
        val atIndex = userInfo.name.indexOf('@')
        val processedName = if (atIndex >= 0) userInfo.name.substring(0, atIndex) else userInfo.name
        userInfo.copy(name = processedName)
    }

    val navigate = useNavigate()

    val saveUser = useDeferredRequest {
        val newUserInfo = userInfo.copy(
            oldName = props.userInfo?.name!!,
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
            window.location.href = window.location.origin
            window.location.reload()
        }
    }

    val (newAvatar, setNewAvatar) = useState<File?>(null)
    useRequest(dependencies = arrayOf(newAvatar)) {
        newAvatar?.let { avatar ->
            postImageUpload(
                avatar,
                props.userInfo?.name!!,
                AvatarType.USER,
                loadingHandler = ::loadingHandler,
            )
        }
    }

    if (props.userInfo?.status != UserStatus.CREATED) {
        navigate("/", jso { replace = true })
    }

    val saveAvatar = useDeferredRequest {
        avatar?.let {
            val response = request(
                url = "$apiUrl/avatar/upload".withParams(jso<dynamic> {
                    owner = props.userInfo?.name
                    this.type = AvatarType.USER
                }),
                method = "POST",
                headers = Headers().apply { append(CONTENT_LENGTH_CUSTOM, avatar.size.toString()) },
                body = FormData().apply { set(FILE_PART_NAME, avatar) },
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
            if (response.ok) {
                window.location.reload()
            }
        }
    }

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
                                        renderPreparedAvatars(1..3, setSelectedAvatar)
                                    }
                                }

                                div {
                                    className = ClassName("col-6")
                                    renderAvatar(avatarWindowOpen, props.userInfo?.avatar)
                                }

                                div {
                                    className = ClassName("col-3")
                                    div {
                                        className = ClassName("row d-flex justify-content-center")
                                        renderPreparedAvatars(4..6, setSelectedAvatar)
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
                                    isDisabled = !isTermsOfUseOk,
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
 * `Props` retrieved from router
 */
external interface RegistrationProps : PropsWithChildren {
    /**
     * Currently logged-in user or null
     */
    var userInfo: UserInfo?
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
                src = avatar?.let { "/api/$v1/avatar$it" } ?: AVATAR_PROFILE_PLACEHOLDER
                style = jso {
                    height = 16.rem
                    width = 16.rem
                }
            }
        }
    }
}

private fun ChildrenBuilder.renderPreparedAvatars(avatarsRange: IntRange, setSelectedAvatar: StateSetter<String?>) {
    for (i in avatarsRange) {
        val avatar = "/img/$AVATARS_PACKS_DIR/avatar$i.png"
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
                }
            }
        }
    }
}
