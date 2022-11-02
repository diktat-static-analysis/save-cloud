/**
 * A view with organization details
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.ImageInfo
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.Role.*
import com.saveourtool.save.entities.*
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.basic.organizations.organizationContestsMenu
import com.saveourtool.save.frontend.components.basic.organizations.organizationSettingsMenu
import com.saveourtool.save.frontend.components.basic.organizations.organizationTestsMenu
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.smallTransparentModalStyle
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.http.getOrganization
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.actionButton
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.getHighestRole
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes

import csstype.*
import history.Location
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import org.w3c.xhr.FormData
import react.*
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.textarea
import react.router.dom.Link
import react.table.columns

import kotlinx.coroutines.launch
import kotlinx.js.jso
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The mandatory column id.
 * For each cell, this will be transformed into "cell_%d_delete_button" and
 * visible as the key in the "Components" tab of the developer tools.
 */
const val DELETE_BUTTON_COLUMN_ID = "delete_button"

/**
 * Empty table header.
 */
const val EMPTY_COLUMN_HEADER = ""

/**
 * CSS classes of the "delete project" button.
 */
val actionButtonClasses: List<String> = listOf("btn", "btn-small")

/**
 * CSS classes of the "delete project" icon.
 */
val actionIconClasses: List<String> = listOf("trash-alt")

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface OrganizationProps : PropsWithChildren {
    var organizationName: String
    var currentUserInfo: UserInfo?
    var location: Location
}

/**
 * [State] of project view component
 */
external interface OrganizationViewState : StateWithRole, State, HasSelectedMenu<OrganizationMenuBar> {
    /**
     * Flag to handle uploading a file
     */
    var isUploading: Boolean

    /**
     * Image to owner avatar
     */
    var image: ImageInfo?

    /**
     * Organization
     */
    var organization: Organization?

    /**
     * List of projects for `this` organization
     */
    var projects: List<Project>

    /**
     * List of projects for `this` organization
     */
    var deletedProjects: List<Project>

    /**
     * List of projects for `this` organization
     */
    var bannedProjects: List<Project>

    /**
     * Message of error
     */
    var errorMessage: String

    /**
     * Flag to handle error
     */
    var isErrorOpen: Boolean

    /**
     * Error label
     */
    var errorLabel: String

    /**
     * State for the creation of unified confirmation logic
     */
    var confirmationType: ConfirmationType

    /**
     * Flag to handle confirm Window
     */
    var isConfirmWindowOpen: Boolean

    /**
     * Whether editing of organization info is disabled
     */
    var isEditDisabled: Boolean

    /**
     * Users in organization
     */
    var usersInOrganization: List<UserInfo>?

    /**
     * Label that will be shown on Close button of modal windows
     */
    var closeButtonLabel: String?

    /**
     * Current state of description input form
     */
    var draftOrganizationDescription: String

    /**
     * Contains the paths of default and other tabs
     */
    var paths: PathsForTabs
}

/**
 * A Component for owner view
 */
