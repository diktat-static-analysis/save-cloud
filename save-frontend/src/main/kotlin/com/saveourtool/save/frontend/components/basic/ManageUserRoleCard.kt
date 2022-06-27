/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.externals.lodash.debounce
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.permission.SetRoleRequest
import com.saveourtool.save.utils.getHighestRole

import csstype.None
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.*
import react.dom.*
import react.dom.onClick

import kotlinx.html.*
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.js.jso
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [RProps] for card component
 */
external interface ManageUserRoleCardProps : Props {
    /**
     * Information about user who is seeing the view
     */
    var selfUserInfo: UserInfo

    /**
     * Full name of a group
     */
    var groupPath: String

    /**
     * Kind of a group that will be shown ("project" or "organization" for now)
     */
    var groupType: String

    /**
     * Flag that shows if the confirm windows was shown or not
     */
    var wasConfirmationModalShown: Boolean

    /**
     * Lambda to show error after fail response
     */
    var updateErrorMessage: (Response) -> Unit

    /**
     * Lambda to get users from project/organization
     */
    var getUserGroups: (UserInfo) -> Map<String, Role>

    /**
     * Lambda to show warning if current user is super admin
     */
    var showGlobalRoleWarning: () -> Unit
}

/**
 * A functional `RComponent` for a card that shows users from the group and their permissions.
 *
 * @return a functional component representing a role managing card
 */
