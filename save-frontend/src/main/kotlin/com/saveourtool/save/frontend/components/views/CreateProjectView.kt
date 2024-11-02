/**
 * A view with project creation details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.common.entities.*
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.common.validation.isValidLengthName
import com.saveourtool.common.validation.isValidName
import com.saveourtool.frontend.common.components.basic.SelectFormRequiredProps
import com.saveourtool.frontend.common.components.basic.selectFormRequired
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.inputform.inputTextFormOptional
import com.saveourtool.frontend.common.components.inputform.inputTextFormRequired
import com.saveourtool.frontend.common.components.modal.displayModal
import com.saveourtool.frontend.common.components.modal.mediumTransparentModalStyle
import com.saveourtool.frontend.common.externals.fontawesome.faQuestionCircle
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.Style
import com.saveourtool.frontend.common.utils.buttonBuilder
import com.saveourtool.frontend.common.utils.particles
import com.saveourtool.frontend.common.utils.useBackground
import com.saveourtool.frontend.common.utils.useDeferredRequest

import react.*
import react.dom.*
import react.dom.aria.ariaDescribedBy
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.textarea
import react.router.dom.Link
import react.router.useNavigate
import react.router.useParams
import web.cssom.ClassName
import web.html.*
import web.html.ButtonType
import web.html.InputType

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val organizationSelectForm: SelectFormFunctionalComponent = selectFormRequired()

val createProjectView: FC<Props> = FC {
    useBackground(Style.SAVE_DARK)
    particles()
    val params = useParams()
    val navigate = useNavigate()
    val organization = params["organization"]

    val (isErrorWithProjectSave, setIsErrorWithProjectSave) = useState(false)
    val (errorMessage, setErrorMessage) = useState("")

    /*
     * Update the state if there's a parent organization available
     */
    val (projectCreationRequest, setProjectCreationRequest) = useState(
        if (!organization.isNullOrEmpty()) {
            ProjectDto.empty.copy(organizationName = organization)
        } else {
            ProjectDto.empty
        }
    )

    val (conflictErrorMessage, setConflictErrorMessage) = useState<String?>(null)

    val saveProject = useDeferredRequest {
        val responseFromCreationProject =
                post(
                    "$apiUrl/projects/save",
                    jsonHeaders,
                    Json.encodeToString(projectCreationRequest),
                    loadingHandler = ::loadingHandler,
                    responseHandler = ::responseHandlerWithValidation,
                )
        if (responseFromCreationProject.ok) {
            navigate(to = "/${projectCreationRequest.organizationName}/${projectCreationRequest.name}")
        } else if (responseFromCreationProject.isConflict()) {
            val responseText = responseFromCreationProject.unpackMessage()
            setConflictErrorMessage(responseText)
        } else {
            responseFromCreationProject.text().then {
                setIsErrorWithProjectSave(true)
                setErrorMessage(it)
            }
        }
    }

    displayModal(
        isErrorWithProjectSave,
        "Error appeared during project creation",
        errorMessage,
        mediumTransparentModalStyle,
        { setIsErrorWithProjectSave(false) },
    ) {
        buttonBuilder("Close", "secondary") {
            setIsErrorWithProjectSave(false)
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
                    className = ClassName("col-sm-4 mt-5")
                    div {
                        className = ClassName("container card o-hidden border-0 shadow-lg my-2 card-body p-0")
                        div {
                            className = ClassName("p-5 text-center")
                            h1 {
                                className = ClassName("h4 text-gray-900 mb-4")
                                +"Create new test project"
                            }
                            form {
                                className = ClassName("needs-validation")
                                div {
                                    className = ClassName("row-3")
                                    organizationSelectForm {
                                        selectClasses = "custom-select"
                                        formType = InputTypes.ORGANIZATION_NAME
                                        validInput =
                                                projectCreationRequest.organizationName.isNotEmpty() && projectCreationRequest.organizationName.isValidName()
                                        classes = "col-12 pl-2 pr-2"
                                        formName = "Organization"
                                        getData = { context ->
                                            context.get(
                                                url = "$apiUrl/organizations/get/list",
                                                headers = jsonHeaders,
                                                loadingHandler = context::loadingHandler,
                                            )
                                                .unsafeMap {
                                                    it.decodeFromJsonString<List<OrganizationDto>>()
                                                }
                                                .map {
                                                    it.name
                                                }
                                        }
                                        dataToString = { it }
                                        selectedValue = projectCreationRequest.organizationName
                                        addNewItemChildrenBuilder = { childrenBuilder ->
                                            with(childrenBuilder) {
                                                Link {
                                                    to = "/${FrontendRoutes.CREATE_ORGANIZATION}"
                                                    +"Add new organization"
                                                }
                                            }
                                        }
                                        disabled = false
                                        onChangeFun = { value ->
                                            setProjectCreationRequest(
                                                projectCreationRequest.copy(
                                                    organizationName = value ?: ""
                                                )
                                            )
                                        }
                                    }
                                    inputTextFormRequired {
                                        form = InputTypes.PROJECT_NAME
                                        textValue = projectCreationRequest.name
                                        validInput =
                                                projectCreationRequest.name.isNotEmpty() && projectCreationRequest.validateProjectName() &&
                                                        projectCreationRequest.name.isValidLengthName() && conflictErrorMessage == null
                                        classes = "col-12 pl-2 pr-2 mt-3 text-left"
                                        name = "Tested tool name"
                                        conflictMessage = conflictErrorMessage
                                        onChangeFun = {
                                            setProjectCreationRequest(projectCreationRequest.copy(name = it.target.value))
                                            setConflictErrorMessage(null)
                                        }
                                    }
                                }

                                inputTextFormOptional {
                                    form = InputTypes.PROJECT_EMAIL
                                    textValue = projectCreationRequest.email
                                    classes = "col-12 pl-2 pr-2 mt-3 text-left"
                                    name = "Contact e-mail"
                                    validInput =
                                            projectCreationRequest.email.isEmpty() || projectCreationRequest.validateEmail()
                                    onChangeFun = {
                                        setProjectCreationRequest(projectCreationRequest.copy(email = it.target.value))
                                    }
                                }
                            }

                            div {
                                className = ClassName("col-12 mt-3 mb-3 pl-2 pr-2 text-left")
                                label {
                                    className = ClassName("form-label")
                                    asDynamic()["for"] = InputTypes.DESCRIPTION.name
                                    +"Description"
                                }
                                div {
                                    className = ClassName("input-group needs-validation")
                                    textarea {
                                        className = ClassName("form-control")
                                        onChange = {
                                            setProjectCreationRequest(projectCreationRequest.copy(description = it.target.value))
                                        }
                                        ariaDescribedBy = "${InputTypes.DESCRIPTION.name}Span"
                                        rows = 2
                                        id = InputTypes.DESCRIPTION.name
                                        required = false
                                    }
                                }
                            }

                            div {
                                className =
                                        ClassName("col-12 mt-3 mb-3 pl-2 pr-0 row d-flex alighn-items-center")
                                label {
                                    className = ClassName("text-xs")
                                    fontAwesomeIcon(icon = faQuestionCircle)
                                    asDynamic()["data-toggle"] = "tooltip"
                                    asDynamic()["data-placement"] = "top"
                                    title =
                                            "Private projects are visible for creator, it's organization admins and selected users, " +
                                                    "while public ones are visible for everyone."
                                }
                                div {
                                    className = ClassName("col-5 text-left align-self-center")
                                    +"Project visibility:"
                                }
                                form {
                                    className =
                                            ClassName("col-7 form-group row d-flex justify-content-around align-items-center mb-0")
                                    div {
                                        className = ClassName("form-check-inline")
                                        input {
                                            className = ClassName("form-check-input")
                                            defaultChecked = projectCreationRequest.isPublic
                                            name = "projectVisibility"
                                            type = InputType.radio
                                            id = "isProjectPublicSwitch"
                                            value = "true"
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
                                            defaultChecked = !projectCreationRequest.isPublic
                                            name = "projectVisibility"
                                            type = InputType.radio
                                            id = "isProjectPrivateSwitch"
                                            value = "false"
                                        }
                                        label {
                                            className = ClassName("form-check-label")
                                            htmlFor = "isProjectPrivateSwitch"
                                            +"Private"
                                        }
                                    }
                                    onChange = {
                                        setProjectCreationRequest(
                                            projectCreationRequest.copy(isPublic = (it.target as HTMLInputElement).value.toBoolean())
                                        )
                                    }
                                }
                            }
                            button {
                                type = ButtonType.button
                                className = ClassName("btn btn-info mt-4")
                                +"Create test project"
                                disabled =
                                        !projectCreationRequest.validate() || conflictErrorMessage != null
                                onClick = { saveProject() }
                            }

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

typealias SelectFormFunctionalComponent = FC<SelectFormRequiredProps<String>>
