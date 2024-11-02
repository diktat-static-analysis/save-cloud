@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.OrganizationDto
import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.common.entities.ProjectStatus
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.basic.scoreCard
import com.saveourtool.frontend.common.components.tables.TableProps
import com.saveourtool.frontend.common.components.tables.columns
import com.saveourtool.frontend.common.components.tables.tableComponent
import com.saveourtool.frontend.common.components.tables.value
import com.saveourtool.frontend.common.components.views.*
import com.saveourtool.frontend.common.components.views.usersettings.right.actionButtonClasses
import com.saveourtool.frontend.common.components.views.usersettings.right.actionIconClasses
import com.saveourtool.frontend.common.externals.fontawesome.faRedo
import com.saveourtool.frontend.common.externals.fontawesome.faTrashAlt
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.isSuperAdmin

import org.w3c.fetch.Response
import react.*
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import web.cssom.ClassName
import web.html.ButtonType

/**
 * The mandatory column id.
 * For each cell, this will be transformed into "cell_%d_delete_button" and
 * visible as the key in the "Components" tab of the developer tools.
 */
const val DELETE_BUTTON_COLUMN_ID = "delete_button"

/**
 * Empty table header.
 */
const val EMPTY_COLUMN_HEADER = ""

val organizationToolsMenu = organizationToolsMenu()

/**
 * OrganizationToolsMenu component props
 */
external interface OrganizationToolsMenuProps : Props {
    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo?

    /**
     * [Role] of user that is observing this component
     */
    var selfRole: Role

    /**
     * Current organization
     */
    var organization: OrganizationDto

    /**
     * Organization projects
     */
    var projects: List<ProjectDto>

    /**
     * lambda for update projects
     */
    var updateProjects: (List<ProjectDto>) -> Unit
}

private fun ChildrenBuilder.renderTopProject(topProject: ProjectDto?, organizationName: String) {
    div {
        className = ClassName("col-6 mb-4")
        topProject?.let {
            scoreCard {
                name = it.name
                contestScore = it.contestRating
                url = "/$organizationName/${it.name}"
            }
        }
    }
}

/**
 * Makes a call to change project status
 *
 * @param status - the status that will be assigned to the project [project]
 * @param projectPath - the path [organizationName/projectName] for response
 * @return lazy response
 */
fun responseChangeProjectStatus(projectPath: String, status: ProjectStatus): suspend WithRequestStatusContext.() -> Response = {
    post(
        url = "$apiUrl/projects/$projectPath/change-status?status=$status",
        headers = jsonHeaders,
        body = undefined,
        loadingHandler = ::noopLoadingHandler,
        responseHandler = ::noopResponseHandler,
    )
}

/**
 * Removes the projects specified by [oldProjects], adds projects specified by [newProjects],
 * and sorts the resulting list by their status and then by name.
 *
 * @param projects is list of the projects
 * @param oldProject is an old project, it needs to be removed from the list
 * @param newProject is a new project, it needs to be added to the list
 * @param setProjects is setter to update projects
 * @param updateProjects method from props, for changing props
 */