@Suppress(
    "LongMethod",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
    "TOO_LONG_FUNCTION",
    "MAGIC_NUMBER",
    "ComplexMethod",
)
fun manageUserRoleCardComponent() = fc<ManageUserRoleCardProps> { props ->
    val (changeUsersFromGroup, setChangeUsersFromGroup) = useState(true)
    val (usersFromGroup, setUsersFromGroup) = useState(emptyList<UserInfo>())
    val getUsersFromGroup = useRequest(dependencies = arrayOf(changeUsersFromGroup)) {
        val usersFromDb = get(
            url = "$apiUrl/${props.groupType}s/${props.groupPath}/users",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<UserInfo>>()
            }
        setUsersFromGroup(usersFromDb)
    }

    val (roleChange, setRoleChange) = useState(SetRoleRequest("", Role.NONE))
    val updatePermissions = useRequest(dependencies = arrayOf(roleChange)) {
        val headers = Headers().apply {
            set("Accept", "application/json")
            set("Content-Type", "application/json")
        }
        val response = post(
            "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles",
            headers,
            Json.encodeToString(roleChange),
            loadingHandler = ::noopLoadingHandler,
        )
        if (!response.ok) {
            props.updateErrorMessage(response)
        } else {
            getUsersFromGroup()
        }
    }

    val (userToAdd, setUserToAdd) = useState(UserInfo(""))
    val (usersNotFromGroup, setUsersNotFromGroup) = useState(emptyList<UserInfo>())
    val getUsersNotFromGroup = debounce(
        useRequest(dependencies = arrayOf(changeUsersFromGroup, userToAdd)) {
            val headers = Headers().apply {
                set("Accept", "application/json")
                set("Content-Type", "application/json")
            }
            val users = get(
                url = "$apiUrl/${props.groupType}s/${props.groupPath}/users/not-from?prefix=${userToAdd.name}",
                headers = headers,
                loadingHandler = ::noopLoadingHandler,
            )
                .unsafeMap {
                    it.decodeFromJsonString<List<UserInfo>>()
                }
            setUsersNotFromGroup(users)
        },
        500,
    )
    val addUserToGroup = useRequest {
        val headers = Headers().apply {
            set("Accept", "application/json")
            set("Content-Type", "application/json")
        }
        val response = post(
            url = "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles",
            headers = headers,
            body = Json.encodeToString(SetRoleRequest(userToAdd.name, Role.VIEWER)),
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            setUserToAdd(UserInfo(""))
            setChangeUsersFromGroup { !it }
            getUsersFromGroup()
            setUsersNotFromGroup(emptyList())
        } else {
            props.updateErrorMessage(response)
        }
    }

    val (userToDelete, setUserToDelete) = useState(UserInfo(""))
    val deleteUser = useRequest(dependencies = arrayOf(userToDelete)) {
        val headers = Headers().apply {
            set("Accept", "application/json")
            set("Content-Type", "application/json")
        }
        val response = delete(
            url = "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles/${userToDelete.name}",
            headers = headers,
            body = Json.encodeToString(userToDelete),
            loadingHandler = ::loadingHandler,
        )
        if (!response.ok) {
            props.updateErrorMessage(response)
        } else {
            setChangeUsersFromGroup { !it }
            getUsersFromGroup()
            setUsersNotFromGroup(emptyList())
        }
    }

    val (selfRole, setSelfRole) = useState(Role.NONE)
    useRequest(isDeferred = false) {
        val role = get(
            "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<String>()
            }
            .toRole()
        if (!props.wasConfirmationModalShown && role.priority < Role.OWNER.priority && props.selfUserInfo.globalRole == Role.SUPER_ADMIN) {
            props.showGlobalRoleWarning()
        }
        setSelfRole(getHighestRole(role, props.selfUserInfo.globalRole))
    }()

    val (isFirstRender, setIsFirstRender) = useState(true)
    if (isFirstRender) {
        getUsersFromGroup()
        setIsFirstRender(false)
    }

    div("card card-body mt-0 pt-0 pr-0 pl-0") {
        div("row mt-0 ml-0 mr-0") {
            div("input-group") {
                input(type = InputType.text, classes = "form-control") {
                    attrs.id = "input-users-to-add"
                    attrs.list = "complete-users-to-add"
                    attrs.placeholder = "username"
                    attrs.value = userToAdd.name
                    attrs.onChangeFunction = {
                        setUserToAdd(UserInfo((it.target as HTMLInputElement).value))
                        getUsersNotFromGroup()
                    }
                }
                datalist {
                    attrs.id = "complete-users-to-add"
                    attrs["style"] = jso<CSSProperties> {
                        appearance = None.none
                    }
                    for (user in usersNotFromGroup) {
                        option {
                            attrs.value = user.name
                            attrs.label = user.source ?: ""
                        }
                    }
                }
                div("input-group-append") {
                    button(type = ButtonType.button, classes = "btn btn-sm btn-outline-success") {
                        attrs.onClickFunction = {
                            addUserToGroup()
                        }
                        +"Add user"
                    }
                }
            }
        }
        for (user in usersFromGroup) {
            val userName = user.name
            val userRole = props.getUserGroups(user)[props.groupPath] ?: Role.VIEWER
            val userIndex = usersFromGroup.indexOf(user)
            div("row mt-2 mr-0 justify-content-between align-items-center") {
                div("col-7 d-flex justify-content-start align-items-center") {
                    div("col-2 align-items-center") {
                        fontAwesomeIcon(
                            when (user.source) {
                                "github" -> faGithub
                                "codehub" -> faCopyright
                                else -> faHome
                            },
                            classes = "h-75 w-75"
                        )
                    }
                    div("col-7 text-left align-self-center pl-0") {
                        +userName
                    }
                }
                div("col-5 align-self-right d-flex align-items-center justify-content-end") {
                    button(classes = "btn col-2 align-items-center mr-2") {
                        fontAwesomeIcon(icon = faTimesCircle)
                        val canDelete = selfRole == Role.SUPER_ADMIN ||
                                selfRole == Role.OWNER && !isSelfRecord(props.selfUserInfo, user) ||
                                userRole.isLowerThan(selfRole)
                        attrs.id = "remove-user-$userIndex"
                        attrs.hidden = !canDelete
                        attrs.onClick = {
                            val deletedUserIndex = attrs.id.split("-")[2].toInt()
                            setUserToDelete(usersFromGroup[deletedUserIndex])
                            deleteUser()
                        }
                    }
                    select("custom-select col-9") {
                        attrs.onChangeFunction = { event ->
                            val target = event.target as HTMLSelectElement
                            setRoleChange { SetRoleRequest(userName, target.value.toRole()) }
                            updatePermissions()
                        }
                        attrs.id = "role-$userIndex"
                        Role.values()
                            .filter { it != Role.NONE }
                            .filter { it != Role.SUPER_ADMIN }
                            .filter { selfRole == Role.OWNER || it.isLowerThan(selfRole) || userRole == it }
                            .map {
                                option {
                                    attrs.value = it.formattedName
                                    attrs.selected = it == userRole
                                    +it.formattedName
                                }
                            }
                        attrs.disabled = (selfRole == Role.OWNER && isSelfRecord(props.selfUserInfo, user)) || !(selfRole.isHigherOrEqualThan(Role.OWNER) || userRole.isLowerThan(selfRole))
                    }
                }
            }
        }
    }
}

private fun isSelfRecord(selfUserInfo: UserInfo, otherUserInfo: UserInfo) = otherUserInfo.name == selfUserInfo.name