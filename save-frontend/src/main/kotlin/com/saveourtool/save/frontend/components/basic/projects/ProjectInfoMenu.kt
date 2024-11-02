@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.common.entities.contest.ContestResult
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.basic.*
import com.saveourtool.frontend.common.components.basic.userBoard
import com.saveourtool.frontend.common.externals.fontawesome.faCalendarAlt
import com.saveourtool.frontend.common.externals.fontawesome.faHistory
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.get
import com.saveourtool.frontend.common.utils.unsafeMap
import com.saveourtool.frontend.common.utils.useRequest
import com.saveourtool.save.frontend.components.basic.projectInfo

import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.router.dom.Link
import web.cssom.ClassName
import web.cssom.rem
import web.html.ButtonType

import kotlinx.browser.window

private val infoCard = cardComponent(isBordered = true, hasBg = true)

/**
 * INFO tab in ProjectView
 */
val projectInfoMenu = projectInfoMenu()

/**
 * ProjectSettingsMenu component props
 */
external interface ProjectInfoMenuProps : Props {
    /**
     * Project name
     */
    var projectName: String

    /**
     * Organization name
     */
    var organizationName: String

    /**
     * ID of the latest execution
     */
    var latestExecutionId: Long?
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod"
)
private fun projectInfoMenu() = FC<ProjectInfoMenuProps> { props ->
    val (usersInProject, setUsersInProject) = useState(emptyList<UserInfo>())
    useRequest {
        val users: List<UserInfo> = get(
            url = "$apiUrl/projects/${props.organizationName}/${props.projectName}/users",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }
        setUsersInProject(users)
    }

    val (bestResults, setBestResults) = useState(emptyList<ContestResult>())
    useRequest {
        val results: List<ContestResult> = get(
            url = "$apiUrl/contests/${props.organizationName}/${props.projectName}/best",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }
        setBestResults(results)
    }

    val (project, setProject) = useState(ProjectDto.empty)
    useRequest {
        val projectFromBackend: ProjectDto = get(
            url = "$apiUrl/projects/get/organization-name?name=${props.projectName}&organizationName=${props.organizationName}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }
        setProject(projectFromBackend)
    }

    div {
        className = ClassName("d-flex justify-content-center")
        div {
            className = ClassName("col-2 mr-3")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Best contest scores"
            }
            ul {
                className = ClassName("list-group")
                bestResults.filter { it.score != null }
                    .forEach {
                        li {
                            className = ClassName("list-group-item pl-0 pr-0 pb-0 pt-0 mb-3")
                            Link {
                                to = "/${FrontendRoutes.CONTESTS}/${it.contestName}"
                                className = ClassName("stretched-link")
                            }
                            scoreCard {
                                name = it.contestName
                                contestScore = it.score!!
                            }
                        }
                    }
            }
        }

        div {
            className = ClassName("col-3 mr-3")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Members"
            }
            userBoard {
                users = usersInProject
                avatarOuterClasses = "col-4 px-0"
                avatarInnerClasses = "mx-sm-3"
                widthAndHeight = 7.rem
            }
        }

        div {
            className = ClassName("col-3 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"General info"
            }
            infoCard {
                projectInfo {
                    this.project = project
                }
                div {
                    className = ClassName("ml-3 mt-2 align-items-left justify-content-between")
                    fontAwesomeIcon(icon = faHistory)

                    button {
                        type = ButtonType.button
                        className = ClassName("btn btn-link text-left")
                        +"Latest Execution"
                        disabled = props.latestExecutionId == null

                        onClick = {
                            window.location.href = "${window.location}/history/execution/${props.latestExecutionId}"
                        }
                    }
                }
                div {
                    className = ClassName("ml-3 align-items-left")
                    fontAwesomeIcon(icon = faCalendarAlt)
                    Link {
                        to = "/${props.organizationName}/${props.projectName}/history"
                        className = ClassName("btn btn-link text-left")
                        +"Execution History"
                    }
                }
            }
        }
    }
}
