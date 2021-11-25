/**
 * A view with project details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS", "LargeClass")

package org.cqfn.save.frontend.components.views

import org.cqfn.save.domain.FileInfo
import org.cqfn.save.domain.Sdk
import org.cqfn.save.domain.getSdkVersions
import org.cqfn.save.domain.toSdk
import org.cqfn.save.entities.*
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.frontend.components.basic.*
import org.cqfn.save.frontend.externals.fontawesome.faCalendarAlt
import org.cqfn.save.frontend.externals.fontawesome.faHistory
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.externals.modal.modal
import org.cqfn.save.frontend.utils.*
import org.cqfn.save.testsuite.TestSuiteDto

import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import org.w3c.xhr.FormData
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.dom.*
import react.setState

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.hidden
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.role
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectExecutionRouteProps : PropsWithChildren {
    var owner: String
    var name: String
}

/**
 * [State] of project view component
 */
external interface ProjectViewState : State {
    /**
     * Files required for tests execution for this project
     */
    var files: MutableList<FileInfo>

    /**
     * Files that are available on server side
     */
    var availableFiles: MutableList<FileInfo>

    /**
     * Message of error
     */
    var errorMessage: String

    /**
     * Flag to handle error
     */
    var isErrorOpen: Boolean?

    /**
     * Error label
     */
    var errorLabel: String

    /**
     * Message of warning
     */
    var confirmMessage: String

    /**
     * Flag to handle confirm Window
     */
    var isConfirmWindowOpen: Boolean?

    /**
     * Label of confirm Window
     */
    var confirmLabel: String

    /**
     * Flag to handle loading
     */
    var isLoading: Boolean?

    /**
     * Selected sdk
     */
    var selectedSdk: String

    /**
     * Selected version
     */
    var selectedSdkVersion: String

    /**
     * Flag to handle upload type project
     */
    var testingType: TestingType

    /**
     * Sumbit button was pressed
     */
    var isSubmitButtonPressed: Boolean?

    /**
     * State for the creation of unified confirmation logic
     */
    var confirmationType: ConfirmationType

    /**
     * Url to the custom tests
     */
    var gitUrlFromInputField: String

    /**
     * Directory in the repository where tests are placed
     */
    var testRootPath: String
}

/**
 * enum that stores types of confirmation windows
 */
enum class ConfirmationType {
    DELETE_CONFIRM,
    NO_BINARY_CONFIRM,
    NO_CONFIRM,
    ;
}

