@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.common.entities.ProjectProblemCritical
import com.saveourtool.common.entities.ProjectProblemDto
import com.saveourtool.common.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.common.utils.isNotNull
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.inputform.inputTextFormOptional
import com.saveourtool.frontend.common.components.inputform.inputTextFormRequired
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.FC
import react.Props
import react.dom.aria.ariaDescribedBy
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.textarea
import react.router.useNavigate
import react.useState
import web.cssom.ClassName

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val createProjectProblem: FC<CreateProjectProblemViewProps> = FC {props ->
    useBackground(Style.SAVE_DARK)

    val navigate = useNavigate()

    val (projectProblem, setProjectProblem) = useState(ProjectProblemDto.empty)
    val (conflictErrorMessage, setConflictErrorMessage) = useState<String?>(null)

    val enrollRequest = useDeferredRequest {
        val projectProblemNew = projectProblem.copy(projectName = props.projectName, organizationName = props.organizationName)
        val response = post(
            url = "$apiUrl/projects/problem/save",
            headers = jsonHeaders,
            body = Json.encodeToString(projectProblemNew),
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            navigate(to = "/project/${props.organizationName}/${props.projectName}/security")
        }
    }

    val enrollCheckVulnerabilityRequest = useDeferredRequest {
        val response = get(
            url = "$apiUrl/vulnerabilities/by-identifier-and-status",
            params = jso<dynamic> {
                name = projectProblem.identifier
                status = VulnerabilityStatus.APPROVED
            },
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
        if (!response.ok) {
            setConflictErrorMessage("No vulnerability found with ${projectProblem.identifier} CVE identifier")
        } else {
            setConflictErrorMessage(null)
            enrollRequest()
        }
    }

    @Suppress("MAGIC_NUMBER")
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
                            +"Open a new problem"
                        }
                        form {
                            className = ClassName("needs-validation")
                            div {
                                className = ClassName("row-3")

                                inputTextFormRequired {
                                    form = InputTypes.PROJECT_PROBLEM_NAME
                                    conflictMessage = "Name must not be empty"
                                    textValue = projectProblem.name
                                    validInput = projectProblem.name.isNotBlank()
                                    classes = "col-12 pl-2 pr-2 mt-3 text-left"
                                    name = "Name:"
                                    onChangeFun = { event ->
                                        setProjectProblem { it.copy(name = event.target.value) }
                                    }
                                }

                                div {
                                    className = ClassName("col-12 mt-3 mb-3 pl-2 pr-2 text-left")
                                    label {
                                        className = ClassName("form-label")
                                        +"Description"
                                        span {
                                            className = ClassName("text-danger text-left")
                                            +"*"
                                        }
                                    }
                                    div {
                                        className = ClassName("input-group needs-validation")
                                        textarea {
                                            className = ClassName("form-control")
                                            onChange = { event ->
                                                setProjectProblem { it.copy(description = event.target.value) }
                                            }
                                            ariaDescribedBy = "${InputTypes.DESCRIPTION.name}Span"
                                            rows = 3
                                            id = InputTypes.DESCRIPTION.name
                                            required = true
                                        }
                                    }
                                }

                                div {
                                    className = ClassName("col-12 mt-3 mb-3 pl-2 pr-2 text-left")

                                    label {
                                        className = ClassName("form-label")
                                        +"Critical"
                                        span {
                                            className = ClassName("text-danger text-left")
                                            +"*"
                                        }
                                    }

                                    div {
                                        className = ClassName("input-group-sm input-group")
                                        select {
                                            className = ClassName("form-control")
                                            ProjectProblemCritical.values().map { it.toString() }.forEach {
                                                option {
                                                    className = ClassName("list-group-item")
                                                    val entries = it
                                                    value = entries
                                                    +entries
                                                }
                                            }
                                            onChange = { event ->
                                                val entries = event.target.value
                                                setProjectProblem { problem -> problem.copy(critical = ProjectProblemCritical.valueOf(entries.uppercase())) }
                                            }
                                        }
                                    }
                                }

                                inputTextFormOptional {
                                    form = InputTypes.CVE_NAME
                                    textValue = projectProblem.identifier
                                    validInput = conflictErrorMessage.isNullOrEmpty()
                                    classes = "col-12 pl-2 pr-2 mt-3 text-left"
                                    name = "CVE identifier"
                                    onChangeFun = { event ->
                                        setConflictErrorMessage(null)
                                        setProjectProblem { problem -> problem.copy(identifier = event.target.value) }
                                    }
                                }
                            }
                        }

                        buttonBuilder(
                            "Create a problem",
                            style = "info",
                            classes = "mt-4",
                            isDisabled = projectProblem.name.isEmpty() || projectProblem.description.isEmpty() || conflictErrorMessage.isNotNull()
                        ) {
                            if (projectProblem.identifier.isNullOrEmpty()) {
                                enrollRequest()
                            } else {
                                enrollCheckVulnerabilityRequest()
                            }
                        }

                        conflictErrorMessage?.let {
                            div {
                                className = ClassName("invalid-feedback d-block text-center")
                                +it
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * [Props] for CreateProjectProblemView
 */
external interface CreateProjectProblemViewProps : Props {
    /**
     * Name of security vulnerabilities
     */
    var organizationName: String

    /**
     * Information about current user
     */
    var projectName: String
}
