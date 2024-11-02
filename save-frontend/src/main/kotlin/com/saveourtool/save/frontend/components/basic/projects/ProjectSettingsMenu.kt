@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.common.entities.ProjectStatus
import com.saveourtool.common.info.UserInfo
import com.saveourtool.frontend.common.components.basic.manageUserRoleCardComponent
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.inputform.inputTextFormOptional
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopLoadingHandler

import org.w3c.fetch.Response
import react.*
import react.dom.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.router.useNavigate
import web.cssom.ClassName
import web.html.ButtonType
import web.html.HTMLInputElement
import web.html.InputType

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SETTINGS tab in ProjectView
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod",
    "EMPTY_BLOCK_STRUCTURE_ERROR"
)
val projectSettingsMenu: FC<ProjectSettingsMenuProps> = FC { props ->
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    val projectRef = useRef(props.project)
    val (draftProject, setDraftProject) = useState(props.project)
    useEffect(props.project) {
        if (projectRef.current !== props.project) {
            setDraftProject(props.project)
            projectRef.current = props.project
        }
    }
    val navigate = useNavigate()

    val projectPath = props.project.let { "${it.organizationName}/${it.name}" }

    val updateProject = useDeferredRequest {
        post(
            url = "$apiUrl/projects/update",
            headers = jsonHeaders,
            body = Json.encodeToString(draftProject),
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        ).let {
            if (it.ok) {
                props.onProjectUpdate(draftProject)
            }
        }
    }

    div {
        className = ClassName("row justify-content-center mb-2 text-gray-900")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-4 mb-2 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Users"
            }
            manageUserRoleCardComponent {
                selfUserInfo = props.currentUserInfo
                groupPath = projectPath
                groupType = "project"
                getUserGroups = { it.projects }
            }
        }
        // ===================== RIGHT COLUMN ======================================================================
        div {
            className = ClassName("col-4 mb-2 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Main settings"
            }
            div {
                className = ClassName("card border card-body mt-0")
                div {
                    className = ClassName("row mt-2 ml-2 mr-2")
                    div {
                        className = ClassName("col-5 text-left align-self-center")
                        +"Project email:"
                    }
                    div {
                        className = ClassName("col-7 input-group pl-0")
                        inputTextFormOptional {
                            form = InputTypes.PROJECT_EMAIL
                            textValue = draftProject.email
                            classes = ""
                            name = null
                            validInput = draftProject.validateEmail()
                            onChangeFun = {
                                setDraftProject(draftProject.copy(email = it.target.value))
                            }
                        }
                    }
                }
                div {
                    className = ClassName("row mt-2 ml-2 mr-2")
                    div {
                        className = ClassName("col-5 text-left align-self-center")
                        +"Project visibility:"
                    }
                    form {
                        className = ClassName("col-7 form-group row")
                        div {
                            className = ClassName("form-check-inline")
                            input {
                                className = ClassName("form-check-input")
                                defaultChecked = draftProject.isPublic
                                name = "projectVisibility"
                                type = InputType.radio
                                id = "isProjectPublicSwitch"
                                value = "public"
                            }
                            label {
                                className = ClassName("form-check-label")
                                htmlFor = "isProjectPublicSwitch"
                                +"Public"
                            }
                        }
                        div {
                            className = ClassName("form-check-inline")
                            input {
                                className = ClassName("form-check-input")
                                defaultChecked = !draftProject.isPublic
                                name = "projectVisibility"
                                type = InputType.radio
                                id = "isProjectPrivateSwitch"
                                value = "private"
                            }
                            label {
                                className = ClassName("form-check-label")
                                htmlFor = "isProjectPrivateSwitch"
                                +"Private"
                            }
                        }
                        onChange = {
                            setDraftProject(draftProject.copy(isPublic = (it.target as HTMLInputElement).value == "public"))
                        }
                    }
                }
                div {
                    className = ClassName("row d-flex align-items-center mt-2 mr-2 ml-2")
                    div {
                        className = ClassName("col-5 text-left")
                        +"Number of containers:"
                    }
                    div {
                        className = ClassName("col-7 row")
                        div {
                            className = ClassName("form-switch")
                            select {
                                className = ClassName("custom-select")
                                // fixme: later we will need to change amount of containers
                                disabled = true
                                onChange = {
                                    setDraftProject(draftProject.copy(numberOfContainers = it.target.value.toInt()))
                                }
                                id = "numberOfContainers"
                                for (i in 1..8) {
                                    option {
                                        value = i.toString()
                                        selected = i == draftProject.numberOfContainers
                                        +i.toString()
                                    }
                                }
                            }
                        }
                    }
                }

                hr {}
                div {
                    className = ClassName("row d-flex justify-content-center")
                    div {
                        className = ClassName("col-3 d-sm-flex align-items-center justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-sm btn-outline-primary")
                            onClick = {
                                updateProject()
                            }
                            +"Save changes"
                        }
                    }
                    div {
                        className = ClassName("col-3 d-sm-flex align-items-center justify-content-center")
                        actionButton {
                            title = "WARNING: You are about to delete this project"
                            errorTitle = "You cannot delete the project ${props.project.name}"
                            message = "Are you sure you want to delete the project $projectPath?"
                            clickMessage = "Also ban this project"
                            onActionSuccess = { _ ->
                                navigate(to = "/${props.project.organizationName}")
                            }
                            buttonStyleBuilder = { childrenBuilder ->
                                with(childrenBuilder) {
                                    +"Delete project"
                                }
                            }
                            classes = "btn btn-sm btn-outline-danger"
                            modalButtons = { action, closeWindow, childrenBuilder, isClickMode ->
                                val actionName = if (isClickMode) "ban" else "delete"
                                with(childrenBuilder) {
                                    buttonBuilder(
                                        label = "Yes, $actionName ${props.project.name}",
                                        style = "danger",
                                        classes = "mr-2"
                                    ) {
                                        action()
                                        closeWindow()
                                    }
                                    buttonBuilder("Cancel") {
                                        closeWindow()
                                    }
                                }
                            }
                            conditionClick = props.currentUserInfo.isSuperAdmin()
                            sendRequest = { isBanned ->
                                val newStatus = if (isBanned) ProjectStatus.BANNED else ProjectStatus.DELETED
                                responseChangeProjectStatus(projectPath, newStatus)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ProjectSettingsMenu component props
 */
external interface ProjectSettingsMenuProps : Props {
    /**
     * Current project settings
     */
    var project: ProjectDto

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo

    /**
     * Role of a current user
     */
    var selfRole: Role

    /**
     * Callback to update project state in ProjectView after update request's response is received.
     */
    var onProjectUpdate: (ProjectDto) -> Unit

    /**
     * Callback to show error message
     */
    @Suppress("TYPE_ALIAS")
    var updateErrorMessage: (Response, String) -> Unit
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
