@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.frontend.common.components.basic

import com.saveourtool.common.entities.CommentDto
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.utils.toUnixCalendarFormat
import com.saveourtool.common.validation.FrontendCosvRoutes
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.externals.fontawesome.faPaperPlane
import com.saveourtool.frontend.common.externals.fontawesome.faTimes
import com.saveourtool.frontend.common.externals.i18next.TranslationFunction
import com.saveourtool.frontend.common.externals.i18next.useTranslation
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.PropsWithChildren
import react.dom.aria.ariaDescribedBy
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.textarea
import react.router.dom.Link
import react.useState
import web.cssom.*

import kotlinx.browser.window
import kotlinx.datetime.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @return a function component
 */
@Suppress("MAGIC_NUMBER")
val newCommentWindow: FC<NewCommentWindowProps> = FC { props ->
    val (comment, setComment) = useState(CommentDto.empty)
    val (t) = useTranslation("comments")

    val enrollRequest = useDeferredRequest {
        val commentNew = comment.copy(section = window.location.pathname)
        val response = post(
            url = "$apiUrl/comments/save",
            headers = jsonHeaders,
            body = Json.encodeToString(commentNew),
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
        if (response.ok) {
            props.addComment()
            setComment(CommentDto.empty)
        }
    }

    div {
        className = ClassName("shadow col mx-auto border border-secondary card card-body p-0")
        div {
            className = ClassName("row no-gutters mx-auto input-group px-0 shadow-none")
            renderLeftColumn(
                props.currentUserInfo.avatar,
                props.currentUserInfo.name,
                props.currentUserInfo.rating,
                t,
                "#f7f5fb",
            )
            div {
                className = ClassName("col")
                textarea {
                    className = ClassName("form-control p-3 border-0")
                    style = jso {
                        width = "100%".unsafeCast<Width>()
                        height = "100%".unsafeCast<Height>()
                    }
                    onChange = { event -> setComment { it.copy(message = event.target.value) } }
                    value = comment.message
                    ariaDescribedBy = "${InputTypes.COMMENT.name}Span"
                    rows = 5
                    id = InputTypes.COMMENT.name
                    required = true
                    placeholder = "Write a comment".t()
                }
            }
        }
        div {
            className = ClassName("d-flex justify-content-end p-2")
            style = jso { background = "#f7f5fb".unsafeCast<Background>() }
            buttonBuilder(
                faPaperPlane,
                isDisabled = comment.message.isBlank(),
                classes = "rounded-circle btn-sm",
                isOutline = true,
            ) {
                enrollRequest()
            }
        }
    }
}

/**
 * [FC] for comment displaying
 */
@Suppress("MAGIC_NUMBER")
val commentWindow: FC<CommentWindowProps> = FC { props ->
    val (t) = useTranslation("comments")
    div {
        className = ClassName("shadow input-group row no-gutters mx-auto border-secondary")
        renderLeftColumn(
            props.comment.userAvatar,
            props.comment.userName,
            props.comment.userRating,
            t,
        )
        div {
            className = ClassName("shadow-none card col-10 text-left border-0")
            val comment = props.comment
            div {
                className = ClassName("flex-wrap d-flex justify-content-between")
                style = jso { background = "#f1f1f1".unsafeCast<Background>() }
                span {
                    className = ClassName("ml-1")
                    +(comment.createDate?.toUnixCalendarFormat(TimeZone.currentSystemDefault()) ?: "Unknown".t())
                }
                div {
                    if (props.currentUserInfo?.canDelete(props.comment) == true) {
                        buttonBuilder(faTimes, style = "", classes = "btn-sm") {
                            if (window.confirm("Are you sure you want to delete a comment?".t())) {
                                props.setCommentForDeletion(props.comment)
                            }
                        }
                    }
                }
            }
            div {
                className = ClassName("shadow-none card card-body border-0")
                markdown(comment.message.split("\n").joinToString("\n\n"))
            }
        }
    }
}

/**
 * Props for comment card component
 */
external interface CommentWindowProps : PropsWithChildren {
    /**
     * User comment
     */
    var comment: CommentDto

    /**
     * [UserInfo] of current user
     */
    var currentUserInfo: UserInfo?

    /**
     * Callback invoked to set selected comment for deletion
     */
    var setCommentForDeletion: (CommentDto) -> Unit
}

/**
 * Props for new comment card component
 */
external interface NewCommentWindowProps : PropsWithChildren {
    /**
     * Callback invoked when added new comment
     */
    var addComment: () -> Unit

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo
}

private fun UserInfo.canDelete(commentDto: CommentDto) = isSuperAdmin() || name == commentDto.userName

@Suppress("MAGIC_NUMBER", "IDENTIFIER_LENGTH")
private fun ChildrenBuilder.renderLeftColumn(
    userAvatar: String?,
    name: String,
    rating: Long,
    t: TranslationFunction,
    color: String = "#f1f1f1",
) {
    val (avatar, setAvatar) = useState(userAvatar?.avatarRenderer() ?: AVATAR_PROFILE_PLACEHOLDER)

    div {
        className = ClassName("input-group-prepend col-2")
        style = jso {
            background = color.unsafeCast<Background>()
        }
        div {
            className = ClassName("mb-0 font-weight-bold text-gray-800")
            form {
                div {
                    className = ClassName("row justify-content-center g-3 ml-3 mr-3 pb-2 pt-2 border-bottom-0")
                    div {
                        className = ClassName("md-4 pl-0 pr-0")
                        Link {
                            img {
                                className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                src = avatar
                                height = 80.0
                                width = 80.0
                                onError = { setAvatar(AVATAR_PLACEHOLDER) }
                            }
                            to = "/${FrontendCosvRoutes.PROFILE}/$name"
                        }
                    }
                    div {
                        className = ClassName("row mt-2 md-6 pl-0")
                        style = jso {
                            display = Display.flex
                            alignItems = AlignItems.center
                        }
                        div {
                            className = ClassName("col-12 text-center text-xs font-weight-bold text-info text-uppercase")
                            +"Rating".t()
                        }
                        div {
                            className = ClassName("col-12 text-center text-xs")
                            +rating.toString()
                        }
                        h1 {
                            className = ClassName("col-12 text-center font-weight-bold h5")
                            Link {
                                to = "/${FrontendCosvRoutes.PROFILE}/$name"
                                +name
                            }
                        }
                    }
                }
            }
        }
    }
}
