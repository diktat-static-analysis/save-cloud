@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.domain.Sdk
import com.saveourtool.common.entities.FileDto
import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.common.execution.TestingType
import com.saveourtool.common.request.CreateExecutionRequest
import com.saveourtool.common.testsuite.TestSuiteVersioned
import com.saveourtool.frontend.common.components.basic.cardComponent
import com.saveourtool.frontend.common.externals.fontawesome.faCalendarAlt
import com.saveourtool.frontend.common.externals.fontawesome.faHistory
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.apiUrl
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.basic.fileuploader.simpleFileUploader

import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import web.cssom.ClassName

import kotlinx.browser.window

private val typeSelection = cardComponent()

private val projectInfoCard = cardComponent(isBordered = true, hasBg = true)

/**
 * RUN tab in ProjectView
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
val projectRunMenu: FC<ProjectRunMenuProps> = FC { props ->
    val (project, setProject) = useState(ProjectDto.empty)
    useRequest {
        val projectFromBackend: ProjectDto = get(
            url = "$apiUrl/projects/get/organization-name?name=${props.projectName}&organizationName=${props.organizationName}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }
        setProject(projectFromBackend)
    }

    val (selectedContest, setSelectedContest) = useState(ContestDto.empty)
    val (availableContests, setAvailableContests) = useState<List<ContestDto>>(emptyList())
    useRequest {
        val contests: List<ContestDto> = get(
            "$apiUrl/contests/active",
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }
        setAvailableContests(contests)
        contests.firstOrNull()?.let {
            setSelectedContest(it)
        }
    }

    val (testingType, setTestingType) = useState(TestingType.PRIVATE_TESTS)
    val (files, setFiles) = useState<List<FileDto>>(emptyList())

    val (selectedSdk, setSelectedSdk) = useState<Sdk>(Sdk.Default)
    val (execCmd, setExecCmd) = useState("")
    val (batchSizeForAnalyzer, setBatchSizeForAnalyzer) = useState("1")

    val (selectedPrivateTestSuites, setSelectedPrivateTestSuites) = useState<List<TestSuiteVersioned>>(emptyList())
    val (selectedPublicTestSuites, setSelectedPublicTestSuites) = useState<List<TestSuiteVersioned>>(emptyList())

    val buildExecutionRequest: () -> CreateExecutionRequest = {
        val (selectedTestSuiteIds, testsVersion) = when (testingType) {
            TestingType.PRIVATE_TESTS -> selectedPrivateTestSuites.extractIdsAndVersion()
            TestingType.PUBLIC_TESTS -> selectedPublicTestSuites.extractIdsAndVersion()
            TestingType.CONTEST_MODE -> selectedContest.testSuites.map { it.id } to null
        }
        CreateExecutionRequest(
            projectCoordinates = ProjectCoordinates(
                organizationName = project.organizationName,
                projectName = project.name
            ),
            testSuiteIds = selectedTestSuiteIds,
            fileIds = files.map { it.requiredId() },
            sdk = selectedSdk,
            execCmd = execCmd.takeUnless { it.isBlank() },
            batchSizeForAnalyzer = batchSizeForAnalyzer.takeUnless { it.isBlank() },
            testingType = testingType,
            contestName = testingType.takeIf { it == TestingType.CONTEST_MODE }?.let { selectedContest.name },
            testsVersion = testsVersion,
        )
    }

    @Suppress("TYPE_ALIAS")
    val displayTestingTypeButton: ChildrenBuilder.(TestingType, String, String) -> Unit = { selectedTestingType, text, divClass ->
        testingTypeButton(selectedTestingType, text, divClass, testingType) {
            setTestingType(it)
        }
    }

    val isRunButtonDisabled: () -> Boolean = {
        files.isEmpty() || (testingType == TestingType.PRIVATE_TESTS && selectedPrivateTestSuites.isEmpty()) ||
                (testingType == TestingType.PUBLIC_TESTS && selectedPublicTestSuites.isEmpty())
    }

    div {
        className = ClassName("row justify-content-center ml-5")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-2 mr-3")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Testing types"
            }

            typeSelection {
                div {
                    className = ClassName("text-left")
                    displayTestingTypeButton(
                        TestingType.PRIVATE_TESTS,
                        "Evaluate your tool with your own tests",
                        "mr-2",
                    )
                    displayTestingTypeButton(
                        TestingType.PUBLIC_TESTS,
                        "Evaluate your tool with public test suites",
                        "mt-3 mr-2",
                    )
                    if (project.isPublic) {
                        displayTestingTypeButton(
                            TestingType.CONTEST_MODE,
                            "Participate in SAVE contests with your tool",
                            "mt-3 mr-2",
                        )
                    }
                }
            }
        }
        // ===================== MIDDLE COLUMN =====================================================================
        div {
            className = ClassName("col-4")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Test configuration"
            }

            // ======== file selector =========
            div {
                className = ClassName("mb-2")
                label {
                    className =
                            ClassName("control-label col-auto justify-content-between font-weight-bold text-gray-800 mb-1 pl-0")
                    +"1. Upload your tool-related files on FILES tab and select them for testing:"
                }
                simpleFileUploader {
                    isDisabled = false
                    fileDtosSetter = setFiles
                    getUrlForSelectedFilesFetch = null
                    getUrlForAvailableFilesFetch = {
                        "$apiUrl/files/${props.organizationName}/${props.projectName}/list"
                    }
                }
            }

            // ======== sdk selection =========
            sdkSelection {
                title = "2. Select the SDK if needed:"
                this.selectedSdk = selectedSdk
                onSdkChange = { setSelectedSdk(it) }
            }

            // ======== test resources selection =========
            testResourcesSelection {
                this.testingType = testingType
                // properties for CONTEST_TESTS mode
                projectName = props.projectName
                organizationName = props.organizationName
                onContestEnrollerResponse = { window.alert(it) }
                this.selectedContest = selectedContest
                this.setSelectedContest = { setSelectedContest(it) }
                this.availableContests = availableContests
                // properties for PRIVATE_TESTS mode
                this.selectedPrivateTestSuites = selectedPrivateTestSuites
                this.setSelectedPrivateTestSuites = { setSelectedPrivateTestSuites(it) }
                // properties for PUBLIC_TESTS mode
                this.selectedPublicTestSuites = selectedPublicTestSuites
                this.setSelectedPublicTestSuites = { setSelectedPublicTestSuites(it) }
                // properties for PRIVATE_TESTS and PUBLIC_TESTS modes
                this.execCmd = execCmd
                this.setExecCmd = { setExecCmd(it) }
                this.batchSizeForAnalyzer = batchSizeForAnalyzer
                this.setBatchSizeForAnalyzer = { value ->
                    setBatchSizeForAnalyzer(value)
                }
            }

            div {
                className = ClassName("d-sm-flex align-items-center justify-content-center")
                withNavigate { navigateContext ->
                    buttonBuilder("Test the tool now", isDisabled = isRunButtonDisabled()) {
                        props.submitExecutionRequest(navigateContext, buildExecutionRequest())
                    }
                }
            }
        }
        // ===================== RIGHT COLUMN ======================================================================
        div {
            className = ClassName("col-3 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Information"
            }

            projectInfoCard {
                projectInfo {
                    this.project = project
                    this.onProjectUpdate = { setProject(it) }
                }

                div {
                    className = ClassName("ml-3 mt-2 align-items-left justify-content-between")
                    fontAwesomeIcon(icon = faHistory)
                    withNavigate { navigateContext ->
                        buttonBuilder("Latest Execution", "link", isDisabled = props.latestExecutionId == null, classes = "text-left") {
                            navigateContext.navigate("history/execution/${props.latestExecutionId}")
                        }
                    }
                }
                div {
                    className = ClassName("ml-3 align-items-left")
                    fontAwesomeIcon(icon = faCalendarAlt)
                    withNavigate { navigateContext ->
                        buttonBuilder("Execution History", "link", classes = "text-left") {
                            navigateContext.navigate("history")
                        }
                    }
                }
            }
        }
    }
}

/**
 * ProjectRunMenu component props
 */
external interface ProjectRunMenuProps : Props {
    /**
     * Project name
     */
    var projectName: String

    /**
     * Organization name
     */
    var organizationName: String

    /**
     * ID of the latest execution
     */
    var latestExecutionId: Long?

    /**
     * Callback to send execution request
     */
    @Suppress("TYPE_ALIAS")
    var submitExecutionRequest: (NavigateFunctionContext, CreateExecutionRequest) -> Unit
}

private fun ChildrenBuilder.testingTypeButton(
    selectedTestingType: TestingType,
    text: String,
    divClass: String,
    testingType: TestingType,
    setTestingType: (TestingType) -> Unit,
) {
    div {
        className = ClassName(divClass)
        buttonBuilder(text, isOutline = true, isActive = testingType == selectedTestingType) {
            setTestingType(selectedTestingType)
        }
    }
}

private fun Collection<TestSuiteVersioned>.extractIdsAndVersion(): Pair<List<Long>, String?> = this.map(TestSuiteVersioned::id) to this.map(TestSuiteVersioned::version)
    .distinct()
    .single()
    .takeIf { it.isNotEmpty() }