@Suppress("TYPE_ALIAS")
private fun updateOneProjectInProjects(
    projects: List<ProjectDto>,
    oldProject: ProjectDto,
    newProject: ProjectDto,
    updateProjects: (List<ProjectDto>) -> Unit,
) {
    val comparator: Comparator<ProjectDto> =
            compareBy<ProjectDto> { it.status.ordinal }
                .thenBy { it.name }
    projects
        .minus(oldProject)
        .plus(newProject)
        .sortedWith(comparator)
        .also { sortedProjects ->
            updateProjects(sortedProjects)
        }
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod", "CyclomaticComplexMethod")
private fun organizationToolsMenu() = FC<OrganizationToolsMenuProps> { props ->
    @Suppress("TYPE_ALIAS")
    val tableWithProjects: FC<TableProps<ProjectDto>> = tableComponent(
        columns = {
            columns {
                column(id = "name", header = "Evaluated Tool", { name }) { cellContext ->
                    Fragment.create {
                        val projectDto = cellContext.row.original
                        td {
                            className = ClassName("align-middle text-center")
                            when (projectDto.status) {
                                ProjectStatus.CREATED -> div {
                                    Link {
                                        to = "/${projectDto.organizationName}/${cellContext.value}"
                                        +cellContext.value
                                    }
                                    spanWithClassesAndText("text-muted", "active")
                                }
                                ProjectStatus.DELETED -> div {
                                    className = ClassName("text-secondary")
                                    +cellContext.value
                                    spanWithClassesAndText("text-secondary", "deleted")
                                }
                                ProjectStatus.BANNED -> div {
                                    className = ClassName("text-danger")
                                    +cellContext.value
                                    spanWithClassesAndText("text-danger", "banned")
                                }
                            }
                        }
                    }
                }
                column(id = "description", header = "Description") {
                    Fragment.create {
                        td {
                            className = ClassName("align-middle text-center")
                            +it.value.description
                        }
                    }
                }
                column(id = "rating", header = "Contest Rating") {
                    Fragment.create {
                        td {
                            className = ClassName("align-middle text-center")
                            +"0"
                        }
                    }
                }

                /*
                 * A "secret" possibility to delete projects (intended for super-admins).
                 */
                if (props.selfRole.isHigherOrEqualThan(Role.OWNER)) {
                    column(id = DELETE_BUTTON_COLUMN_ID, header = EMPTY_COLUMN_HEADER) { cellProps ->
                        Fragment.create {
                            td {
                                className = ClassName("align-middle text-center")
                                val project = cellProps.row.original
                                val projectName = project.name

                                when (project.status) {
                                    ProjectStatus.CREATED -> actionButton {
                                        title = "WARNING: You are about to delete this project"
                                        errorTitle = "You cannot delete the project $projectName"
                                        message = """Are you sure you want to delete the project "$projectName"?"""
                                        clickMessage = "Also ban this project"
                                        buttonStyleBuilder = { childrenBuilder ->
                                            with(childrenBuilder) {
                                                fontAwesomeIcon(icon = faTrashAlt, classes = actionIconClasses.joinToString(" "))
                                            }
                                        }
                                        classes = actionButtonClasses.joinToString(" ")
                                        modalButtons = { action, closeWindow, childrenBuilder, isClickMode ->
                                            val actionName = if (isClickMode) "ban" else "delete"
                                            with(childrenBuilder) {
                                                buttonBuilder(label = "Yes, $actionName $projectName", style = "danger", classes = "mr-2") {
                                                    action()
                                                    closeWindow()
                                                }
                                                buttonBuilder("Cancel") {
                                                    closeWindow()
                                                }
                                            }
                                        }
                                        onActionSuccess = { isBanMode ->
                                            updateOneProjectInProjects(props.projects,
                                                project,
                                                project.copy(status = if (isBanMode) ProjectStatus.BANNED else ProjectStatus.DELETED),
                                                props.updateProjects,
                                            )
                                        }
                                        conditionClick = props.currentUserInfo.isSuperAdmin()
                                        sendRequest = { isBanned ->
                                            val newStatus = if (isBanned) ProjectStatus.BANNED else ProjectStatus.DELETED
                                            responseChangeProjectStatus("${project.organizationName}/${project.name}", newStatus)
                                        }
                                    }
                                    ProjectStatus.DELETED -> actionButton {
                                        title = "WARNING: You are about to recover this project"
                                        errorTitle = "You cannot recover the project $projectName"
                                        message = """Are you sure you want to recover the project "$projectName"?"""
                                        buttonStyleBuilder = { childrenBuilder ->
                                            with(childrenBuilder) {
                                                fontAwesomeIcon(icon = faRedo, classes = actionIconClasses.joinToString(" "))
                                            }
                                        }
                                        classes = actionButtonClasses.joinToString(" ")
                                        modalButtons = { action, closeWindow, childrenBuilder, _ ->
                                            with(childrenBuilder) {
                                                buttonBuilder(label = "Yes, recover $projectName", style = "warning", classes = "mr-2") {
                                                    action()
                                                    closeWindow()
                                                }
                                                buttonBuilder("Cancel") {
                                                    closeWindow()
                                                }
                                            }
                                        }
                                        onActionSuccess = { _ ->
                                            updateOneProjectInProjects(
                                                props.projects,
                                                project,
                                                project.copy(status = ProjectStatus.CREATED),
                                                props.updateProjects,
                                            )
                                        }
                                        conditionClick = false
                                        sendRequest = { _ ->
                                            responseChangeProjectStatus("${project.organizationName}/${project.name}", ProjectStatus.CREATED)
                                        }
                                    }
                                    ProjectStatus.BANNED -> if (props.currentUserInfo.isSuperAdmin()) {
                                        actionButton {
                                            title = "WARNING: You are about to unban this BANNED project"
                                            errorTitle = "You cannot unban the project $projectName"
                                            message = """Are you sure you want to unban the project "$projectName"?"""
                                            buttonStyleBuilder = { childrenBuilder ->
                                                with(childrenBuilder) {
                                                    fontAwesomeIcon(icon = faRedo, classes = actionIconClasses.joinToString(" "))
                                                }
                                            }
                                            classes = actionButtonClasses.joinToString(" ")
                                            modalButtons = { action, closeWindow, childrenBuilder, _ ->
                                                with(childrenBuilder) {
                                                    buttonBuilder(label = "Yes, unban $projectName", style = "danger", classes = "mr-2") {
                                                        action()
                                                        closeWindow()
                                                    }
                                                    buttonBuilder("Cancel") {
                                                        closeWindow()
                                                    }
                                                }
                                            }
                                            onActionSuccess = { _ ->
                                                updateOneProjectInProjects(
                                                    props.projects,
                                                    project,
                                                    project.copy(status = ProjectStatus.CREATED),
                                                    props.updateProjects,
                                                )
                                            }
                                            conditionClick = false
                                            sendRequest = { _ ->
                                                responseChangeProjectStatus("${project.organizationName}/${project.name}", ProjectStatus.CREATED)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { tableProps ->
        /*-
         * Necessary for the table to get re-rendered once a project gets
         * deleted.
         *
         * The order and size of the array must remain constant.
         */
        arrayOf(tableProps)
    }

    div {
        className = ClassName("row justify-content-center")
        div {
            className = ClassName("col-6")
            div {
                className = ClassName("d-flex justify-content-center mb-2")
                if (props.selfRole.isHigherOrEqualThan(Role.ADMIN)) {
                    Link {
                        to = "/${FrontendRoutes.CREATE_PROJECT}/${props.organization?.name}"
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-outline-info")
                            +"Add new Tool"
                        }
                    }
                }
            }

            // ================= Rows for TOP projects ================
            val topProjects = props.projects.sortedByDescending { it.contestRating }.take(TOP_PROJECTS_NUMBER)

            if (topProjects.isNotEmpty()) {
                // ================= Title for TOP projects ===============
                div {
                    className = ClassName("row justify-content-center mb-2")
                    ReactHTML.h4 {
                        +"Top Tools"
                    }
                }
                div {
                    className = ClassName("row justify-content-center")

                    renderTopProject(topProjects.getOrNull(0), props.organization.name)
                    renderTopProject(topProjects.getOrNull(1), props.organization.name)
                }

                @Suppress("MAGIC_NUMBER")
                (div {
                    className = ClassName("row justify-content-center")

                    renderTopProject(topProjects.getOrNull(2), props.organization.name)
                    renderTopProject(topProjects.getOrNull(3), props.organization.name)
                })
            }

            tableWithProjects {
                getData = { _, _ ->
                    props.projects.toTypedArray()
                }
                getPageCount = null
            }
        }
    }
}
