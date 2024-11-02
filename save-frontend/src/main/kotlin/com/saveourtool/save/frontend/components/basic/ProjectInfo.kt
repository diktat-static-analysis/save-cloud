/**
 * Function component for project info and edit support
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.frontend.common.externals.fontawesome.faCheck
import com.saveourtool.frontend.common.externals.fontawesome.faEdit
import com.saveourtool.frontend.common.externals.fontawesome.faTimesCircle
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.*

import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.useEffect
import react.useRef
import react.useState
import web.cssom.ClassName
import web.html.ButtonType
import web.html.InputType

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val projectInfo = projectInfo()

private val projectInformationHeaders = mapOf(
    "name" to "Tested tool name: ",
    "description" to "Description: ",
    "url" to "Tested tool Url: ",
)

/**
 * ProjectInfo component props
 */
external interface ProjectInfoProps : Props {
    /**
     * Project passed from parent component that should be used for initial values
     */
    var project: ProjectDto

    /**
     * Callback to update project state in ProjectView after update request's response is received.
     */
    var onProjectUpdate: ((ProjectDto) -> Unit)?
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "KDOC_WITHOUT_RETURN_TAG",
    "LongMethod",
    "TYPE_ALIAS"
)
private fun projectInfo() = FC<ProjectInfoProps> { props ->
    val projectRef = useRef(props.project)
    val (draftProject, setDraftProject) = useState(props.project)
    useEffect(props.project) {
        if (projectRef.current !== props.project) {
            setDraftProject(props.project)
            projectRef.current = props.project
        }
    }

    val updateProject = useDeferredRequest {
        props.onProjectUpdate?.let { onProjectUpdate ->
            post(
                "$apiUrl/projects/update",
                jsonHeaders,
                Json.encodeToString(draftProject),
                loadingHandler = ::loadingHandler,
            ).let {
                if (it.ok) {
                    onProjectUpdate(draftProject)
                }
            }
        }
    }

    val idToValue = mapOf(
        "name" to draftProject.name,
        "url" to draftProject.url,
        "description" to draftProject.description,
    )
    val idToValueSetter: Map<String, (String) -> ProjectDto> = mapOf(
        "name" to { draftProject.copy(name = it) },
        "url" to { draftProject.copy(url = it) },
        "description" to { draftProject.copy(description = it) },
    )
    val (isEditDisabled, setIsEditDisabled) = useState(true)
    props.onProjectUpdate?.let {
        div {
            className = ClassName("d-flex justify-content-center")
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link text-xs text-muted text-left p-1 ml-2")
                +"Edit  "
                fontAwesomeIcon(icon = faEdit)
                onClick = {
                    setIsEditDisabled { !it }
                }
            }
        }
    }
    form {
        div {
            className = ClassName("row g-3 ml-3 mr-3 pb-2 pt-2  border-bottom")
            idToValue.forEach { (fieldId, text) ->
                div {
                    className = ClassName("col-6 pl-0 pr-0")
                    label {
                        className = ClassName("control-label col-auto justify-content-between pl-0")
                        +projectInformationHeaders.getValue(fieldId)
                    }
                }
                div {
                    className = ClassName("col-6 pl-0")
                    div {
                        className = ClassName("controls col-auto pl-0")
                        input {
                            className = ClassName("form-control-plaintext pt-0 pb-0")
                            type = InputType.text
                            this.id = fieldId
                            value = text
                            // temporary workaround for https://github.com/saveourtool/save-cloud/issues/589#issuecomment-1049674021
                            disabled = fieldId != "name" && props.onProjectUpdate != null && isEditDisabled
                            readOnly = fieldId == "name" || props.onProjectUpdate == null
                            onChange = { event ->
                                setDraftProject(idToValueSetter.getValue(fieldId)(event.target.value))
                            }
                        }
                    }
                }
            }
        }

        div {
            className = ClassName("ml-3 mt-2 align-items-right float-right")
            button {
                className = ClassName("btn")
                type = ButtonType.button
                fontAwesomeIcon(icon = faCheck)
                hidden = isEditDisabled
                onClick = {
                    updateProject()
                    setIsEditDisabled(true)
                }
            }

            button {
                className = ClassName("btn")
                type = ButtonType.button
                fontAwesomeIcon(icon = faTimesCircle)
                hidden = isEditDisabled
                onClick = {
                    setDraftProject(props.project)
                    setIsEditDisabled(true)
                }
            }
        }
    }
}
