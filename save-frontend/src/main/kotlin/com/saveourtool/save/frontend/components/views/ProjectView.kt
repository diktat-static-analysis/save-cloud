/**
 * A view with project details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.common.domain.*
import com.saveourtool.common.entities.*
import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.utils.getHighestRole
import com.saveourtool.frontend.common.components.RequestStatusContext
import com.saveourtool.frontend.common.components.modal.displayModal
import com.saveourtool.frontend.common.components.modal.mediumTransparentModalStyle
import com.saveourtool.frontend.common.components.requestStatusContext
import com.saveourtool.frontend.common.components.views.AbstractView
import com.saveourtool.frontend.common.http.getProject
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopResponseHandler
import com.saveourtool.save.frontend.components.basic.projects.*
import com.saveourtool.save.frontend.utils.ProjectMenuBar

import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p
import remix.run.router.Location
import web.cssom.ClassName
import web.cssom.Cursor

import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [Props] retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectViewProps : PropsWithChildren {
    var owner: String
    var name: String
    var currentUserInfo: UserInfo?
    var location: Location<*>
}

/**
 * [State] of project view component
 */
external interface ProjectViewState : StateWithRole {
    /**
     * Currently loaded for display Project
     */
    var project: ProjectDto

    /**
     * Message of error
     */
    var errorMessage: String

    /**
     * Flag to handle error
     */
    var isErrorOpen: Boolean

    /**
     * Error label
     */
    var errorLabel: String

    /**
     * latest execution id for this project
     */
    var latestExecutionId: Long?

    /**
     * User role in project
     */
    var projectRole: Role

    /**
     * User role in organization
     */
    var organizationRole: Role

    /**
     * Label that will be shown on close button
     */
    var closeButtonLabel: String?

    /**
     * Currently selected [ProjectMenuBar] tab
     */
    var selectedMenu: ProjectMenuBar
}

