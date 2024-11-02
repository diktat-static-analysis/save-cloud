/**
 * Function component for project info and edit support
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.entities.GitDto
import com.saveourtool.frontend.common.components.modal.displayModal
import com.saveourtool.frontend.common.components.modal.modal
import com.saveourtool.frontend.common.components.modal.smallTransparentModalStyle
import com.saveourtool.frontend.common.utils.*

import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import web.cssom.ClassName
import web.html.ButtonType
import web.html.InputType

/**
 * Component that allows to change git settings in ManageGitCredentialsCard.kt
 */
val gitWindow = createGitWindow()

/**
 * GitWindow component props
 */
external interface GitWindowProps : Props {
    /**
     * Window openness
     */
    var windowOpenness: WindowOpenness

    /**
     * name of organization, assumption that it's checked by previous views and valid here
     */
    var organizationName: String

    /**
     * Git credential to upsert
     */
    var gitToUpsertState: StateInstance<GitDto>

    /**
     * Flag controls that current window is for update
     */
    var isUpdate: Boolean

    /**
     * Request to fetch git credentials
     */
    var fetchGitCredentials: () -> Unit
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "TYPE_ALIAS",
)
private fun createGitWindow() = FC<GitWindowProps> { props ->
    val (gitToUpsert, setGitToUpsert) = props.gitToUpsertState

    val failedResponseWindowOpenness = useWindowOpenness()
    val failedResponseWindowCloseAction = {
        failedResponseWindowOpenness.closeWindow()
        props.windowOpenness.openWindow()
    }
    val (failedReason, setFailedReason) = useState("N/A")
    displayModal(
        failedResponseWindowOpenness.isOpen(),
        "Failed to ${if (props.isUpdate) "update" else "create"} git credential",
        "Url [${gitToUpsert.url}]: $failedReason",
        smallTransparentModalStyle,
        failedResponseWindowCloseAction
    ) {
        buttonBuilder(
            label = "Ok",
            style = "secondary",
            onClickFun = failedResponseWindowCloseAction.withUnusedArg()
        )
    }

    val upsertGitCredentialRequest = useDeferredRequest {
        val endpointPrefix = if (props.isUpdate) {
            "update"
        } else {
            "create"
        }
        val response = post(
            "$apiUrl/organizations/${props.organizationName}/$endpointPrefix-git",
            headers = jsonHeaders,
            body = gitToUpsert.toJsonBody(),
            loadingHandler = ::loadingHandler,
            responseHandler = ::responseHandlerWithValidation
        )
        props.windowOpenness.closeWindow()
        if (!response.ok) {
            setFailedReason(response.decodeFieldFromJsonString("message"))
            failedResponseWindowOpenness.openWindow()
        } else if (!props.isUpdate) {
            props.fetchGitCredentials()
        }
    }

    modal { modalProps ->
        modalProps.isOpen = props.windowOpenness.isOpen()

        div {
            className = ClassName("row mt-2 ml-2 mr-2")
            div {
                className = ClassName("col-5 text-left align-self-center")
                +"Git Url:"
            }
            div {
                className = ClassName("col-7 input-group pl-0")
                input {
                    type = InputType.text
                    className = ClassName("form-control")
                    defaultValue = gitToUpsert.url
                    readOnly = props.isUpdate
                    required = true
                    onChange = {
                        setGitToUpsert(gitToUpsert.copy(url = it.target.value))
                    }
                    if (props.isUpdate) {
                        asDynamic()["data-toggle"] = "tooltip"
                        asDynamic()["data-placement"] = "bottom"
                        title = "Cannot be changed on update"
                    }
                }
            }
        }
        div {
            className = ClassName("row mt-2 ml-2 mr-2")
            div {
                className = ClassName("col-5 text-left align-self-center")
                +"Git Username:"
            }
            div {
                className = ClassName("col-7 input-group pl-0")
                input {
                    type = InputType.text
                    className = ClassName("form-control")
                    defaultValue = gitToUpsert.username
                    onChange = {
                        setGitToUpsert(gitToUpsert.copy(username = it.target.value))
                    }
                }
            }
        }
        div {
            className = ClassName("row mt-2 ml-2 mr-2")
            div {
                className = ClassName("col-5 text-left align-self-center")
                +"Git Token:"
            }
            div {
                className = ClassName("col-7 input-group pl-0")
                input {
                    type = InputType.text
                    className = ClassName("form-control")
                    onChange = {
                        setGitToUpsert(gitToUpsert.copy(password = it.target.value))
                    }
                }
            }
        }
        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mt-4")
            button {
                type = ButtonType.button
                className = ClassName("btn btn-outline-primary mr-3")
                onClick = {
                    upsertGitCredentialRequest()
                }
                val buttonName = if (props.isUpdate) {
                    "Update"
                } else {
                    "Create"
                }
                +buttonName
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-outline-primary")
                onClick = props.windowOpenness.closeWindowAction().withUnusedArg()
                +"Cancel"
            }
        }
    }

    useTooltip()
}
