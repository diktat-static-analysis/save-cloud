/**
 * This file contains function to create ManageGitCredentialsCard
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.GitDto
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.utils.getHighestRole
import com.saveourtool.frontend.common.components.modal.displayModal
import com.saveourtool.frontend.common.components.modal.mediumTransparentModalStyle
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import org.w3c.fetch.Response
import react.FC
import react.Props
import react.StateSetter
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.router.dom.Link
import react.useState
import web.cssom.ClassName
import web.cssom.rem
import web.html.ButtonType

typealias RequestWithDependency<R> = Triple<R, StateSetter<R>, () -> Unit>

/**
 * Props for ManageGitCredentialsCard
 */
external interface ManageGitCredentialsCardProps : Props {
    /**
     * Information about user who is seeing the view
     */
    var selfUserInfo: UserInfo

    /**
     * name of organization, assumption that it's checked by previous views and valid here
     */
    var organizationName: String

    /**
     * Lambda to show error after fail response
     */
    @Suppress("TYPE_ALIAS")
    var updateErrorMessage: (Response, String) -> Unit
}

/**
 * @return ManageGitCredentialsCard
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun manageGitCredentialsCardComponent() = FC<ManageGitCredentialsCardProps> { props ->
    val (selfRole, setSelfRole) = useState(Role.NONE)
    useRequest {
        val role = get(
            "$apiUrl/organizations/${props.organizationName}/users/roles",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<String>()
            }
            .toRole()
        setSelfRole(getHighestRole(role, props.selfUserInfo.globalRole))
    }

    val (gitCredentials, _, fetchGitCredentialsRequest) = prepareFetchGitCredentials(props.organizationName)

    val (isUpdate, setUpdateFlag) = useState(false)
    val gitCredentialToUpsertState = useState(GitDto.empty)
    val (_, setGitCredentialToUpsert) = gitCredentialToUpsertState

    val (gitCredentialToDelete, setGitCredentialToDelete, deleteGitCredentialRequest) =
            prepareDeleteGitCredential(props.organizationName, props.updateErrorMessage, fetchGitCredentialsRequest)

    val gitWindowOpenness = useWindowOpenness()
    gitWindow {
        windowOpenness = gitWindowOpenness
        organizationName = props.organizationName
        gitToUpsertState = gitCredentialToUpsertState
        this.isUpdate = isUpdate
        fetchGitCredentials = fetchGitCredentialsRequest
    }

    val (isConfirmDeleteGitCredentialWindowOpened, setConfirmDeleteGitCredentialWindowOpened) = useState(false)
    displayModal(
        isConfirmDeleteGitCredentialWindowOpened,
        "Deletion of git credential",
        "Please confirm deletion of git credential for ${gitCredentialToDelete.url}. " +
                "Note! This action will also delete all corresponding data to that repository, such as test suites sources, test executions and so on.",
        mediumTransparentModalStyle,
        { setConfirmDeleteGitCredentialWindowOpened(false) },
    ) {
        buttonBuilder("Ok") {
            deleteGitCredentialRequest()
            setConfirmDeleteGitCredentialWindowOpened(false)
        }
        buttonBuilder("Close", "secondary") {
            setConfirmDeleteGitCredentialWindowOpened(false)
        }
    }

    val canModify = selfRole.isSuperAdmin() || selfRole == Role.ADMIN
    div {
        className = ClassName("card card-body mt-0 pt-0 pr-0 pl-0")
        gitCredentials.forEachIndexed { index, gitCredential ->
            val url = gitCredential.url
            div {
                className = ClassName("row mt-2 mr-0 justify-content-between align-items-center")
                div {
                    className = ClassName("col-7 d-flex justify-content-start align-items-center")
                    div {
                        className = ClassName("col-2 align-items-center")
                        style = jso {
                            fontSize = 1.5.rem
                        }
                        fontAwesomeIcon(
                            when {
                                url.contains("github") -> faGithub
                                url.contains("codehub") -> faCopyright
                                else -> faHome
                            }
                        )
                    }
                    div {
                        className = ClassName("col-7 text-left align-self-center pl-0")
                        Link {
                            to = url
                            +url
                        }
                    }
                }
                div {
                    className = ClassName("col-5 align-self-right d-flex align-items-center justify-content-end")
                    button {
                        type = ButtonType.button
                        className = ClassName("btn col-2 align-items-center mr-2")
                        fontAwesomeIcon(icon = faEdit)
                        id = "edit-git-credential-$index"
                        onClick = {
                            setGitCredentialToUpsert(gitCredential)
                            setUpdateFlag(true)
                            gitWindowOpenness.openWindow()
                        }
                    }
                    button {
                        type = ButtonType.button
                        className = ClassName("btn col-2 align-items-center mr-2")
                        fontAwesomeIcon(icon = faTimesCircle)
                        id = "remove-git-credential-$index"
                        onClick = {
                            setGitCredentialToDelete(gitCredential)
                            setConfirmDeleteGitCredentialWindowOpened(true)
                        }
                    }
                    hidden = !canModify
                }
            }
        }
        @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
        hr {}
        div {
            className = ClassName("row d-flex justify-content-center")
            div {
                className = ClassName("col-11 text-right")
                button {
                    type = ButtonType.button
                    className = ClassName("btn btn-sm btn-outline-primary")
                    onClick = {
                        setGitCredentialToUpsert(GitDto.empty)
                        setUpdateFlag(false)
                        gitWindowOpenness.openWindow()
                    }
                    +"Add new"
                }
            }
        }
    }

    useOnce {
        fetchGitCredentialsRequest()
    }
}

@Suppress("TYPE_ALIAS")
private fun prepareFetchGitCredentials(organizationName: String): RequestWithDependency<List<GitDto>> {
    val (gitCredentials, setGitCredentials) = useState(emptyList<GitDto>())
    val fetchGitCredentialsRequest = useDeferredRequest {
        get(
            "$apiUrl/organizations/$organizationName/list-git",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<GitDto>>()
            }.let {
                setGitCredentials(it)
            }
    }
    return Triple(gitCredentials, setGitCredentials, fetchGitCredentialsRequest)
}

@Suppress("TYPE_ALIAS")
private fun prepareDeleteGitCredential(
    organizationName: String,
    @Suppress("TYPE_ALIAS")
    updateErrorMessage: (Response, String) -> Unit,
    fetchGitCredentialsRequest: () -> Unit
): RequestWithDependency<GitDto> {
    val (gitCredentialToDelete, setGitCredentialToDelete) = useState(GitDto("N/A"))
    val deleteGitCredentialRequest = useDeferredRequest {
        val response = delete(
            url = "$apiUrl/organizations/$organizationName/delete-git?url=${gitCredentialToDelete.url}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
        if (!response.ok) {
            updateErrorMessage(response, response.unpackMessage())
        } else {
            fetchGitCredentialsRequest()
        }
    }
    return Triple(gitCredentialToDelete, setGitCredentialToDelete, deleteGitCredentialRequest)
}