/**
 * A Component for project view
 * Each modal opening call causes re-render of the whole page, that's why we need to use state for all fields
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ProjectView : RComponent<ProjectExecutionRouteProps, ProjectViewState>() {
    private var standardTestSuites: List<TestSuiteDto> = emptyList()
    private val selectedStandardSuites: MutableList<String> = mutableListOf()
    private var gitDto: GitDto? = null
    private var project = Project("stub", "stub", "stub", "stub", ProjectStatus.CREATED)
    private val projectInformation = mutableMapOf(
        "Tested tool name: " to "",
        "Description: " to "",
        "Tested tool Url: " to "",
        "Test project owner: " to ""
    )
    private lateinit var responseFromDeleteProject: Response

    init {
        state.gitUrlFromInputField = ""
        state.testRootPath = ""
        state.confirmationType = ConfirmationType.NO_CONFIRM
        state.testingType = TestingType.CUSTOM_TESTS
        state.isErrorOpen = false
        state.isSubmitButtonPressed = false
        state.errorMessage = ""
        state.errorLabel = ""
        state.isLoading = true
        state.files = mutableListOf()
        state.availableFiles = mutableListOf()
        state.selectedSdk = Sdk.Default.name
        state.selectedSdkVersion = Sdk.Default.version
    }

    override fun componentDidMount() {
        GlobalScope.launch {
            project = getProject(props.name, props.owner)
            val jsonProject = Json.encodeToString(project)
            val headers = Headers().apply {
                set("Accept", "application/json")
                set("Content-Type", "application/json")
            }
            gitDto = post("${apiUrl}/getGit", headers, jsonProject)
                .decodeFromJsonString<GitDto>()
            standardTestSuites = get("${apiUrl}/allStandardTestSuites", headers)
                .decodeFromJsonString()

            val availableFiles = getFilesList()
            setState {
                this.availableFiles.clear()
                this.availableFiles.addAll(availableFiles)
                isLoading = false
            }
        }
    }

    @Suppress("ComplexMethod", "TOO_LONG_FUNCTION")
    private fun submitExecutionRequest() {
        when (state.testingType) {
            TestingType.CUSTOM_TESTS -> {
                val urlWithTests = state.gitUrlFromInputField
                // URL is required in all cases, the processing should not be done without it
                if (urlWithTests.isBlank()) {
                    return
                } else {
                    val newGitDto = gitDto?.copy(url = urlWithTests) ?: GitDto(url = urlWithTests)
                    submitExecutionRequestWithCustomTests(newGitDto)
                }
            }
            else -> {
                if (selectedStandardSuites.isEmpty()) {
                    setState {
                        isErrorOpen = true
                        errorLabel = "Both type of project"
                        errorMessage = "Please choose one of type test suites"
                    }
                    return
                }
                submitExecutionRequestWithStandardTests()
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun submitExecutionRequestWithStandardTests() {
        val headers = Headers()
        val formData = FormData()
        val selectedSdk = "${state.selectedSdk}:${state.selectedSdkVersion}".toSdk()
        val request = ExecutionRequestForStandardSuites(project, selectedStandardSuites, selectedSdk)
        formData.appendJson("execution", request)
        state.files.forEach {
            formData.appendJson("file", it)
        }
        submitRequest("/executionRequestStandardTests", headers, formData)
    }

    private fun submitExecutionRequestWithCustomTests(correctGitDto: GitDto) {
        val selectedSdk = "${state.selectedSdk}:${state.selectedSdkVersion}".toSdk()
        val formData = FormData()

        val testRootPath = if (state.testRootPath.isBlank()) "." else state.testRootPath
        val executionRequest = ExecutionRequest(project, correctGitDto, testRootPath, selectedSdk, null)

        formData.appendJson("executionRequest", executionRequest)
        state.files.forEach {
            formData.appendJson("file", it)
        }
        submitRequest("/submitExecutionRequest", Headers(), formData)
    }

    private fun submitRequest(url: String, headers: Headers, body: dynamic) {
        setState {
            isLoading = true
        }
        GlobalScope.launch {
            val response = post(apiUrl + url, headers, body)
            if (!response.ok) {
                response.text().then { text ->
                    setState {
                        isErrorOpen = true
                        errorLabel = "Error from backend"
                        errorMessage = "Request failed: [${response.statusText}] $text"
                    }
                }
            } else {
                window.location.href = "${window.location}/history"
            }
        }
            .invokeOnCompletion {
                setState {
                    isLoading = false
                }
            }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "ComplexMethod")
    override fun RBuilder.render() {
        // modal windows are initially hidden
        runErrorModal(state.isErrorOpen, state.errorLabel, state.errorMessage) {
            setState { isErrorOpen = false }
        }
        runConfirmWindowModal(
            state.isConfirmWindowOpen,
            state.confirmLabel,
            state.confirmMessage,
            { setState { isConfirmWindowOpen = false } }) {
            when (state.confirmationType) {
                ConfirmationType.NO_BINARY_CONFIRM, ConfirmationType.NO_CONFIRM -> submitExecutionRequest()
                ConfirmationType.DELETE_CONFIRM -> deleteProjectBuilder()
                else -> {
                    // this is a generated else block
                }
            }
            setState { isConfirmWindowOpen = false }
        }
        runLoadingModal()
        // Page Heading
        div("d-sm-flex align-items-center justify-content-center mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +"Project ${project.name}"
            }
        }

        div("row justify-content-center") {
            // ===================== LEFT COLUMN =======================================================================
            div("col-2 mr-3") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Testing types"
                }

                child(cardComponent {
                    div("text-left") {
                        testingTypeButton(
                            TestingType.CUSTOM_TESTS,
                            "Run your tool with your specific tests from git",
                            "mr-2"
                        )
                        testingTypeButton(
                            TestingType.STANDARD_BENCHMARKS,
                            "Run your tool with standard test suites",
                            "mt-3 mr-2"
                        )
                    }
                })
            }
            // ===================== MIDDLE COLUMN =====================================================================
            div("col-4") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Test configuration"
                }

                // ======== file selector =========
                child(fileUploader(
                    onFileSelect = { element ->
                        setState {
                            val availableFile = availableFiles.first { it.name == element.value }
                            files.add(availableFile)
                            availableFiles.remove(availableFile)
                        }
                    },
                    onFileRemove = {
                        setState {
                            files.remove(it)
                            availableFiles.add(it)
                        }
                    },
                    onFileInput = { postFileUpload(it) },
                    onExecutableChange = { selectedFile, checked ->
                        setState {
                            files[files.indexOf(selectedFile)] = selectedFile.copy(isExecutable = checked)
                        }
                    }
                )
                ) {
                    attrs.isSubmitButtonPressed = state.isSubmitButtonPressed
                    attrs.files = state.files
                    attrs.availableFiles = state.availableFiles
                    attrs.confirmationType = state.confirmationType
                }

                // ======== sdk selection =========
                child(sdkSelection({
                    setState {
                        selectedSdk = it.value
                        selectedSdkVersion = selectedSdk.getSdkVersions().first()
                    }
                }, {
                    setState { selectedSdkVersion = it.value }
                })) {
                    attrs.selectedSdk = state.selectedSdk
                    attrs.selectedSdkVersion = state.selectedSdkVersion
                }

                // ======== test resources selection =========
                child(testResourcesSelection(
                    updateGitUrlFromInputField = {
                        setState {
                            gitUrlFromInputField = (it.target as HTMLInputElement).value
                        }
                    },
                    updateTestRootPath = {
                        setState {
                            testRootPath = (it.target as HTMLInputElement).value
                        }
                    },
                    setTestRootPathFromHistory = {
                        setState {
                            testRootPath = it
                        }
                    }
                )) {
                    attrs.testingType = state.testingType
                    attrs.isSubmitButtonPressed = state.isSubmitButtonPressed
                    attrs.gitDto = gitDto
                    // properties for CUSTOM_TESTS mode
                    attrs.testRootPath = state.testRootPath
                    attrs.gitUrlFromInputField = state.gitUrlFromInputField
                    // properties for STANDARD_BENCHMARKS mode
                    attrs.selectedStandardSuites = selectedStandardSuites
                    attrs.standardTestSuites = standardTestSuites
                }

                div("d-sm-flex align-items-center justify-content-center") {
                    button(type = ButtonType.button, classes = "btn btn-primary") {
                        attrs.onClickFunction = { submitWithValidation() }
                        +"Test the tool now"
                    }
                }
            }
            // ===================== RIGHT COLUMN ======================================================================
            div("col-3 ml-2") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Information"
                    button(classes = "btn btn-link text-xs text-muted text-left p-1 ml-2") {
                        +"Edit"
                        attrs.onClickFunction = {
                            turnEditMode(off = false)
                        }
                    }
                }

                child(cardComponent {
                    val newProjectInformation: MutableMap<String, String> = mutableMapOf()
                    form {
                        projectInformation.putAll(
                            projectInformation.keys.zip(
                                listOf(
                                    project.name,
                                    project.description ?: "",
                                    project.url ?: "",
                                    project.owner
                                )
                            )
                        )
                        projectInformation
                            .forEach { (header, text) ->
                                div("control-group form-inline") {
                                    label(classes = "control-label col-auto") {
                                        +header
                                    }
                                    div("controls col-auto") {
                                        input(InputType.text, classes = "form-control-plaintext") {
                                            attrs.id = header
                                            attrs.defaultValue = text
                                            attrs.disabled = true
                                            attrs {
                                                onChangeFunction = {
                                                    val tg = it.target as HTMLInputElement
                                                    val newValue = tg.value
                                                    newProjectInformation[header] = newValue
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    }

                    button(classes = "btn btn-success text-xs p-1 ml-2") {
                        +"Save"
                        attrs.id = "Save new project info"
                        attrs.hidden = true
                        attrs.onClickFunction = {
                            newProjectInformation.forEach { (key, value) ->
                                projectInformation[key] = value
                                (document.getElementById(key) as HTMLInputElement).value = value
                            }
                            updateProjectBuilder(projectInformation)
                            turnEditMode(off = true)
                        }
                    }

                    button(classes = "btn btn-secondary text-xs p-1 ml-5") {
                        +"Cancel"
                        attrs.id = "Cancel"
                        attrs.hidden = true
                        attrs.onClickFunction = {
                            projectInformation.forEach { (key, value) ->
                                (document.getElementById(key) as HTMLInputElement).value = value
                            }
                            newProjectInformation.clear()
                            turnEditMode(off = true)
                        }
                    }

                    div("ml-3 mt-2 align-items-left justify-content-between") {
                        fontAwesomeIcon(icon = faHistory)

                        button(classes = "btn btn-link text-left") {
                            +"Latest Execution"
                            attrs.onClickFunction = {
                                GlobalScope.launch {
                                    switchToLatestExecution()
                                }
                            }
                        }
                    }
                    div("ml-3 align-items-left") {
                        fontAwesomeIcon(icon = faCalendarAlt)
                        a(
                            href = "#/${project.owner}/${project.name}/history",
                            classes = "btn btn-link text-left"
                        ) {
                            +"Execution History"
                        }
                    }
                    div("ml-3 d-sm-flex align-items-left justify-content-between mt-2") {
                        button(type = ButtonType.button, classes = "btn btn-block btn-danger") {
                            attrs.onClickFunction = {
                                deleteProject()
                            }
                            +"Delete project"
                        }
                    }
                })
            }
        }
    }

    private fun postFileUpload(element: HTMLInputElement) =
            GlobalScope.launch {
                setState {
                    isLoading = true
                }
                element.files!!.asList().forEach { file ->
                    val response: FileInfo = post(
                        "${apiUrl}/files/upload",
                        Headers(),
                        FormData().apply {
                            append("file", file)
                        }
                    )
                        .decodeFromJsonString()
                    setState {
                        // add only to selected files so that this entry isn't duplicated
                        files.add(response)
                    }
                }
                setState {
                    isLoading = false
                }
            }

    private fun turnEditMode(off: Boolean) {
        projectInformation.keys.forEach {
            (document.getElementById(it) as HTMLInputElement).disabled = off
        }
        (document.getElementById("Save new project info") as HTMLButtonElement).hidden = off
        (document.getElementById("Cancel") as HTMLButtonElement).hidden = off
    }

    private fun RBuilder.runLoadingModal() = modal {
        attrs {
            isOpen = state.isLoading
            contentLabel = "Loading"
        }
        div("d-flex justify-content-center mt-4") {
            div("spinner-border text-primary spinner-border-lg") {
                attrs.role = "status"
                span("sr-only") {
                    +"Loading..."
                }
            }
        }
    }

    private fun RBuilder.testingTypeButton(selectedTestingType: TestingType, text: String, divClass: String) {
        div(divClass) {
            button(type = ButtonType.button) {
                attrs.classes =
                        if (state.testingType == selectedTestingType) {
                            setOf("btn", "btn-primary")
                        } else {
                            setOf(
                                "btn",
                                "btn-outline-primary"
                            )
                        }
                attrs.onClickFunction = {
                    setState {
                        testingType = selectedTestingType
                    }
                }
                +text
            }
        }
    }

    /**
     * In some cases scripts and binaries can be uploaded to a git repository, so users won't be providing or uploading
     * binaries. For this case we should open a window, so user will need to click a check box, so he will confirm that
     * he understand what he is doing.
     */
    private fun submitWithValidation() {
        setState {
            isSubmitButtonPressed = true
        }
        when {
            // url was not provided
            state.gitUrlFromInputField.isBlank() && state.testingType == TestingType.CUSTOM_TESTS -> setState {
                isErrorOpen = true
                errorMessage =
                        "Git Url with test suites in save format was not provided,but it is required for the testing process." +
                                " Save is not able to run your tests without an information of where to download them from."
                errorLabel = "Git Url"
            }
            // no binaries were provided
            state.files.isEmpty() -> setState {
                confirmationType = ConfirmationType.NO_BINARY_CONFIRM
                isConfirmWindowOpen = true
                confirmLabel = "Single binary confirmation"
                confirmMessage = "You have not provided any files related to your tested tool." +
                        " If these files were uploaded to your repository - press OK, otherwise - please upload these files using 'Upload files' button."
            }
            // everything is in place, can proceed
            else -> submitExecutionRequest()
        }
    }

    private fun deleteProject() {
        project.status = ProjectStatus.DELETED

        setState {
            confirmationType = ConfirmationType.DELETE_CONFIRM
            isConfirmWindowOpen = true
            confirmLabel = ""
            confirmMessage = "Are you sure you want to delete this project?"
        }
    }

    private fun updateProjectBuilder(projectInfo: Map<String, String>) {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        val (name, description, url, owner) = projectInfo.values.toList()
        project.name = name
        project.description = description
        project.url = url
        project.owner = owner
        GlobalScope.launch {
            post("${apiUrl}/updateProject", headers, Json.encodeToString(project))
        }
    }

    private fun deleteProjectBuilder() {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        GlobalScope.launch {
            responseFromDeleteProject =
                    post("${apiUrl}/updateProject", headers, Json.encodeToString(project))
        }.invokeOnCompletion {
            if (responseFromDeleteProject.ok) {
                window.location.href = "${window.location.origin}/"
            } else {
                responseFromDeleteProject.text().then {
                    setState {
                        errorLabel = "Failed to delete project"
                        errorMessage = it
                        isErrorOpen = true
                    }
                }
            }
        }
    }

    private suspend fun switchToLatestExecution() {
        val headers = Headers().apply { set("Accept", "application/json") }
        val response = get(
            "${apiUrl}/latestExecution?name=${project.name}&owner=${project.owner}",
            headers
        )
        if (!response.ok) {
            setState {
                errorLabel = "Failed to fetch latest execution"
                errorMessage =
                        "Failed to fetch latest execution: [${response.status}] ${response.statusText}"
                isErrorOpen = true
            }
        } else {
            val latestExecutionId = response
                .decodeFromJsonString<ExecutionDto>()
                .id
            window.location.href = "${window.location}/history/execution/$latestExecutionId"
        }
    }

    private suspend fun getFilesList() = get("${apiUrl}/files/list", Headers())
        .unsafeMap {
            it.decodeFromJsonString<List<FileInfo>>()
        }

    companion object {
        const val TEST_ROOT_DIR_HINT = """
            The path you are providing should be relative to the root directory of your repository.
            This directory should contain <a href = "https://github.com/cqfn/save#how-to-configure"> save.properties </a>
            or <a href = "https://github.com/cqfn/save#-savetoml-configuration-file">save.toml</a> files. 
            For example, if the URL to your repo with tests is: 
            <a href ="https://github.com/cqfn/save/">https://github.com/cqfn/save</a>, then 
            you need to specify the following directory with 'save.toml': 
            <a href ="https://github.com/cqfn/save/tree/main/examples/kotlin-diktat">examples/kotlin-diktat/</a>. 
 
            Please note, that the tested tool and it's resources will be copied to this directory before the run.
            """
        const val TEST_SUITE_ROW = 4
    }
}
