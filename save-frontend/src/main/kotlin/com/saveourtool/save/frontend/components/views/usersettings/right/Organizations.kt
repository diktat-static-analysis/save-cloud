/**
 * rendering for Organization management card
 */

package com.saveourtool.save.frontend.components.views.usersettings.right

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.entities.OrganizationWithUsers
import com.saveourtool.save.filters.OrganizationFilter
import com.saveourtool.save.frontend.components.basic.AVATAR_ORGANIZATION_PLACEHOLDER
import com.saveourtool.save.frontend.components.basic.organizations.responseChangeOrganizationStatus
import com.saveourtool.save.frontend.components.views.actionButtonClasses
import com.saveourtool.save.frontend.components.views.actionIconClasses
import com.saveourtool.save.frontend.components.views.usersettings.SettingsProps
import com.saveourtool.save.frontend.externals.fontawesome.faRedo
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.v1

import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.useState
import web.cssom.ClassName

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.StateSetter
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.router.dom.Link

val organizationsSettingsCard: FC<SettingsProps> = FC { props ->


    val (organizations, setOrganizations) = useState<List<OrganizationWithUsers>>(emptyList())

    val getOrganizationsForUser = useDeferredRequest {
        val organizationDtos = post(
            url = "$apiUrl/organizations/by-filters",
            headers = jsonHeaders,
            body = Json.encodeToString(OrganizationFilter.all),
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap { it.decodeFromJsonString<List<OrganizationWithUsers>>() }

        setOrganizations(organizationDtos)
    }

    useOnce { getOrganizationsForUser() }

    div {
        className = ClassName("d-sm-flex align-items-center justify-content-center mb-4 mt-4")
        h1 {
            className = ClassName("h3 mb-0 mt-2 text-gray-800")
            +"Organizations"
        }
    }

    ul {
        className = ClassName("list-group list-group-flush")
        organizations.forEach { organizationWithUsers ->
            val organizationDto = organizationWithUsers.organization
            li {
                className = ClassName("list-group-item")
                div {
                    className = ClassName("row justify-content-between align-items-center")
                    div {
                        val textClassName = when (organizationDto.status) {
                            OrganizationStatus.CREATED -> "text-primary"
                            OrganizationStatus.DELETED -> "text-secondary"
                            OrganizationStatus.BANNED -> "text-danger"
                        }
                        className = ClassName("align-items-center ml-3 $textClassName")
                        img {
                            className =
                                ClassName("avatar avatar-user width-full border color-bg-default rounded-circle mr-2")
                            src = organizationDto.avatar?.let {
                                "/api/$v1/avatar$it"
                            } ?: AVATAR_ORGANIZATION_PLACEHOLDER
                            height = 60.0
                            width = 60.0
                        }
                        when (organizationDto.status) {
                            OrganizationStatus.CREATED -> Link {
                                to = "/${organizationDto.name}"
                                +organizationDto.name
                            }

                            OrganizationStatus.DELETED -> {
                                +organizationDto.name
                                spanWithClassesAndText("text-secondary", organizationDto.status.name.lowercase())
                            }

                            OrganizationStatus.BANNED -> {
                                +organizationDto.name
                                spanWithClassesAndText("text-danger", organizationDto.status.name.lowercase())
                            }
                        }
                    }
                    div {
                        className = ClassName("col-5 text-right")
                        val role = props.userInfo?.name?.let { organizationWithUsers.userRoles[it] } ?: Role.NONE
                        if (role.isHigherOrEqualThan(Role.OWNER)) {
                            when (organizationDto.status) {
                                OrganizationStatus.CREATED -> actionButton {
                                    title = "WARNING: You are about to delete this organization"
                                    errorTitle = "You cannot delete the organization ${organizationDto.name}"
                                    message =
                                        "Are you sure you want to delete the organization ${organizationDto.name}?"
                                    buttonStyleBuilder = { childrenBuilder ->
                                        with(childrenBuilder) {
                                            fontAwesomeIcon(
                                                icon = faTrashAlt,
                                                classes = actionIconClasses.joinToString(" ")
                                            )
                                        }
                                    }
                                    classes = actionButtonClasses.joinToString(" ")
                                    modalButtons = { action, closeWindow, childrenBuilder, _ ->
                                        with(childrenBuilder) {
                                            buttonBuilder(
                                                label = "Yes, delete ${organizationDto.name}",
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
                                    onActionSuccess = { _ ->
                                        updateOrganizationWithUserInOrganizationWithUsersList(
                                            organizationWithUsers,
                                            changeOrganizationWithUserStatus(
                                                organizationWithUsers,
                                                OrganizationStatus.DELETED
                                            ),
                                            organizations,
                                            setOrganizations,
                                        )
                                    }
                                    conditionClick = false
                                    sendRequest = { _ ->
                                        responseChangeOrganizationStatus(
                                            organizationDto.name,
                                            OrganizationStatus.DELETED
                                        )
                                    }
                                }

                                OrganizationStatus.DELETED -> actionButton {
                                    title = "WARNING: You are about to recover this organization"
                                    errorTitle = "You cannot recover the organization ${organizationDto.name}"
                                    message =
                                        "Are you sure you want to recover the organization ${organizationDto.name}?"
                                    buttonStyleBuilder = { childrenBuilder ->
                                        with(childrenBuilder) {
                                            fontAwesomeIcon(
                                                icon = faRedo,
                                                classes = actionIconClasses.joinToString(" ")
                                            )
                                        }
                                    }
                                    classes = actionButtonClasses.joinToString(" ")
                                    modalButtons = { action, closeWindow, childrenBuilder, _ ->
                                        with(childrenBuilder) {
                                            buttonBuilder(
                                                label = "Yes, recover ${organizationDto.name}",
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
                                    onActionSuccess = { _ ->
                                        updateOrganizationWithUserInOrganizationWithUsersList(
                                            organizationWithUsers,
                                            changeOrganizationWithUserStatus(
                                                organizationWithUsers,
                                                OrganizationStatus.CREATED
                                            ),
                                            organizations,
                                            setOrganizations,
                                        )
                                    }
                                    conditionClick = false
                                    sendRequest = { _ ->
                                        responseChangeOrganizationStatus(
                                            organizationDto.name,
                                            OrganizationStatus.CREATED
                                        )
                                    }
                                }

                                OrganizationStatus.BANNED -> Unit
                            }
                        }
                        div {
                            className = ClassName("mr-3")
                            +role.formattedName
                        }
                    }
                }
            }
        }
    }
}

/**
 * Removes [oldOrganizationWithUsers] by [selfOrganizationWithUserList], adds [newOrganizationWithUsers] in [selfOrganizationWithUserList]
 * and sorts the resulting list by their status and then by name
 *
 * @param organizationWithUsers
 * @param newStatus
 */
private fun updateOrganizationWithUserInOrganizationWithUsersList(
    oldOrganizationWithUsers: OrganizationWithUsers,
    newOrganizationWithUsers: OrganizationWithUsers,
    organizations: List<OrganizationWithUsers>,
    setOrganizations: StateSetter<List<OrganizationWithUsers>>
) = setOrganizations(
    organizations.minusElement(oldOrganizationWithUsers)
        .plusElement(newOrganizationWithUsers)
        .sortedWith(comparator)
)


private val comparator: Comparator<OrganizationWithUsers> =
    compareBy<OrganizationWithUsers> { it.organization.status.ordinal }
        .thenBy { it.organization.name }

/**
 * Returned the [organizationWithUsers] with the updated [OrganizationStatus] field to the [newStatus] in the organization field
 */
private fun changeOrganizationWithUserStatus(organizationWithUsers: OrganizationWithUsers, newStatus: OrganizationStatus) =
    organizationWithUsers.copy(organization = organizationWithUsers.organization.copy(status = newStatus))
