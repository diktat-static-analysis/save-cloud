/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.frontend.common.components.basic

import com.saveourtool.common.domain.Role
import com.saveourtool.common.domain.Role.OWNER
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.permission.SetRoleRequest
import com.saveourtool.common.utils.getHighestRole
import com.saveourtool.frontend.common.components.inputform.inputWithDebounceForUserInfo
import com.saveourtool.frontend.common.components.inputform.renderUserWithAvatar
import com.saveourtool.frontend.common.externals.fontawesome.faTimesCircle
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import web.cssom.ClassName
import web.cssom.Height
import web.cssom.Width

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A functional `Component` for a card that shows users from the group and their permissions.
 *
 * @return a functional component representing a role managing card
 */
@Suppress(
    "LongMethod",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
    "TOO_LONG_FUNCTION",
    "MAGIC_NUMBER",
)
val manageUserRoleCardComponent: FC<ManageUserRoleCardProps> = FC { props ->
    val (usersFromGroup, setUsersFromGroup) = useState(emptyList<UserInfo>())
    val getUsersFromGroup = useDeferredRequest {
        val usersFromDb = get(
            url = "$apiUrl/${props.groupType}s/${props.groupPath}/users",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<UserInfo>>()
            }
        setUsersFromGroup(usersFromDb)
    }

    val (roleChange, setRoleChange) = useState(SetRoleRequest("", Role.NONE))
    val updatePermissions = useDeferredRequest {
        val response = post(
            "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles",
            jsonHeaders,
            Json.encodeToString(roleChange),
            loadingHandler = ::noopLoadingHandler,
        )
        if (response.ok) {
            getUsersFromGroup()
        }
    }

    val (userToAdd, setUserToAdd) = useState(UserInfo(name = ""))
    val addUserToGroup = useDeferredRequest {
        val response = post(
            url = "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles",
            headers = jsonHeaders,
            body = Json.encodeToString(SetRoleRequest(userToAdd.name, Role.VIEWER)),
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            setUserToAdd(UserInfo(name = ""))
            getUsersFromGroup()
        }
    }

    val (userToDelete, setUserToDelete) = useState(UserInfo(name = ""))
    val deleteUser = useDeferredRequest {
        val response = delete(
            url = "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles/${userToDelete.name}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            getUsersFromGroup()
        }
    }

    val (selfRole, setSelfRole) = useState(Role.NONE)
    useRequest {
        val role = get(
            "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap { it.decodeFromJsonString<String>() }
            .toRole()
        if (role.isLowerThan(OWNER) && props.selfUserInfo.isSuperAdmin()) {
            showGlobalRoleConfirmation()
        }
        setSelfRole(getHighestRole(role, props.selfUserInfo.globalRole))
    }

    useOnce { getUsersFromGroup() }

    div {
        className = ClassName("card border card-body mt-0")
        div {
            className = ClassName("row mb-2 mx-2 shadow-sm rounded")
            inputWithDebounceForUserInfo {
                selectedOption = userToAdd
                setSelectedOption = { setUserToAdd(it) }
                onOptionClick = {
                    setUserToAdd(it)
                    addUserToGroup()
                }
                getUrlForOptionsFetch = { prefix -> "$apiUrl/${props.groupType}s/${props.groupPath}/users/not-from?prefix=$prefix" }
                placeholder = "Input name..."
                renderOption = ::renderUserWithAvatar
            }
        }
        div {
            for (user in usersFromGroup) {
                val userName = user.name
                val userRole = props.getUserGroups(user)[props.groupPath] ?: Role.VIEWER
                val userIndex = usersFromGroup.indexOf(user)
                div {
                    className = ClassName("row mt-2 mr-0 justify-content-between align-items-center")
                    div {
                        className = ClassName("col-7 d-flex justify-content-start align-items-center")
                        div {
                            className = ClassName("col-2 align-items-center")
                            img {
                                className = ClassName("avatar avatar-user border color-bg-default rounded-circle pl-0")
                                src = user.avatar?.let(String::avatarRenderer)
                                    ?: "/img/undraw_profile.svg"
                                style = jso {
                                    width = "2rem".unsafeCast<Width>()
                                    height = "2rem".unsafeCast<Height>()
                                }
                            }
                        }
                        div {
                            className = ClassName("col-7 text-left align-self-center pl-0")
                            +userName
                        }
                    }
                    div {
                        className = ClassName("col-5 align-self-right d-flex align-items-center justify-content-end pr-0")
                        select {
                            className = ClassName("custom-select col-9")
                            onChange = { event ->
                                setRoleChange { SetRoleRequest(userName, event.target.value.toRole()) }
                                updatePermissions()
                            }
                            value = userRole.formattedName
                            id = "role-$userIndex"
                            rolesAssignableBy(selfRole)
                                .sortedByDescending {
                                    it.priority
                                }
                                .map {
                                    option {
                                        value = it.formattedName
                                        +it.formattedName
                                    }
                                }
                            disabled = (selfRole == OWNER && isSelfRecord(props.selfUserInfo, user)) ||
                                    !(selfRole.isHigherOrEqualThan(OWNER) || userRole.isLowerThan(selfRole))
                        }
                        div {
                            className = ClassName("col-2 align-items-center mr-2")
                            val canDelete = selfRole.isSuperAdmin() ||
                                    selfRole == OWNER && !isSelfRecord(props.selfUserInfo, user) ||
                                    userRole.isLowerThan(selfRole)
                            if (canDelete) {
                                buttonBuilder(faTimesCircle, "") {
                                    setUserToDelete(usersFromGroup[userIndex])
                                    deleteUser()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * [Props] for card component
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
     * Lambda to get users from project/organization
     */
    var getUserGroups: (UserInfo) -> Map<String, Role>
}

private fun isSelfRecord(selfUserInfo: UserInfo, otherUserInfo: UserInfo) = otherUserInfo.name == selfUserInfo.name

private fun rolesAssignableBy(role: Role) = Role.values()
    .filter { it != Role.NONE }
    .filterNot(Role::isSuperAdmin)
    .filter { role == OWNER || it.isLowerThan(role) || role == it }
