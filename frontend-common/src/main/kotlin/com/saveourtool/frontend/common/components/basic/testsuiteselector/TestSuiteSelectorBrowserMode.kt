/**
 * Component for selecting test suites in browser mode
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.basic.testsuiteselector

import com.saveourtool.common.testsuite.TestSuiteVersioned
import com.saveourtool.frontend.common.components.basic.showAvailableTestSuites
import com.saveourtool.frontend.common.externals.fontawesome.faCheckDouble
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopResponseHandler

import react.*
import react.dom.aria.AriaRole
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.ol
import react.dom.html.ReactHTML.ul
import web.cssom.ClassName
import web.html.ButtonType

val testSuiteSelectorBrowserMode = testSuiteSelectorBrowserMode()

/**
 * [Props] for [testSuiteSelectorBrowserMode] component
 */
external interface TestSuiteSelectorBrowserModeProps : Props {
    /**
     * Lambda invoked when test suites were successfully set
     */
    var onTestSuitesUpdate: (List<TestSuiteVersioned>) -> Unit

    /**
     * List of test suites that should be preselected
     */
    var preselectedTestSuites: List<TestSuiteVersioned>

    /**
     * Specific organization name which reduces list of test suites source.
     * If null, all the test suites are shown
     */
    var specificOrganizationName: String?

    /**
     * Mode that defines what kind of test suites will be shown
     */
    var selectorPurpose: TestSuiteSelectorPurpose

    /**
     * Name of an organization by the name of which test suites are being managed.
     */
    var currentOrganizationName: String
}

@Suppress(
    "TOO_MANY_PARAMETERS",
    "TOO_LONG_FUNCTION",
    "LongParameterList",
    "LongMethod",
    "ComplexMethod",
)
private fun ChildrenBuilder.showBreadcrumb(
    selectedOrganization: String?,
    selectedTestSuiteSource: String?,
    selectedTestSuiteVersion: String?,
    shouldDisplayVersion: Boolean,
    onOrganizationsClick: () -> Unit,
    onSelectedOrganizationClick: () -> Unit,
    onSelectedTestSuiteSourceClick: () -> Unit,
) {
    nav {
        ariaLabel = "breadcrumb"
        ol {
            className = ClassName("breadcrumb")
            li {
                className = ClassName("breadcrumb-item")
                a {
                    role = "button".unsafeCast<AriaRole>()
                    className = selectedOrganization?.let { ClassName("btn-link") }
                    onClick = {
                        selectedOrganization?.let {
                            onOrganizationsClick()
                        }
                    }
                    +"organizations"
                }
            }
            selectedOrganization?.let {
                li {
                    val isActive = selectedTestSuiteSource?.let { "" } ?: "active"
                    className = ClassName("breadcrumb-item $isActive")
                    a {
                        role = "button".unsafeCast<AriaRole>()
                        className = selectedTestSuiteSource?.let { ClassName("btn-link") }
                        onClick = {
                            selectedTestSuiteSource?.let {
                                onSelectedOrganizationClick()
                            }
                        }
                        +selectedOrganization
                    }
                }
            }
            selectedTestSuiteSource?.let {
                li {
                    val isActive = selectedTestSuiteVersion?.let { "" } ?: "active"
                    className = ClassName("breadcrumb-item $isActive")
                    a {
                        role = "button".unsafeCast<AriaRole>()
                        className = selectedTestSuiteVersion?.let { ClassName("btn-link") }
                        onClick = {
                            if (shouldDisplayVersion) {
                                selectedTestSuiteVersion?.let {
                                    onSelectedTestSuiteSourceClick()
                                }
                            }
                        }
                        +selectedTestSuiteSource
                    }
                }
            }
            if (shouldDisplayVersion) {
                selectedTestSuiteVersion?.let {
                    li {
                        className = ClassName("breadcrumb-item active")
                        a {
                            role = "button".unsafeCast<AriaRole>()
                            +selectedTestSuiteVersion
                        }
                    }
                }
            }
        }
    }
}

