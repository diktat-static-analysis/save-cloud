@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.components.basic.manageUserRoleCardComponent
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.inputform.inputTextFormOptional
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.views.responseDeleteProject
import com.saveourtool.save.frontend.utils.actionButton
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import org.w3c.dom.HTMLInputElement
import org.w3c.fetch.Response
import react.*
import react.dom.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.router.useNavigate

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SETTINGS tab in ProjectView
 */
val projectSettingsMenu = projectSettingsMenu()

/**
 * ProjectSettingsMenu component props
 */
external interface ProjectSettingsMenuProps : Props {
    /**
     * Current project settings
     */
    var project: Project

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
    var onProjectUpdate: (Project) -> Unit

    /**
     * Callback to show error message
     */
    @Suppress("TYPE_ALIAS")
    var updateErrorMessage: (Response, String) -> Unit

    /**
     * Callback to show notification message
     */
    var updateNotificationMessage: (String, String) -> Unit
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod",
    "EMPTY_BLOCK_STRUCTURE_ERROR"
)
private fun projectSettingsMenu() = FC<ProjectSettingsMenuProps> { props ->
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

    val projectPath = props.project.let { "${it.organization.name}/${it.name}" }
    val deleteProject = useDeferredRequest {
        val responseFromDeleteProject = delete(
            "$apiUrl/projects/$projectPath/delete",
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
        if (responseFromDeleteProject.ok) {
            navigate("/${FrontendRoutes.PROJECTS}")
        }
    }

    val updateProject = useDeferredRequest {
        post(
            "$apiUrl/projects/update",
            jsonHeaders,
            Json.encodeToString(draftProject.toDto()),
            loadingHandler = ::loadingHandler,
        ).let {
            if (it.ok) {
                props.onProjectUpdate(draftProject)
            }
        }
    }

    val deletionModalOpener = useWindowOpenness()
    displayModal(
        deletionModalOpener,
        "Warning: deletion of project",
        "You are about to delete project $projectPath. Are you sure?",
    ) {
        buttonBuilder("Yes, delete $projectPath", "danger") {
            deleteProject()
        }
        buttonBuilder("Cancel") {
            deletionModalOpener.closeWindow()
        }
    }

    val (wasConfirmationModalShown, showGlobalRoleWarning) = useGlobalRoleWarningCallback(props.updateNotificationMessage)
    div {
        className = ClassName("row justify-content-center mb-2")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Users"
            }
            manageUserRoleCardComponent {
                selfUserInfo = props.currentUserInfo
                groupPath = projectPath
                groupType = "project"
                this.wasConfirmationModalShown = wasConfirmationModalShown
                updateErrorMessage = props.updateErrorMessage
                getUserGroups = { it.projects }
                this.showGlobalRoleWarning = showGlobalRoleWarning
            }
        }
        // ===================== RIGHT COLUMN ======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Main settings"
            }
            div {
                className = ClassName("card card-body mt-0 pt-0 pr-0 pl-0")
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
                        className = ClassName("col-7 form-group row d-flex justify-content-around")
                        div {
                            className = ClassName("form-check-inline")
                            input {
                                className = ClassName("form-check-input")
                                defaultChecked = draftProject.public
                                name = "projectVisibility"
                                type = react.dom.html.InputType.radio
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
                                defaultChecked = !draftProject.public
                                name = "projectVisibility"
                                type = react.dom.html.InputType.radio
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
                            setDraftProject(draftProject.copy(public = (it.target as HTMLInputElement).value == "public"))
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
                            className = ClassName("btn btn-sm btn-primary")
                            onClick = {
                                updateProject()
                            }
                            +"Save changes"
                        }
                    }
                    div {
                        className = ClassName("col-3 d-sm-flex align-items-center justify-content-center")
                        actionButton {
                            title = "WARNING: You want to delete a project"
                            errorTitle = "You cannot delete ${props.project.name}"
                            message = "Are you sure you want to delete a $projectPath?"
                            clickMessage = "Change to ban mode"
                            onActionSuccess = {
                                window.location.href = "${window.location.origin}/"
                            }
                            buttonStyleBuilder = { childrenBuilder ->
                                with(childrenBuilder) {
                                    +"Delete ${props.project.name}"
                                }
                            }
                            classes = "btn btn-sm btn-danger"
                            modalButtons = { action, window, childrenBuilder ->
                                with(childrenBuilder) {
                                    buttonBuilder(label = "Yes, delete ${props.project.name}", style = "danger", classes = "mr-2") {
                                        action()
                                        window.closeWindow()
                                    }
                                    buttonBuilder("Cancel") {
                                        window.closeWindow()
                                    }
                                }
                            }
                            sendRequest = { typeOfAction ->
                                responseDeleteProject(typeOfAction, props.project)
                            }
                            conditionClick = props.selfRole.isHigherOrEqualThan(Role.SUPER_ADMIN)
                        }
                    }
                }
            }
        }
    }
}