@Suppress("LargeClass")
class OrganizationView : AbstractView<OrganizationProps, OrganizationViewState>(false) {
    private val tableWithProjects: FC<TableProps<Project>> = tableComponent(
        columns = {
            columns {
                column(id = "name", header = "Evaluated Tool", { name }) { cellProps ->
                    Fragment.create {
                        td {
                            val project = cellProps.row.original
                            when (project.status) {
                                ProjectStatus.CREATED -> {
                                    a {
                                        href = "#/${cellProps.row.original.organization.name}/${cellProps.value}"
                                        +cellProps.value
                                    }
                                    privacySpan(cellProps.row.original)
                                }
                                ProjectStatus.DELETED -> {
                                    +cellProps.value
                                    span {
                                        className = ClassName("border ml-2 pr-1 pl-1 text-xs text-muted ")
                                        style = jso { borderRadius = "2em".unsafeCast<BorderRadius>() }
                                        +"deleted"
                                    }
                                }
                                ProjectStatus.BANNED -> div {
                                    className = ClassName("text-danger")
                                    +cellProps.value
                                    span {
                                        className = ClassName("border ml-2 pr-1 pl-1 text-xs text-muted ")
                                        style = jso { borderRadius = "2em".unsafeCast<BorderRadius>() }
                                        +"banned"
                                    }
                                }
                            }
                        }
                    }
                }
                column(id = "description", header = "Description") {
                    Fragment.create {
                        td {
                            +(it.value.description ?: "Description not provided")
                        }
                    }
                }
                column(id = "rating", header = "Contest Rating") {
                    Fragment.create {
                        td {
                            +"0"
                        }
                    }
                }
                /*
                 * A "secret" possibility to delete projects (intended for super-admins).
                 */
                column(id = DELETE_BUTTON_COLUMN_ID, header = EMPTY_COLUMN_HEADER) { cellProps ->
                    val project = cellProps.row.original
                    Fragment.create {
                        td {
                            when (project.status) {
                                ProjectStatus.CREATED -> if (state.selfRole.isHigherOrEqualThan(OWNER)) {
                                    actionButton {
                                        val projectName = project.name

                                        title = "WARNING: You want to delete a project"
                                        errorTitle = "You cannot delete a $projectName"
                                        message = "Are you sure you want to delete a $projectName?"
                                        clickMessage = "Change to ban mode"
                                        buttonStyleBuilder = { childrenBuilder ->
                                            with(childrenBuilder) {
                                                fontAwesomeIcon(
                                                    icon = faTrashAlt,
                                                    classes = actionIconClasses.joinToString(" ")
                                                )
                                            }
                                        }
                                        classes = actionButtonClasses.joinToString(" ")
                                        modalButtons = { action, window, childrenBuilder ->
                                            with(childrenBuilder) {
                                                buttonBuilder(label = "Yes, delete $projectName", style = "danger", classes = "mr-2") {
                                                    action()
                                                    window.closeWindow()
                                                }
                                                buttonBuilder("Cancel") {
                                                    window.closeWindow()
                                                }
                                            }
                                        }
                                        onActionSuccess = { clickMode: Boolean ->
                                            setState {
                                                projects = projects.minus(project)
                                                if (clickMode) {
                                                    bannedProjects = bannedProjects.plus(project.copy(status = ProjectStatus.BANNED))
                                                } else {
                                                    deletedProjects = deletedProjects.plus(project.copy(status = ProjectStatus.DELETED))
                                                }
                                            }
                                        }
                                        conditionClick = state.selfRole.isHigherOrEqualThan(SUPER_ADMIN)
                                        sendRequest = { isClickMode ->
                                            responseDeleteProject(isClickMode, project)
                                        }
                                    }
                                }

                                ProjectStatus.DELETED -> if (state.selfRole.isHigherOrEqualThan(OWNER)) {
                                    actionButton {
                                        val projectName = project.name

                                        title = "WARNING: You want to recover a project"
                                        errorTitle = "You cannot recover $projectName"
                                        message = "Are you sure you want to recover an $projectName?"
                                        buttonStyleBuilder = { childrenBuilder ->
                                            with(childrenBuilder) {
                                                fontAwesomeIcon(
                                                    icon = faRedo,
                                                    classes = actionIconClasses.joinToString(" ")
                                                )
                                            }
                                        }
                                        classes = actionButtonClasses.joinToString(" ")
                                        modalButtons = { action, window, childrenBuilder ->
                                            with(childrenBuilder) {
                                                buttonBuilder(label = "Yes, recover $projectName", style = "warning", classes = "mr-2") {
                                                    action()
                                                    window.closeWindow()
                                                }
                                                buttonBuilder("Cancel") {
                                                    window.closeWindow()
                                                }
                                            }
                                        }
                                        onActionSuccess = { _ ->
                                            setState {
                                                deletedProjects = deletedProjects.minus(project)
                                                projects = projects.plus(project.copy(status = ProjectStatus.CREATED))
                                            }
                                        }
                                        conditionClick = false
                                        sendRequest = { _ ->
                                            responseRecoverProject(project)
                                        }
                                    }
                                }
                                ProjectStatus.BANNED -> if (state.selfRole.isHigherOrEqualThan(SUPER_ADMIN)) {
                                    actionButton {
                                        val projectName = project.name

                                        title = "WARNING: You want to recover a banned project"
                                        errorTitle = "You cannot recover a banned $projectName"
                                        message = "Are you sure you want to recover an $projectName?"
                                        buttonStyleBuilder = { childrenBuilder ->
                                            with(childrenBuilder) {
                                                fontAwesomeIcon(
                                                    icon = faRedo,
                                                    classes = actionIconClasses.joinToString(" ")
                                                )
                                            }
                                        }
                                        classes = actionButtonClasses.joinToString(" ")
                                        modalButtons = { action, window, childrenBuilder ->
                                            with(childrenBuilder) {
                                                buttonBuilder(
                                                    label = "Yes, recover $projectName",
                                                    style = "warning",
                                                    classes = "mr-2"
                                                ) {
                                                    action()
                                                    window.closeWindow()
                                                }
                                                buttonBuilder("Cancel") {
                                                    window.closeWindow()
                                                }
                                            }
                                        }
                                        onActionSuccess = { _ ->
                                            setState {
                                                bannedProjects = bannedProjects.minus(project)
                                                projects = projects.plus(project.copy(status = ProjectStatus.CREATED))
                                            }
                                        }
                                        conditionClick = false
                                        sendRequest = { _ ->
                                            responseRecoverProject(project)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        useServerPaging = false,
        usePageSelection = false,
        getAdditionalDependencies = { tableProps ->
            /*-
             * Necessary for the table to get re-rendered once a project gets
             * deleted.
             *
             * The order and size of the array must remain constant.
             */
            arrayOf(tableProps, state.projects, state.deletedProjects, state.bannedProjects)
        }
    )

    init {
        state.isUploading = false
        state.organization = Organization("", OrganizationStatus.CREATED, null, null, null)
        state.selectedMenu = OrganizationMenuBar.defaultTab
        state.projects = listOf()
        state.deletedProjects = listOf()
        state.bannedProjects = listOf()
        state.closeButtonLabel = null
        state.selfRole = NONE
        state.draftOrganizationDescription = ""
        state.isConfirmWindowOpen = false
        state.isErrorOpen = false
        state.confirmationType = ConfirmationType.DELETE_CONFIRM
    }

    private fun showNotification(notificationLabel: String, notificationMessage: String) {
        setState {
            isErrorOpen = true
            errorLabel = notificationLabel
            errorMessage = notificationMessage
            closeButtonLabel = "Confirm"
        }
    }

    override fun componentDidUpdate(prevProps: OrganizationProps, prevState: OrganizationViewState, snapshot: Any) {
        if (state.selectedMenu != prevState.selectedMenu) {
            changeUrl(state.selectedMenu, OrganizationMenuBar, state.paths)
        } else if (props.location != prevProps.location) {
            urlAnalysis(OrganizationMenuBar, state.selfRole, state.organization?.canCreateContests)
        }
    }

    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch {
            val organizationLoaded = getOrganization(props.organizationName)
            val projectsLoaded = getProjectsForOrganization()
            val role = getRoleInOrganization()
            val users = getUsers()
            val highestRole = getHighestRole(role, props.currentUserInfo?.globalRole)
            setState {
                paths = PathsForTabs("/${props.organizationName}", "#/${OrganizationMenuBar.nameOfTheHeadUrlSection}/${props.organizationName}")
                organization = organizationLoaded
                image = ImageInfo(organizationLoaded.avatar)
                draftOrganizationDescription = organizationLoaded.description ?: ""
                projects = projectsLoaded.filter { it.status == ProjectStatus.CREATED }
                deletedProjects = projectsLoaded.filter { it.status == ProjectStatus.DELETED }
                bannedProjects = projectsLoaded.filter { it.status == ProjectStatus.BANNED }
                isEditDisabled = true
                selfRole = highestRole
                usersInOrganization = users
            }
            urlAnalysis(OrganizationMenuBar, highestRole, organizationLoaded.canCreateContests)
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
    override fun ChildrenBuilder.render() {
        val errorCloseCallback = {
            setState {
                isErrorOpen = false
                closeButtonLabel = null
            }
        }
        displayModal(state.isErrorOpen, state.errorLabel, state.errorMessage, smallTransparentModalStyle, errorCloseCallback) {
            buttonBuilder(state.closeButtonLabel ?: "Close", "secondary") { errorCloseCallback() }
        }

        renderOrganizationMenuBar()

        when (state.selectedMenu) {
            OrganizationMenuBar.INFO -> renderInfo()
            OrganizationMenuBar.TOOLS -> renderTools()
            OrganizationMenuBar.TESTS -> renderTests()
            OrganizationMenuBar.SETTINGS -> renderSettings()
            OrganizationMenuBar.CONTESTS -> renderContests()
        }
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "LongMethod",
        "ComplexMethod",
        "PARAMETER_NAME_IN_OUTER_LAMBDA",
    )
    private fun ChildrenBuilder.renderInfo() {
        // ================= Title for TOP projects ===============
        div {
            className = ClassName("row justify-content-center mb-2")
            h4 {
                +"Top Tools"
            }
        }

        // ================= Rows for TOP projects ================
        val topProjects = state.projects.sortedByDescending { it.contestRating }.take(TOP_PROJECTS_NUMBER)

        div {
            className = ClassName("row justify-content-center")

            renderTopProject(topProjects.getOrNull(0))
            renderTopProject(topProjects.getOrNull(1))
        }

        @Suppress("MAGIC_NUMBER")
        div {
            className = ClassName("row justify-content-center")

            renderTopProject(topProjects.getOrNull(2))
            renderTopProject(topProjects.getOrNull(3))
        }

        div {
            className = ClassName("row justify-content-center")

            div {
                className = ClassName("col-3 mb-4")
                div {
                    className = ClassName("card shadow mb-4")
                    div {
                        className = ClassName("card-header py-3")
                        div {
                            className = ClassName("row")
                            h6 {
                                className = ClassName("m-0 font-weight-bold text-primary")
                                style = jso {
                                    display = Display.flex
                                    alignItems = AlignItems.center
                                }
                                +"Description"
                            }
                            if (state.selfRole.hasWritePermission() && state.isEditDisabled) {
                                button {
                                    type = ButtonType.button
                                    className = ClassName("btn btn-link text-xs text-muted text-left ml-auto")
                                    +"Edit  "
                                    fontAwesomeIcon(icon = faEdit)
                                    onClick = {
                                        turnEditMode(isOff = false)
                                    }
                                }
                            }
                        }
                    }
                    div {
                        className = ClassName("card-body")
                        textarea {
                            className = ClassName("auto_height form-control-plaintext pt-0 pb-0")
                            value = state.draftOrganizationDescription
                            disabled = !state.selfRole.hasWritePermission() || state.isEditDisabled
                            onChange = {
                                setNewDescription(it.target.value)
                            }
                        }
                    }
                    div {
                        className = ClassName("ml-3 mt-2 align-items-right float-right")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faCheck)
                            hidden = !state.selfRole.hasWritePermission() || state.isEditDisabled
                            onClick = {
                                state.organization?.let { onOrganizationSave(it) }
                                turnEditMode(true)
                            }
                        }

                        button {
                            type = ButtonType.button
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faTimesCircle)
                            hidden = !state.selfRole.hasWritePermission() || state.isEditDisabled
                            onClick = {
                                turnEditMode(true)
                            }
                        }
                    }
                }
            }

            div {
                className = ClassName("col-3")
                userBoard {
                    users = state.usersInOrganization.orEmpty()
                }
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    private fun ChildrenBuilder.renderTools() {
        div {
            className = ClassName("row justify-content-center")
            div {
                className = ClassName("col-6")
                div {
                    className = ClassName("d-flex justify-content-center mb-2")
                    if (state.selfRole.isHigherOrEqualThan(Role.ADMIN)) {
                        Link {
                            to = "/${FrontendRoutes.CREATE_PROJECT.path}/${this@OrganizationView.state.organization?.name}"
                            button {
                                type = ButtonType.button
                                className = ClassName("btn btn-outline-info")
                                +"Add new Tool"
                            }
                        }
                    }
                }

                tableWithProjects {
                    getData = { _, _ ->
                        getProjectsFromCache().toTypedArray()
                    }
                    getPageCount = null
                }
            }
        }
    }

    private fun setNewDescription(value: String) {
        setState {
            draftOrganizationDescription = value
        }
    }

    private fun onOrganizationSave(newOrganization: Organization) {
        newOrganization.apply {
            description = state.draftOrganizationDescription
        }
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            val response = post(
                "$apiUrl/organizations/${props.organizationName}/update",
                headers,
                Json.encodeToString(newOrganization),
                loadingHandler = ::noopLoadingHandler,
            )
            if (response.ok) {
                setState {
                    organization = newOrganization
                }
            }
        }
    }

    private fun ChildrenBuilder.renderTests() {
        organizationTestsMenu {
            organizationName = props.organizationName
            selfRole = state.selfRole
        }
    }

    private fun ChildrenBuilder.renderContests() {
        organizationContestsMenu {
            organizationName = props.organizationName
            selfRole = state.selfRole
            updateErrorMessage = {
                setState {
                    isErrorOpen = true
                    errorLabel = ""
                    errorMessage = "Failed to create contest: ${it.status} ${it.statusText}"
                }
            }
        }
    }

    private fun ChildrenBuilder.renderSettings() {
        organizationSettingsMenu {
            organizationName = props.organizationName
            currentUserInfo = props.currentUserInfo ?: UserInfo("Undefined")
            selfRole = state.selfRole
            updateErrorMessage = { response, message ->
                setState {
                    isErrorOpen = true
                    errorLabel = response.statusText
                    errorMessage = message
                }
            }
            updateNotificationMessage = ::showNotification
            organization = state.organization ?: Organization.stub(-1)
            onCanCreateContestsChange = ::onCanCreateContestsChange
        }
    }

    private fun turnEditMode(isOff: Boolean) {
        setState {
            isEditDisabled = isOff
        }
    }

    /**
     * Small workaround to avoid the request to the backend for the second time and to use it inside the Table view
     */
    private fun getProjectsFromCache(): List<Project> = state.projects + state.deletedProjects + state.bannedProjects

    private suspend fun getProjectsForOrganization(): List<Project> = get(
        url = "$apiUrl/projects/get/projects-by-organization?organizationName=${props.organizationName}&status=${ProjectStatus.CREATED}",
        headers = jsonHeaders,
        loadingHandler = ::classLoadingHandler,
    )
        .unsafeMap {
            it.decodeFromJsonString()
        }

    private fun onCanCreateContestsChange(canCreateContests: Boolean) {
        scope.launch {
            val response = post(
                "$apiUrl/organizations/${props.organizationName}/manage-contest-permission?isAbleToCreateContests=${!state.organization!!.canCreateContests}",
                headers = jsonHeaders,
                undefined,
                loadingHandler = ::classLoadingHandler,
            )
            if (response.ok) {
                setState {
                    organization = organization?.copy(canCreateContests = canCreateContests)
                }
            }
        }
    }

    private suspend fun getRoleInOrganization(): Role = get(
        url = "$apiUrl/organizations/${props.organizationName}/users/roles",
        headers = Headers().also {
            it.set("Accept", "application/json")
        },
        loadingHandler = ::classLoadingHandler,
        responseHandler = ::noopResponseHandler,
    )
        .unsafeMap {
            it.decodeFromJsonString()
        }

    private suspend fun getUsers(): List<UserInfo> = get(
        url = "$apiUrl/organizations/${props.organizationName}/users",
        headers = Headers().also {
            it.set("Accept", "application/json")
        },
        loadingHandler = ::classLoadingHandler,
    )
        .unsafeMap {
            it.decodeFromJsonString()
        }

    private fun postImageUpload(element: dom.html.HTMLInputElement) =
            scope.launch {
                setState {
                    isUploading = true
                }
                element.files!!.asList().single().let { file ->
                    val response: ImageInfo? = post(
                        "$apiUrl/image/upload?owner=${props.organizationName}&type=${AvatarType.ORGANIZATION}",
                        Headers(),
                        FormData().apply {
                            append("file", file)
                        },
                        loadingHandler = ::noopLoadingHandler,
                    )
                        .decodeFromJsonString()
                    setState {
                        image = response
                    }
                }
                setState {
                    isUploading = false
                }
            }

    private fun ChildrenBuilder.renderTopProject(topProject: Project?) {
        div {
            className = ClassName("col-3 mb-4")
            topProject?.let {
                scoreCard {
                    name = it.name
                    contestScore = it.contestRating
                    url = "#/${props.organizationName}/${it.name}"
                }
            }
        }
    }

    @Suppress("LongMethod", "TOO_LONG_FUNCTION", "MAGIC_NUMBER")
    private fun ChildrenBuilder.renderOrganizationMenuBar() {
        div {
            className = ClassName("row d-flex")
            div {
                className = ClassName("col-3 ml-auto justify-content-center")
                style = jso {
                    display = Display.flex
                    alignItems = AlignItems.center
                }
                label {
                    input {
                        type = InputType.file
                        hidden = true
                        onChange = { event ->
                            postImageUpload(event.target)
                        }
                    }
                    ariaLabel = "Change organization's avatar"
                    img {
                        className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                        src = state.image?.path?.let {
                            "/api/$v1/avatar$it"
                        }
                            ?: run {
                                "img/company.svg"
                            }
                        height = 100.0
                        width = 100.0
                    }
                }

                h1 {
                    className = ClassName("h3 mb-0 text-gray-800 ml-2")
                    +(state.organization?.name ?: "N/A")
                }
            }

            div {
                className = ClassName("col-auto mx-0 justify-content-center")
                style = jso {
                    display = Display.flex
                    alignItems = AlignItems.center
                }

                nav {
                    className = ClassName("nav nav-tabs")
                    OrganizationMenuBar.values()
                        .filter {
                            it != OrganizationMenuBar.SETTINGS || state.selfRole.isHigherOrEqualThan(Role.ADMIN)
                        }
                        .filter {
                            it != OrganizationMenuBar.CONTESTS || state.selfRole.isHigherOrEqualThan(Role.OWNER) && state.organization?.canCreateContests == true
                        }
                        .forEach { organizationMenu ->
                            li {
                                className = ClassName("nav-item")
                                style = jso {
                                    cursor = "pointer".unsafeCast<Cursor>()
                                }
                                val classVal = if (state.selectedMenu == organizationMenu) " active font-weight-bold" else ""
                                p {
                                    className = ClassName("nav-link $classVal text-gray-800")
                                    onClick = {
                                        if (state.selectedMenu != organizationMenu) {
                                            setState { selectedMenu = organizationMenu }
                                        }
                                    }
                                    +organizationMenu.getTitle()
                                }
                            }
                        }
                }
            }

            div {
                className = ClassName("col-3 mr-auto justify-content-center align-items-center")
            }
        }
    }
    companion object :
        RStatics<OrganizationProps, OrganizationViewState, OrganizationView, Context<RequestStatusContext>>(
        OrganizationView::class
    ) {
        const val TOP_PROJECTS_NUMBER = 4
        init {
            contextType = requestStatusContext
        }
    }
}

/**
 * Makes a call to delete or ban the project, depending on the [isClickMode] value
 *
 * @param isClickMode to determine whether a click occurred or not
 * @param project is project
 * @return response
 */
fun responseDeleteProject(isClickMode: Boolean, project: Project): suspend WithRequestStatusContext.() -> Response = {
    delete(
        url = buildString {
            append("$apiUrl/projects/${project.organization.name}/${project.name}/delete")
            if (isClickMode) append("?status=${OrganizationStatus.BANNED}") else append("?status=${OrganizationStatus.DELETED}")
        },
        headers = jsonHeaders,
        loadingHandler = ::noopLoadingHandler,
        errorHandler = ::noopResponseHandler,
    )
}

/**
 * Makes a call to recover the project
 *
 * @param project
 * @return response
 */
fun responseRecoverProject(project: Project): suspend WithRequestStatusContext.() -> Response = {
    post(
        url = "$apiUrl/projects/${project.organization.name}/${project.name}/recover",
        headers = jsonHeaders,
        body = undefined,
        loadingHandler = ::noopLoadingHandler,
        responseHandler = ::noopResponseHandler,
    )
}
