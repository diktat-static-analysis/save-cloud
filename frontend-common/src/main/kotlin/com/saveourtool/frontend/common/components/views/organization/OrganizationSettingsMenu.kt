@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.OrganizationDto
import com.saveourtool.common.entities.OrganizationStatus
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.basic.manageUserRoleCardComponent
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.isSuperAdmin
import com.saveourtool.frontend.common.utils.noopLoadingHandler
import com.saveourtool.frontend.common.utils.noopResponseHandler

import org.w3c.fetch.Response
import react.*
import react.dom.html.ReactHTML.div
import react.router.useNavigate
import web.cssom.ClassName

private val organizationGitCredentialsManageCard = manageGitCredentialsCardComponent()

/**
 * SETTINGS tab in OrganizationView
 */
val organizationSettingsMenu = organizationSettingsMenu()

/**
 * OrganizationSettingsMenu component props
 */
external interface OrganizationSettingsMenuProps : Props {
    /**
     * Current organization settings
     */
    var organizationName: String

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo

    /**
     * [Role] of user that is observing this component
     */
    var selfRole: Role

    /**
     * Callback to show error message
     */
    @Suppress("TYPE_ALIAS")
    var updateErrorMessage: (Response, String) -> Unit

    /**
     * Callback to show notification message
     */
    var updateNotificationMessage: (String, String) -> Unit

    /**
     * Current organization
     */
    var organization: OrganizationDto

    /**
     * Callback invoked in order to change canCreateContests flag
     */
    var onCanCreateContestsChange: (Boolean) -> Unit

    /**
     * Callback invoked in order to change canBulkUpload flag
     */
    var onCanBulkUploadCosvFilesChange: (Boolean) -> Unit
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

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod"
)
private fun organizationSettingsMenu() = FC<OrganizationSettingsMenuProps> { props ->
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    val organizationPath = props.organizationName

    val navigate = useNavigate()
    div {
        className = ClassName("row justify-content-center mb-2 text-gray-900")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Users"
            }
            manageUserRoleCardComponent {
                selfUserInfo = props.currentUserInfo
                groupPath = organizationPath
                groupType = "organization"
                getUserGroups = { it.organizations }
            }
        }
        // ===================== RIGHT COLUMN ======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Git credentials"
            }
            organizationGitCredentialsManageCard {
                selfUserInfo = props.currentUserInfo
                organizationName = props.organizationName
                updateErrorMessage = props.updateErrorMessage
            }
        }
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Main settings"
            }
            div {
                className = ClassName("card card-body mt-0 p-0")
                div {
                    className = ClassName("d-sm-flex align-items-center justify-content-center p-3")
                    actionButton {
                        title = "WARNING: You are about to delete this organization"
                        errorTitle = "You cannot delete the organization ${props.organizationName}"
                        message = "Are you sure you want to delete the organization ${props.organizationName}?"
                        clickMessage = "Also ban this organization"
                        buttonStyleBuilder = { childrenBuilder ->
                            with(childrenBuilder) {
                                +"Delete ${props.organizationName}"
                            }
                        }
                        classes = "btn btn-sm btn-outline-danger"
                        modalButtons = { action, closeWindow, childrenBuilder, isClickMode ->
                            val actionName = if (isClickMode) "ban" else "delete"
                            with(childrenBuilder) {
                                buttonBuilder(label = "Yes, $actionName ${props.organizationName}", style = "danger", classes = "mr-2") {
                                    action()
                                    closeWindow()
                                }
                                buttonBuilder("Cancel") {
                                    closeWindow()
                                }
                            }
                        }
                        onActionSuccess = { _ ->
                            navigate(to = "/${FrontendRoutes.PROJECTS}")
                        }
                        conditionClick = props.currentUserInfo.isSuperAdmin()
                        sendRequest = { isBanned ->
                            val newStatus = if (isBanned) OrganizationStatus.BANNED else OrganizationStatus.DELETED
                            responseChangeOrganizationStatus(props.organizationName, newStatus)
                        }
                    }
                }
            }
        }
    }
}
