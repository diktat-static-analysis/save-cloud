@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "TOP_LEVEL_ORDER"
)

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.domain.Role
import com.saveourtool.common.test.*
import com.saveourtool.common.testsuite.*
import com.saveourtool.frontend.common.components.basic.testsuitespermissions.PermissionManagerMode
import com.saveourtool.frontend.common.components.basic.testsuitespermissions.manageTestSuitePermissionsComponent
import com.saveourtool.frontend.common.components.basic.testsuitessources.fetch.testSuitesSourceFetcher
import com.saveourtool.frontend.common.components.basic.testsuitessources.showTestSuiteSourceUpsertModal
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.WithRequestStatusContext
import com.saveourtool.frontend.common.utils.loadingHandler
import com.saveourtool.frontend.common.utils.useTooltip
import com.saveourtool.frontend.common.utils.useWindowOpenness

import react.*
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

import kotlinx.browser.window

/**
 * TESTS tab in OrganizationView
 */
val organizationTestsMenu = organizationTestsMenu()

/**
 * OrganizationTestsMenu component props
 */
external interface OrganizationTestsMenuProps : Props {
    /**
     * Current organization name
     */
    var organizationName: String

    /**
     * [Role] of user that is observing this component
     */
    var selfRole: Role
}

private suspend fun WithRequestStatusContext.getTestSuitesSourcesWithId(
    organizationName: String,
) = get(
    url = "$apiUrl/test-suites-sources/$organizationName/list",
    headers = jsonHeaders,
    loadingHandler = ::loadingHandler,
)

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun organizationTestsMenu() = FC<OrganizationTestsMenuProps> { props ->
    useTooltip()
    val testSuitesSourceUpsertWindowOpenness = useWindowOpenness()
    val (isSourceCreated, setIsSourceCreated) = useState(false)
    val (testSuitesSources, setTestSuitesSources) = useState(emptyList<TestSuitesSourceDto>())
    useRequest(dependencies = arrayOf(props.organizationName, isSourceCreated)) {
        val response = getTestSuitesSourcesWithId(props.organizationName)
        if (response.ok) {
            setTestSuitesSources(response.decodeFromJsonString<TestSuitesSourceDtoList>())
        } else {
            setTestSuitesSources(emptyList())
        }
    }
    val (testSuiteSourceToFetch, setTestSuiteSourceToFetch) = useState<TestSuitesSourceDto>()
    val testSuitesSourceFetcherWindowOpenness = useWindowOpenness()
    testSuitesSourceFetcher(
        testSuitesSourceFetcherWindowOpenness,
        testSuiteSourceToFetch ?: TestSuitesSourceDto.empty
    )

    val (selectedTestSuitesSource, setSelectedTestSuitesSource) = useState<TestSuitesSourceDto>()
    val (testsSourceVersionInfoList, setTestsSourceVersionInfoList) = useState(emptyList<TestsSourceVersionInfo>())
    val fetchTestsSourcesVersionInfoList = useDeferredRequest {
        selectedTestSuitesSource?.let { testSuitesSource ->
            val response = get(
                url = "$apiUrl/test-suites-sources/${testSuitesSource.organizationName}/${encodeURIComponent(testSuitesSource.name)}/list-version",
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
            )
            if (response.ok) {
                response.unsafeMap {
                    it.decodeFromJsonString<TestsSourceVersionInfoList>()
                }.let {
                    setTestsSourceVersionInfoList(it)
                }
            } else {
                setTestsSourceVersionInfoList(emptyList())
            }
        }
    }

    val (testsSourceVersionInfoToDelete, setTestsSourceVersionInfoToDelete) = useState<TestsSourceVersionInfo>()
    val deleteTestSuitesSourcesSnapshotKey = useDeferredRequest {
        testsSourceVersionInfoToDelete?.let { key ->
            delete(
                url = with(key) {
                    "$apiUrl/test-suites-sources/$organizationName/${encodeURIComponent(sourceName)}/delete-version?version=$version"
                },
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
            )
            setTestsSourceVersionInfoToDelete(null)
        }
    }
    val selectHandler: (TestSuitesSourceDto) -> Unit = {
        if (selectedTestSuitesSource == it) {
            setSelectedTestSuitesSource(null)
        } else {
            setSelectedTestSuitesSource(it)
            fetchTestsSourcesVersionInfoList()
        }
    }
    val fetchHandler: (TestSuitesSourceDto) -> Unit = {
        setTestSuiteSourceToFetch(it)
        testSuitesSourceFetcherWindowOpenness.openWindow()
    }
    val (testSuiteSourceToUpsert, setTestSuiteSourceToUpsert) = useState<TestSuitesSourceDto>()
    val editHandler: (TestSuitesSourceDto) -> Unit = {
        setTestSuiteSourceToUpsert(it)
        testSuitesSourceUpsertWindowOpenness.openWindow()
    }
    val deleteHandler: (TestsSourceVersionInfo) -> Unit = {
        if (window.confirm("Are you sure you want to delete snapshot ${it.version} of ${it.sourceName}?")) {
            setTestsSourceVersionInfoToDelete(it)
            deleteTestSuitesSourcesSnapshotKey()
            setTestsSourceVersionInfoList(testsSourceVersionInfoList.filterNot(it::equals))
        }
    }
    val refreshTestSuitesSources = useDeferredRequest {
        val response = getTestSuitesSourcesWithId(props.organizationName)
        if (response.ok) {
            setTestSuitesSources(response.decodeFromJsonString<TestSuitesSourceDtoList>())
        } else {
            setTestSuitesSources(emptyList())
        }
    }
    val refreshHandler: () -> Unit = {
        refreshTestSuitesSources()
        fetchTestsSourcesVersionInfoList()
    }
    val (managePermissionsMode, setManagePermissionsMode) = useState<PermissionManagerMode?>(null)
    manageTestSuitePermissionsComponent {
        organizationName = props.organizationName
        isModalOpen = managePermissionsMode != null
        closeModal = { setManagePermissionsMode(null) }
        mode = managePermissionsMode
    }
    showTestSuiteSourceUpsertModal(
        windowOpenness = testSuitesSourceUpsertWindowOpenness,
        testSuitesSource = testSuiteSourceToUpsert,
        organizationName = props.organizationName,
    ) {
        setIsSourceCreated { !it }
    }

    div {
        className = ClassName("d-flex justify-content-center mb-3")
        buttonBuilder("+ Create test suites source", "primary", !props.selfRole.hasWritePermission(), classes = "btn-sm mr-2") {
            testSuitesSourceUpsertWindowOpenness.openWindow()
        }
        buttonBuilder("Manage permissions", "info", !props.selfRole.hasWritePermission(), classes = "btn-sm mr-2 ml-2") {
            setManagePermissionsMode(PermissionManagerMode.TRANSFER)
        }
        buttonBuilder("Publish test suites", "info", !props.selfRole.hasWritePermission(), classes = "btn-sm ml-2") {
            setManagePermissionsMode(PermissionManagerMode.PUBLISH)
        }
    }

    div {
        className = ClassName("mb-2 d-flex justify-content-center")
        when (selectedTestSuitesSource) {
            null -> showTestSuitesSources(testSuitesSources, selectHandler, fetchHandler, editHandler, refreshHandler)
            else -> showTestsSourceVersionInfoList(
                selectedTestSuitesSource,
                testsSourceVersionInfoList,
                selectHandler,
                editHandler,
                fetchHandler,
                deleteHandler,
                refreshHandler,
            )
        }
    }
}