/**
 * A Component for project view
 * Each modal opening call causes re-render of the whole page, that's why we need to use state for all fields
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress("MAGIC_NUMBER")
class ProjectView : AbstractView<ProjectViewProps, ProjectViewState>(Style.SAVE_LIGHT) {
    init {
        state.project = ProjectDto.empty
        state.isErrorOpen = false
        state.errorMessage = ""
        state.errorLabel = ""
        state.selectedMenu = ProjectMenuBar.defaultTab
        state.closeButtonLabel = null
        state.selfRole = Role.NONE
    }

    @Suppress("TOO_LONG_FUNCTION")
    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch {
            val result = getProject(props.name, props.owner)
            val project = if (result.isFailure) {
                return@launch
            } else {
                result.getOrThrow()
            }
            setState {
                this.project = project
            }
            val currentUserRoleInProject: Role = get(
                "$apiUrl/projects/${project.organizationName}/${project.name}/users/roles",
                jsonHeaders,
                loadingHandler = ::classLoadingHandler,
            ).decodeFromJsonString()

            val currentUserRoleInOrganization: Role = get(
                url = "$apiUrl/organizations/${project.organizationName}/users/roles",
                headers = jsonHeaders,
                loadingHandler = ::classLoadingHandler,
            ).decodeFromJsonString()

            val currentUserRole = getHighestRole(currentUserRoleInProject, currentUserRoleInOrganization)

            val role = getHighestRole(currentUserRole, props.currentUserInfo?.globalRole)
            setState {
                selfRole = role
                projectRole = currentUserRoleInProject
                organizationRole = currentUserRoleInOrganization
            }

            fetchLatestExecutionId()
        }
    }

    private fun NavigateFunctionContext.submitRequest(url: String, headers: Headers, body: dynamic) {
        scope.launch {
            val response = post(
                apiUrl + url,
                headers,
                body,
                loadingHandler = ::classLoadingHandler,
            )
            if (response.ok) {
                navigate(to = "/${state.project.organizationName}/${state.project.name}/history")
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "ComplexMethod")
    override fun ChildrenBuilder.render() {
        val modalCloseCallback = {
            setState {
                isErrorOpen = false
                closeButtonLabel = null
            }
        }
        displayModal(
            state.isErrorOpen,
            state.errorLabel,
            state.errorMessage,
            mediumTransparentModalStyle,
            modalCloseCallback,
        ) {
            buttonBuilder(state.closeButtonLabel ?: "Close", "secondary") {
                modalCloseCallback()
            }
        }
        // Page Heading
        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mb-4")
            h1 {
                className = ClassName("h3 mb-0 text-gray-800")
                +" Project ${state.project.name}"
            }
            privacySpan(state.project)
        }

        renderProjectMenuBar()

        when (state.selectedMenu) {
            ProjectMenuBar.RUN -> renderRun()
            ProjectMenuBar.STATISTICS -> renderStatistics()
            ProjectMenuBar.SETTINGS -> renderSettings()
            ProjectMenuBar.INFO -> renderInfo()
            ProjectMenuBar.DEMO -> renderDemo()
            ProjectMenuBar.FILES -> renderFiles()
        }
    }

    private fun ChildrenBuilder.renderSecurity() {
        projectSecurityMenu {
            project = state.project
            currentUserInfo = props.currentUserInfo ?: UserInfo(name = "Unknown")
        }
    }

    private fun ChildrenBuilder.renderFiles() {
        projectFilesMenu {
            project = state.project
            currentUserInfo = props.currentUserInfo ?: UserInfo(name = "Unknown")
            selfRole = state.selfRole
        }
    }

    private fun ChildrenBuilder.renderDemo() {
        projectDemoMenu {
            projectName = props.name
            organizationName = props.owner
            userProjectRole = calculateUserRoleToProject()
            updateErrorMessage = { label, message ->
                setState {
                    errorLabel = label
                    errorMessage = message
                    isErrorOpen = true
                }
            }
        }
    }

    private fun ChildrenBuilder.renderProjectMenuBar() {
        div {
            className = ClassName("row align-items-center justify-content-center")
            nav {
                className = ClassName("nav nav-tabs mb-4")
                ProjectMenuBar.values()
                    .filterNot {
                        it in listOf(ProjectMenuBar.RUN, ProjectMenuBar.SETTINGS, ProjectMenuBar.FILES) && !state.selfRole.isHigherOrEqualThan(Role.ADMIN)
                    }
                    .forEach { projectMenu ->
                        li {
                            className = ClassName("nav-item")
                            style = jso {
                                cursor = "pointer".unsafeCast<Cursor>()
                            }
                            val classVal = if (state.selectedMenu == projectMenu) " active font-weight-bold" else ""
                            p {
                                className = ClassName("nav-link $classVal text-gray-800")
                                onClick = {
                                    if (state.selectedMenu != projectMenu) {
                                        setState { selectedMenu = projectMenu }
                                    }
                                }
                                +projectMenu.name
                            }
                        }
                    }
            }
        }
    }

    private fun ChildrenBuilder.renderRun() {
        projectRunMenu {
            organizationName = props.owner
            projectName = props.name
            latestExecutionId = state.latestExecutionId
            submitExecutionRequest = { context, executionRequest ->
                context.submitRequest("/run/trigger", jsonHeaders, Json.encodeToString(executionRequest))
            }
        }
    }

    private fun ChildrenBuilder.renderStatistics() {
        projectStatisticMenu {
            executionId = state.latestExecutionId
        }
    }

    private fun ChildrenBuilder.renderInfo() {
        projectInfoMenu {
            projectName = props.name
            organizationName = props.owner
            latestExecutionId = state.latestExecutionId
        }
    }

    private fun ChildrenBuilder.renderSettings() {
        projectSettingsMenu {
            project = state.project
            currentUserInfo = props.currentUserInfo ?: UserInfo(name = "Unknown")
            selfRole = state.selfRole
            updateErrorMessage = { response, message ->
                setState {
                    errorLabel = response.statusText
                    errorMessage = message
                    isErrorOpen = true
                }
            }
        }
    }

    private fun calculateUserRoleToProject() = when {
        props.currentUserInfo?.globalRole.isSuperAdmin() -> Role.SUPER_ADMIN
        state.organizationRole.isHigherOrEqualThan(Role.OWNER) -> state.organizationRole
        else -> state.projectRole
    }

    private suspend fun fetchLatestExecutionId() {
        val response = get(
            "$apiUrl/latestExecution?name=${state.project.name}&organizationName=${state.project.organizationName}",
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        )
        when {
            !response.ok -> setState {
                errorLabel = "Failed to fetch latest execution"
                errorMessage =
                        "Failed to fetch latest execution: [${response.status}] ${response.statusText}, please refresh the page and try again"
                latestExecutionId = null
            }
            response.status == 204.toShort() -> setState {
                latestExecutionId = null
            }
            else -> {
                val executionIdFromResponse: Long = response
                    .decodeFromJsonString<ExecutionDto>().id

                setState {
                    latestExecutionId = executionIdFromResponse
                }
            }
        }
    }

    companion object :
        RStatics<ProjectViewProps, ProjectViewState, ProjectView, Context<RequestStatusContext?>>(ProjectView::class) {
        const val TEST_ROOT_DIR_HINT = """
            The path you are providing should be relative to the root directory of your repository.
            This directory should contain <a href = "https://github.com/saveourtool/save#how-to-configure"> save.properties </a>
            or <a href = "https://github.com/saveourtool/save-cli#-savetoml-configuration-file">save.toml</a> files.
            For example, if the URL to your repo with tests is: 
            <a href ="https://github.com/saveourtool/save-cli/">https://github.com/saveourtool/save</a>, then
            you need to specify the following directory with 'save.toml': 
            <a href ="https://github.com/saveourtool/save-cli/tree/main/examples/kotlin-diktat">examples/kotlin-diktat/</a>.
 
            Please note, that the tested tool and it's resources will be copied to this directory before the run.
            """

        init {
            contextType = requestStatusContext
        }
    }
}
