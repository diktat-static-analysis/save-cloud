@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.common.entities.CommentDto
import com.saveourtool.common.entities.ProjectProblemDto
import com.saveourtool.common.validation.FrontendCosvRoutes
import com.saveourtool.frontend.common.components.basic.cardComponent
import com.saveourtool.frontend.common.components.basic.commentWindow
import com.saveourtool.frontend.common.components.basic.markdown
import com.saveourtool.frontend.common.components.basic.newCommentWindow
import com.saveourtool.frontend.common.components.modal.displayModal
import com.saveourtool.frontend.common.components.modal.mediumTransparentModalStyle
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopLoadingHandler

import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.span
import react.router.dom.Link
import react.router.useNavigate
import react.useState
import web.cssom.*

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val projectProblem: FC<ProjectProblemViewProps> = FC {props ->

    val (projectProblem, setProjectProblem) = useState(ProjectProblemDto.empty)
    val (comments, setComments) = useState(emptyList<CommentDto>())

    val closeProjectProblemWindowOpenness = useWindowOpenness()
    val editProjectProblemWindowOpenness = useWindowOpenness()
    val navigate = useNavigate()

    useRequest {
        val projectProblemNew: ProjectProblemDto = get(
            url = "$apiUrl/projects/problem/get/by-id",
            params = jso<dynamic> {
                id = props.projectProblemId
            },
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }

        setProjectProblem(projectProblemNew)

        val newComments = get(
            url = "$apiUrl/comments/get-all",
            headers = jsonHeaders,
            params = jso<dynamic> {
                section = window.location.pathname
            },
            loadingHandler = ::loadingHandler,
        ).unsafeMap {
            it.decodeFromJsonString<List<CommentDto>>()
        }

        setComments(newComments)
    }

    val enrollCommentsRequest = useDeferredRequest {
        val newComments = get(
            url = "$apiUrl/comments/get-all",
            headers = jsonHeaders,
            params = jso<dynamic> {
                section = window.location.pathname
            },
            loadingHandler = ::loadingHandler,
        ).unsafeMap {
            it.decodeFromJsonString<List<CommentDto>>()
        }

        setComments(newComments)
    }

    val enrollCloseOpenRequest = useDeferredRequest {
        val response = post(
            url = "$apiUrl/projects/problem/update",
            headers = jsonHeaders,
            body = Json.encodeToString(projectProblem),
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            navigate(to = "/project/${props.organizationName}/${props.projectName}/security")
        }
    }

    val columnCard = cardComponent(isBordered = true, hasBg = true, isNoPadding = false, isPaddingBottomNull = true)

    displayModal(
        closeProjectProblemWindowOpenness.isOpen(),
        "${if (projectProblem.isClosed) "Close" else "Reopen"} of ${projectProblem.name}",
        "Are you sure you want to ${if (projectProblem.isClosed) "close" else "reopen"} this problem?",
        mediumTransparentModalStyle,
        closeProjectProblemWindowOpenness.closeWindowAction(),
    ) {
        buttonBuilder("Ok") {
            enrollCloseOpenRequest()
            closeProjectProblemWindowOpenness.closeWindow()
        }
        buttonBuilder("Close", "secondary") {
            setProjectProblem { it.copy(isClosed = !it.isClosed) }
            closeProjectProblemWindowOpenness.closeWindow()
        }
    }

    editProjectProblemWindow {
        windowOpenness = editProjectProblemWindowOpenness
        problem = projectProblem
    }

    div {
        className = ClassName("d-sm-flex align-items-center justify-content-center mb-4")
        h1 {
            className = ClassName("h3 mb-0 text-gray-800")
            +projectProblem.name
        }
        span {
            className = ClassName("border border-danger ml-2 pr-1 pl-1 text-red-700")
            style = jso {
                borderRadius = "2em".unsafeCast<BorderRadius>()
            }
            +projectProblem.critical.toString()
        }
    }
    div {
        className = ClassName("d-flex justify-content-end")
        buttonBuilder(label = "Edit", classes = "mr-2") {
            // setProjectProblem { it.copy(isClosed = true) }
            editProjectProblemWindowOpenness.openWindow()
        }
        if (!projectProblem.isClosed) {
            buttonBuilder(label = "Close", style = "danger", classes = "mr-2") {
                setProjectProblem { it.copy(isClosed = true) }
                closeProjectProblemWindowOpenness.openWindow()
            }
        } else {
            buttonBuilder(label = "Reopen", classes = "mr-2") {
                setProjectProblem { it.copy(isClosed = false) }
                closeProjectProblemWindowOpenness.openWindow()
            }
        }
    }

    div {
        className = ClassName("row justify-content-center")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Information"
            }

            div {
                className = ClassName("card card-body mt-0 pt-0 pr-0 pl-0 border-secondary")
                div {
                    className = ClassName("col mr-2 pr-0 pl-0")
                    nav {
                        div {
                            className = ClassName("pl-3 ui vertical menu profile-setting")
                            form {
                                div {
                                    className = ClassName("item mt-2")
                                    div {
                                        className = ClassName("header")
                                        +"Severity:"
                                    }
                                    div {
                                        className = ClassName("menu")
                                        div {
                                            className = ClassName("mt-2")
                                            span {
                                                className =
                                                        ClassName("border border-danger ml-2 pr-1 pl-1 text-red-700")
                                                style = jso {
                                                    borderRadius = "2em".unsafeCast<BorderRadius>()
                                                }
                                                +projectProblem.critical.toString()
                                            }
                                        }
                                    }
                                }
                            }
                            form {
                                div {
                                    className = ClassName("item mt-2")
                                    div {
                                        className = ClassName("header")
                                        +"CVE ID:"
                                    }
                                    div {
                                        className = ClassName("menu")
                                        div {
                                            className = ClassName("mt-2 pl-2")
                                            projectProblem.identifier?.let {
                                                Link {
                                                    to = "https://cosv.gitlink.org.cn/${FrontendCosvRoutes.VULN}/${projectProblem.identifier}"
                                                    +it
                                                }
                                            } ?: +"No known CVE"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===================== RIGHT COLUMN =======================================================================
        div {
            className = ClassName("col-6")

            div {
                className = ClassName("col-12")
                div {
                    className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Description"
                }
                div {
                    className = ClassName("text-left")
                    columnCard {
                        markdown(projectProblem.description.split("\n").joinToString("\n\n"))
                    }
                }
            }

            div {
                className = ClassName("col-12 mt-4")
                newCommentWindow {
                    addComment = { enrollCommentsRequest() }
                }
            }

            comments.forEach { message ->
                div {
                    className = ClassName("col-12 mt-4")
                    commentWindow {
                        comment = message
                    }
                }
            }
        }
    }
}

/**
 * [Props] for ProjectProblemView
 */
external interface ProjectProblemViewProps : Props {
    /**
     * Name of security vulnerabilities
     */
    var organizationName: String

    /**
     * Information about current user
     */
    var projectName: String

    /**
     * Information about project problem
     */
    var projectProblemId: Long
}