private fun ChildrenBuilder.showAvailableOptions(
    options: List<String>,
    onOptionClick: (String) -> Unit,
) {
    ul {
        className = ClassName("list-group")
        if (options.isEmpty()) {
            li {
                className = ClassName("list-group-item")
                +"There is no test suite that you can manage."
            }
        } else {
            options.forEach { option ->
                a {
                    className = ClassName("btn text-center list-group-item list-group-item-action")
                    onClick = {
                        onOptionClick(option)
                    }
                    +option
                }
            }
        }
    }
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod", "ComplexMethod")
private fun testSuiteSelectorBrowserMode() = FC<TestSuiteSelectorBrowserModeProps> { props ->
    useTooltip()
    val (selectedOrganization, setSelectedOrganization) = useState<String?>(null)
    val (selectedTestSuiteSource, setSelectedTestSuiteSource) = useState<String?>(null)
    val (selectedTestSuiteVersion, setSelectedTestSuiteVersion) = useState<String?>(null)
    val (selectedTestSuites, setSelectedTestSuites) = useState<List<TestSuiteVersioned>>(emptyList())

    val (availableOrganizations, setAvailableOrganizations) = useState<List<String>>(emptyList())
    val (availableTestSuiteSources, setAvailableTestSuiteSources) = useState<List<String>>(emptyList())
    val (availableTestSuitesVersions, setAvailableTestSuitesVersions) = useState<List<String>>(emptyList())
    val (availableTestSuites, setAvailableTestSuites) = useState<List<TestSuiteVersioned>>(emptyList())
    val (fetchedTestSuites, setFetchedTestSuites) = useState<List<TestSuiteVersioned>>(emptyList())
    useRequest {
        val options = when (props.selectorPurpose) {
            TestSuiteSelectorPurpose.PUBLIC -> "?permission=READ"
            TestSuiteSelectorPurpose.PRIVATE -> "?permission=WRITE"
            TestSuiteSelectorPurpose.CONTEST -> "?permission=READ&isContest=true"
        }
        val response = get(
            url = "$apiUrl/test-suites/${props.currentOrganizationName}/available$options",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        )

        val testSuites: List<TestSuiteVersioned> = response.decodeFromJsonString()
        setFetchedTestSuites(testSuites)
        setAvailableOrganizations(testSuites.map { it.organizationName }.distinct())
    }

    useEffect(selectedOrganization) {
        selectedOrganization?.let { selectedOrganization ->
            setAvailableTestSuiteSources(
                fetchedTestSuites
                    .filter { it.organizationName == selectedOrganization }
                    .map { it.sourceName }
                    .distinct()
            )
        } ?: setAvailableTestSuiteSources(emptyList())
    }

    useEffect(selectedTestSuiteSource) {
        selectedTestSuiteSource?.let { selectedTestSuiteSource ->
            setAvailableTestSuitesVersions(
                fetchedTestSuites.filter { it.sourceName == selectedTestSuiteSource }
                    .map { it.version }
                    .distinct()
            )
        } ?: setAvailableTestSuitesVersions(emptyList())
    }

    useEffect(selectedTestSuiteVersion) {
        selectedTestSuiteVersion?.let { selectedTestSuiteVersion ->
            setAvailableTestSuites(
                fetchedTestSuites.filter {
                    it.sourceName == selectedTestSuiteSource && it.version == selectedTestSuiteVersion
                }
            )
        } ?: setAvailableTestSuites(emptyList())
    }

    val (namePrefix, setNamePrefix) = useState("")
    div {
        // ==================== BREADCRUMB ====================
        className = ClassName("")
        showBreadcrumb(
            selectedOrganization,
            selectedTestSuiteSource,
            selectedTestSuiteVersion,
            availableTestSuitesVersions.size > 1,
            onOrganizationsClick = {
                setSelectedOrganization(null)
                setSelectedTestSuiteSource(null)
                setSelectedTestSuiteVersion(null)
                setNamePrefix("")
            },
            onSelectedOrganizationClick = {
                setSelectedTestSuiteSource(null)
                setSelectedTestSuiteVersion(null)
                setNamePrefix("")
            }
        ) {
            setSelectedTestSuiteVersion(null)
            setNamePrefix("")
        }
        // ==================== TOOLBAR ====================
        div {
            className = ClassName("d-flex justify-content-center mb-2")
            input {
                className = ClassName("form-control")
                value = namePrefix
                placeholder = selectedOrganization?.let {
                    selectedTestSuiteSource?.let {
                        selectedTestSuiteVersion?.let {
                            "Test suite name"
                        } ?: "Test suite version name"
                    } ?: "Test suite source name"
                } ?: "Organization name"
                onChange = {
                    setNamePrefix(it.target.value)
                }
            }
            selectedTestSuiteVersion?.let {
                val active = if (selectedTestSuites.containsAll(availableTestSuites)) {
                    "active"
                } else {
                    ""
                }
                button {
                    type = ButtonType.button
                    className = ClassName("btn btn-outline-secondary $active")
                    asDynamic()["data-toggle"] = "tooltip"
                    asDynamic()["data-placement"] = "bottom"
                    title = "Select all"
                    onClick = {
                        setSelectedTestSuites { selectedTestSuites ->
                            if (selectedTestSuites.containsAll(availableTestSuites)) {
                                selectedTestSuites.filter { it !in availableTestSuites }
                            } else {
                                selectedTestSuites.toMutableList()
                                    .apply {
                                        addAll(availableTestSuites)
                                    }
                                    .distinctBy { it.id }
                            }
                                .also { testSuites ->
                                    props.onTestSuitesUpdate(testSuites)
                                }
                        }
                    }
                    fontAwesomeIcon(faCheckDouble)
                }
            }
        }
        // ==================== SELECTOR ====================
        div {
            className = ClassName("")
            when {
                selectedOrganization == null -> showAvailableOptions(
                    availableOrganizations.filter { it.contains(namePrefix, true) }
                ) { organization ->
                    setSelectedOrganization(organization)
                    setNamePrefix("")
                }
                selectedTestSuiteSource == null -> showAvailableOptions(
                    availableTestSuiteSources.filter { it.contains(namePrefix, true) }
                ) { testSuiteSource ->
                    setSelectedTestSuiteSource(testSuiteSource)
                    setNamePrefix("")
                }
                selectedTestSuiteVersion == null -> showAvailableOptions(
                    availableTestSuitesVersions.filter { it.contains(namePrefix, true) }
                ) { testSuiteVersion ->
                    setSelectedTestSuiteVersion(testSuiteVersion)
                    setNamePrefix("")
                }
                else -> showAvailableTestSuites(
                    availableTestSuites.filter { it.name.contains(namePrefix, true) },
                    selectedTestSuites,
                    TestSuiteSelectorMode.BROWSER,
                ) { testSuite ->
                    setSelectedTestSuites { selectedTestSuites ->
                        selectedTestSuites.toMutableList()
                            .apply {
                                if (testSuite in selectedTestSuites) {
                                    remove(testSuite)
                                } else {
                                    add(testSuite)
                                }
                            }
                            .toList()
                            .also { listOfTestSuiteDtos ->
                                props.onTestSuitesUpdate(listOfTestSuiteDtos)
                            }
                    }
                }
            }
        }
    }
}
