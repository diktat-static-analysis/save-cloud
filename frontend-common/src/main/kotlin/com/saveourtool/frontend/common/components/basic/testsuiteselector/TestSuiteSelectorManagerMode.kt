/**
 * Component for selecting test suites in manager mode
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.basic.testsuiteselector

import com.saveourtool.common.testsuite.TestSuiteVersioned
import com.saveourtool.frontend.common.components.basic.showAvailableTestSuites
import com.saveourtool.frontend.common.utils.*

import react.FC
import react.Props
import react.dom.html.ReactHTML.h6
import react.useState
import web.cssom.ClassName

val testSuiteSelectorManagerMode = testSuiteSelectorManagerMode()

/**
 * [Props] for [testSuiteSelectorManagerMode] component
 */
external interface TestSuiteSelectorManagerModeProps : Props {
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

@Suppress("TOO_LONG_FUNCTION", "LongMethod", "ComplexMethod")
private fun testSuiteSelectorManagerMode() = FC<TestSuiteSelectorManagerModeProps> { props ->
    val (selectedTestSuites, setSelectedTestSuites) = useState(props.preselectedTestSuites)
    useTooltip()
    if (props.preselectedTestSuites.isEmpty()) {
        h6 {
            className = ClassName("text-center")
            +"No test suites are selected yet."
        }
    } else {
        showAvailableTestSuites(
            props.preselectedTestSuites,
            selectedTestSuites,
            TestSuiteSelectorMode.MANAGER,
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
