@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.common.entities.ProjectProblemCritical
import com.saveourtool.common.entities.ProjectProblemDto
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.inputform.inputTextFormOptional
import com.saveourtool.frontend.common.components.inputform.inputTextFormRequired
import com.saveourtool.frontend.common.components.modal.modal
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.FC
import react.Props
import react.dom.aria.ariaDescribedBy
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.textarea
import react.useState
import web.cssom.ClassName

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Component that allows to edit project problem
 */
val editProjectProblemWindow: FC<EditProjectProblemWindowProps> = FC { props ->
    val (projectProblem, setProjectProblem) = useStateFromProps(props.problem)
    val (conflictErrorMessage, setConflictErrorMessage) = useState<String?>(null)

    val enrollRequest = useDeferredRequest {
        val response = post(
            url = "$apiUrl/projects/problem/update",
            headers = jsonHeaders,
            body = Json.encodeToString(projectProblem),
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            props.windowOpenness.closeWindow()
            window.location.reload()
        }
    }

    val enrollCheckVulnerabilityRequest = useDeferredRequest {
        val response = get(
            url = "$apiUrl/vulnerabilities/by-identifier",
            params = jso<dynamic> {
                identifier = projectProblem.identifier
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

    modal { modalProps ->
        modalProps.isOpen = props.windowOpenness.isOpen()

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
                    value = projectProblem.description
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
                    value = projectProblem.critical
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

        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mt-4")

            buttonBuilder(label = "Save", classes = "mr-2") {
                if (projectProblem.identifier.isNullOrEmpty()) {
                    enrollRequest()
                } else {
                    enrollCheckVulnerabilityRequest()
                }
            }

            buttonBuilder(label = "Cancel", isOutline = true, classes = "mr-2") {
                props.windowOpenness.closeWindow()
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

/**
 * EditProjectProblemWindow component props
 */
external interface EditProjectProblemWindowProps : Props {
    /**
     * Window openness
     */
    var windowOpenness: WindowOpenness

    /**
     * Project problem
     */
    var problem: ProjectProblemDto
}
