/**
 * Component for selecting test suites in search mode
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.basic.testsuiteselector

import com.saveourtool.common.filters.TestSuiteFilter
import com.saveourtool.common.testsuite.TestSuiteVersioned
import com.saveourtool.common.utils.DEFAULT_DEBOUNCE_PERIOD
import com.saveourtool.frontend.common.components.basic.showAvailableTestSuites
import com.saveourtool.frontend.common.components.basic.testsuiteselector.TestSuiteSelectorPurpose.CONTEST
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopResponseHandler

import react.*
import react.dom.events.ChangeEvent
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import web.cssom.ClassName
import web.html.HTMLInputElement
import web.html.InputType

val testSuiteSelectorSearchMode = testSuiteSelectorSearchMode()

/**
 * [Props] for [testSuiteSelectorSearchMode] component
 */
external interface TestSuiteSelectorSearchModeProps : Props {
    /**
     * List of test suites that should be preselected
     */
    var preselectedTestSuites: List<TestSuiteVersioned>

    /**
     * Callback invoked when test suite is being removed
     */
    var onTestSuitesUpdate: (List<TestSuiteVersioned>) -> Unit

    /**
     * Mode that defines what kind of test suites will be shown
     */
    var selectorPurpose: TestSuiteSelectorPurpose

    /**
     * Name of an organization by the name of which test suites are being managed.
     */
    var currentOrganizationName: String
}

private fun ChildrenBuilder.buildInput(
    currentValue: String,
    inputName: String,
    classes: String,
    setValue: (ChangeEvent<HTMLInputElement>) -> Unit
) {
    input {
        className = ClassName("form-control $classes")
        value = currentValue
        placeholder = inputName
        onChange = setValue
    }
}

private fun ChildrenBuilder.showAvailableTestSuitesForSearchMode(
    testSuites: List<TestSuiteVersioned>,
    selectedTestSuites: List<TestSuiteVersioned>,
    isOnlyLatestVersion: Boolean,
    onTestSuiteClick: (TestSuiteVersioned) -> Unit,
) {
    val testSuitesToBeShown = testSuites.filter {
        !isOnlyLatestVersion || it.isLatestFetchedVersion
    }

    showAvailableTestSuites(
        testSuitesToBeShown,
        selectedTestSuites,
        TestSuiteSelectorMode.SEARCH,
        onTestSuiteClick
    )
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod", "ComplexMethod")
private fun testSuiteSelectorSearchMode() = FC<TestSuiteSelectorSearchModeProps> { props ->
    val (selectedTestSuites, setSelectedTestSuites) = useState(props.preselectedTestSuites)
    val (filteredTestSuites, setFilteredTestSuites) = useState<List<TestSuiteVersioned>>(emptyList())
    val (filters, setFilters) = useState(TestSuiteFilter.empty)
    val getFilteredTestSuites = useDebouncedDeferredRequest(DEFAULT_DEBOUNCE_PERIOD) {
        if (filters.isNotEmpty()) {
            val testSuitesFromBackend: List<TestSuiteVersioned> = get(
                url = "$apiUrl/test-suites/${props.currentOrganizationName}/filtered${
                    filters.copy(language = encodeURIComponent(filters.language))
                        .toQueryParams("isContest" to "${props.selectorPurpose == CONTEST}")
                }",
                headers = jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
                .decodeFromJsonString()
            setFilteredTestSuites(testSuitesFromBackend)
        }
    }

    useEffect(filters) {
        if (filters.isEmpty()) {
            setFilteredTestSuites(emptyList())
        } else {
            getFilteredTestSuites()
        }
    }

    div {
        className = ClassName("d-flex justify-content-around mb-2")
        buildInput(filters.name, "Name", "mr-1") { event ->
            setFilters { it.copy(name = event.target.value) }
        }
        buildInput(filters.language, "Language", "ml-1 mr-1") { event ->
            setFilters { it.copy(language = event.target.value) }
        }
        buildInput(filters.tags, "Tags", "ml-1") { event ->
            setFilters { it.copy(tags = event.target.value) }
        }
    }

    val (isOnlyLatestVersion, setIsOnlyLatestVersion) = useState(false)
    div {
        className = ClassName("d-flex justify-content-around mb-3")
        div {
            className = ClassName("form-group form-check")
            input {
                type = InputType.checkbox
                className = ClassName("form-check-input")
                id = "isOnlyLatestVersion"
                checked = isOnlyLatestVersion
                onChange = {
                    setIsOnlyLatestVersion(it.target.checked)
                }
            }
            label {
                className = ClassName("form-check-label")
                htmlFor = "isOnlyLatestVersion"
                +"Show only latest fetched version"
            }
        }
    }
    useTooltip()
    showAvailableTestSuitesForSearchMode(
        filteredTestSuites,
        selectedTestSuites,
        isOnlyLatestVersion,
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
