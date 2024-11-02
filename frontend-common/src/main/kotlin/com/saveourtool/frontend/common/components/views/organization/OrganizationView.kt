@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
)

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.OrganizationDto
import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.common.entities.ProjectStatus
import com.saveourtool.common.filters.ProjectFilter
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.utils.getHighestRole
import com.saveourtool.frontend.common.components.basic.AVATAR_ORGANIZATION_PLACEHOLDER
import com.saveourtool.frontend.common.components.basic.avatarRenderer
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import org.w3c.fetch.Headers
import react.FC
import react.Props
import react.useState

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val organizationView: FC<OrganizationProps> = FC { props ->
    useBackground(Style.SAVE_LIGHT)

    val (organization, setOrganization) = useState(OrganizationDto.empty)
    val (selectedMenu, setSelectedMenu) = useState(OrganizationMenuBar.defaultTab)
    val (closeButtonLabel, setCloseButtonLabel) = useState<String?>(null)
    val (selfRole, setSelfRole) = useState(Role.NONE)
    val (isErrorOpen, setIsErrorOpen) = useState(false)
    val (errorMessage, setErrorMessage) = useState("")
    val (errorLabel, setErrorLabel) = useState("")
    val (isAvatarWindowOpen, setIsAvatarWindowOpen) = useState(false)
    val (usersInOrganization, setUsersInOrganization) = useState<List<UserInfo>>(emptyList())
    val (avatar, setAvatar) = useState(AVATAR_ORGANIZATION_PLACEHOLDER)
    val (projects, setProjects) = useState<List<ProjectDto>>(emptyList())

    val (canCreateContests, setCanCreateContests) = useState(false)
    val (canBulkUpload, setCanBulkUpload) = useState(false)

    val valuesOrganizationMenuBar: Array<OrganizationMenuBar> = props.organizationType.listTab

    useRequest {
        val organizationLoaded: OrganizationDto = get(
            "$apiUrl/organizations/${props.organizationName}",
            jsonHeaders,
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .decodeFromJsonString()
        setOrganization(organizationLoaded)
        organizationLoaded.avatar?.let { setAvatar(it.avatarRenderer()) }
        setCanCreateContests(organizationLoaded.canCreateContests)

        val comparator: Comparator<ProjectDto> =
                compareBy<ProjectDto> { it.status.ordinal }
                    .thenBy { it.name }

        val projectsLoaded = post(
            url = "$apiUrl/projects/by-filters",
            headers = jsonHeaders,
            body = Json.encodeToString(ProjectFilter("", props.organizationName, enumValues<ProjectStatus>().toSet())),
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<ProjectDto>>()
            }
        setProjects(projectsLoaded.sortedWith(comparator))

        val role = get(
            url = "$apiUrl/organizations/${props.organizationName}/users/roles",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<Role>()
            }

        val users = get(
            url = "$apiUrl/organizations/${props.organizationName}/users",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap { it.decodeFromJsonString<List<UserInfo>>() }

        val highestRole = getHighestRole(role, props.currentUserInfo?.globalRole)

        setSelfRole(highestRole)
        setUsersInOrganization(users)
    }

    val onCanCreateContestsChange = useDeferredRequest {
        val response = post(
            "$apiUrl/organizations/${props.organizationName}/manage-contest-permission?isAbleToCreateContests=${!organization.canCreateContests}",
            headers = jsonHeaders,
            undefined,
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            setOrganization {
                it.copy(canCreateContests = canCreateContests)
            }
        }
    }

    val onCanBulkUploadCosvFilesChange = useDeferredRequest {
        val response = post(
            "$apiUrl/organizations/${props.organizationName}/manage-bulk-upload-permission",
            params = jso<dynamic> {
                isAbleToToBulkUpload = !organization.canBulkUpload
            },
            headers = jsonHeaders,
            undefined,
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            setOrganization {
                it.copy(canBulkUpload = canBulkUpload)
            }
        }
    }

    renderOrganizationMenuBar {
        this.isAvatarWindowOpen = isAvatarWindowOpen
        this.setIsAvatarWindowOpen = setIsAvatarWindowOpen
        this.avatar = avatar
        this.setAvatar = setAvatar
        this.organization = organization
        this.valuesOrganizationMenuBar = valuesOrganizationMenuBar
        this.selectedMenu = selectedMenu
        this.setSelectedMenu = setSelectedMenu
        this.selfRole = selfRole
        this.organizationName = props.organizationName
    }

    when (selectedMenu) {
        OrganizationMenuBar.INFO -> renderInfoTab {
            this.usersInOrganization = usersInOrganization
            this.organization = organization
            this.setOrganization = setOrganization
            this.selfRole = selfRole
            this.organizationName = props.organizationName
        }
        OrganizationMenuBar.SETTINGS -> organizationSettingsMenu {
            this.organizationName = props.organizationName
            this.currentUserInfo = props.currentUserInfo ?: UserInfo(name = "Undefined")
            this.selfRole = selfRole
            this.updateErrorMessage = { response, message ->
                setIsErrorOpen(true)
                setErrorLabel(response.statusText)
                setErrorMessage(message)
            }
            this.updateNotificationMessage = { notificationLabel, notificationMessage ->
                setIsErrorOpen(true)
                setErrorLabel(notificationLabel)
                setErrorMessage(notificationMessage)
                setCloseButtonLabel("Confirm")
            }
            this.organization = organization
            this.onCanCreateContestsChange = { isCreateContests ->
                setCanCreateContests(isCreateContests)
                onCanCreateContestsChange()
            }
            this.onCanBulkUploadCosvFilesChange = { isCanBulkUpload ->
                setCanBulkUpload(isCanBulkUpload)
                onCanBulkUploadCosvFilesChange()
            }
        }
        OrganizationMenuBar.VULNERABILITIES -> renderVulnerabilitiesTab {
            this.currentUserInfo = props.currentUserInfo
            this.organizationName = props.organizationName
            this.selfRole = selfRole
        }
        OrganizationMenuBar.BENCHMARKS -> organizationTestsMenu {
            this.organizationName = props.organizationName
            this.selfRole = selfRole
        }
        OrganizationMenuBar.CONTESTS -> organizationContestsMenu {
            this.organizationName = props.organizationName
            this.selfRole = selfRole
            this.updateErrorMessage = { response ->
                setIsErrorOpen(true)
                setErrorLabel("")
                setErrorMessage("Failed to create contest: ${response.status} ${response.statusText}")
            }
        }
        OrganizationMenuBar.TOOLS -> organizationToolsMenu {
            this.currentUserInfo = props.currentUserInfo
            this.selfRole = selfRole
            this.organization = organization
            this.projects = projects
            this.updateProjects = { projectsList ->
                setProjects(projectsList)
            }
        }
        OrganizationMenuBar.ADMIN -> renderAdminTab {
            this.organization = organization
            this.onCanCreateContestsChange = { isCreateContests ->
                setCanCreateContests(isCreateContests)
                onCanCreateContestsChange()
            }
            this.onCanBulkUploadCosvFilesChange = { isCanBulkUpload ->
                setCanBulkUpload(isCanBulkUpload)
                onCanBulkUploadCosvFilesChange()
            }
        }
    }
}

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface OrganizationProps : Props {
    var organizationName: String
    var currentUserInfo: UserInfo?
    var organizationType: OrganizationType
}
