/**
 * rendering for Organization management card
 */

package com.saveourtool.frontend.common.components.views.usersettings.right

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.OrganizationStatus
import com.saveourtool.common.entities.OrganizationWithUsers
import com.saveourtool.common.filters.OrganizationFilter
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.basic.AVATAR_ORGANIZATION_PLACEHOLDER
import com.saveourtool.frontend.common.components.basic.avatarRenderer
import com.saveourtool.frontend.common.components.views.usersettings.SettingsProps
import com.saveourtool.frontend.common.externals.fontawesome.faRedo
import com.saveourtool.frontend.common.externals.fontawesome.faTrashAlt
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import org.w3c.fetch.Response
import react.ChildrenBuilder
import react.FC
import react.StateSetter
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.router.dom.Link
import react.useState
import web.cssom.ClassName
import web.cssom.rem

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * CSS classes of the "delete project" button.
 */
val actionButtonClasses: List<String> = listOf("btn", "btn-small")

/**
 * CSS classes of the "delete project" icon.
 */
val actionIconClasses: List<String> = listOf("trash-alt")

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
        className = ClassName("row justify-content-center")
        div {
            className = ClassName("col-8 px-5")
            div {
                className = ClassName("d-sm-flex align-items-center justify-content-center mt-4")
                h3 {
                    className = ClassName("mb-0 mt-2 text-gray-800")
                    +"Organizations"
                }
            }

            div {
                className = ClassName("d-sm-flex align-items-center justify-content-center mt-1")
                Link {
                    to = "/${FrontendRoutes.CREATE_ORGANIZATION}"
                    buttonBuilder(
                        "Create new Organization",
                        style = "outline-primary rounded-pill btn-sm",
                        isOutline = false
                    ) {
                        }
                }
            }

            if (organizations.isEmpty()) {
                div {
                    className = ClassName("d-sm-flex align-items-center justify-content-center mt-5")
                    h5 {
                        className = ClassName("mt-2 text-gray-800")
                        +"You are not added to any organization"
                    }
                }
                div {
                    className = ClassName("d-sm-flex align-items-center justify-content-center mt-1")
                    img {
                        src = "/img/sad_cat.png"
                        @Suppress("MAGIC_NUMBER")
                        style = jso {
                            width = 14.rem
                        }
                    }
                }
            } else {
                renderOrganizations(organizations, setOrganizations, props)
            }
        }
    }
}

private val comparator: Comparator<OrganizationWithUsers> =
        compareBy<OrganizationWithUsers> { it.organization.status.ordinal }
            .thenBy { it.organization.name }

typealias OrganizationSetter = StateSetter<List<OrganizationWithUsers>>

@Suppress("TOO_LONG_FUNCTION", "CyclomaticComplexMethod", "LongMethod")
private fun ChildrenBuilder.renderOrganizations(
    organizations: List<OrganizationWithUsers>,
    setOrganizations: OrganizationSetter,
    props: SettingsProps
) {
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
                            src = organizationDto.avatar?.avatarRenderer() ?: AVATAR_ORGANIZATION_PLACEHOLDER
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
                                spanWithClassesAndText(
                                    "text-secondary",
                                    organizationDto.status.name.lowercase()
                                )
                            }

                            OrganizationStatus.BANNED -> {
                                +organizationDto.name
                                spanWithClassesAndText("text-danger", organizationDto.status.name.lowercase())
                            }
                        }
                    }
                    div {
                        className = ClassName("col-5 text-right")
                        val role =
                                props.userInfo?.name?.let { organizationWithUsers.userRoles[it] } ?: Role.NONE
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
 * Makes a call to change project status
 *
 * @param organizationName name of the organization whose status will be changed
 * @param status is new status
 * @return lazy response
 */
fun responseChangeOrganizationStatus(organizationName: String, status: OrganizationStatus): suspend WithRequestStatusContext.() -> Response = {
    post(
        url = "$apiUrl/organizations/$organizationName/change-status?status=$status",
        headers = jsonHeaders,
        body = undefined,
        loadingHandler = ::noopLoadingHandler,
        responseHandler = ::noopResponseHandler,
    )
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
    setOrganizations: OrganizationSetter
) = setOrganizations(
    organizations.minusElement(oldOrganizationWithUsers)
        .plusElement(newOrganizationWithUsers)
        .sortedWith(comparator)
)

/**
 * Returned the [organizationWithUsers] with the updated [OrganizationStatus] field to the [newStatus] in the organization field
 */
private fun changeOrganizationWithUserStatus(
    organizationWithUsers: OrganizationWithUsers,
    newStatus: OrganizationStatus
) =
        organizationWithUsers.copy(organization = organizationWithUsers.organization.copy(status = newStatus))
