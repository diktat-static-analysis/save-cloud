/**
 * Component for selecting test suites
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.basic.testsuiteselector

import com.saveourtool.common.testsuite.TestSuiteVersioned
import com.saveourtool.frontend.common.components.modal.largeTransparentModalStyle
import com.saveourtool.frontend.common.components.modal.modal
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.utils.WindowOpenness
import com.saveourtool.frontend.common.utils.buttonWithIcon
import com.saveourtool.frontend.common.utils.useTooltip

import react.*
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import web.cssom.ClassName
import web.html.ButtonType

val testSuiteSelector = testSuiteSelector()

/**
 * [Props] for [testSuiteSelector] component
 */
external interface TestSuiteSelectorProps : Props {
    /**
     * Lambda invoked when test suites were successfully set
     */
    var onTestSuiteUpdate: (List<TestSuiteVersioned>) -> Unit

    /**
     * List of test suite ids that should be preselected
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
     * Name of a current organization (by which test suite selection is happening)
     */
    var currentOrganizationName: String
}

/**
 * Enum that represents different modes of [testSuiteSelector]
 */
enum class TestSuiteSelectorMode {
    BROWSER,
    MANAGER,
    SEARCH,
    ;
}

/**
 * Enum that defines what type of test suites should be shown
 */
enum class TestSuiteSelectorPurpose {
    CONTEST,
    PRIVATE,
    PUBLIC,
    ;
}

/**
 * Browse all the available test suites.
 *
 * @param currentOrganizationName
 * @param initTestSuites initial value
 * @param windowOpenness state to control openness of window
 * @param testSuitesInSelectorState state for intermediate result in selector
 * @param setSelectedTestSuiteIds consumer for result
 */
@Suppress("TYPE_ALIAS")
fun ChildrenBuilder.showPublicTestSuitesSelectorModal(
    currentOrganizationName: String,
    initTestSuites: List<TestSuiteVersioned>,
    windowOpenness: WindowOpenness,
    testSuitesInSelectorState: StateInstance<List<TestSuiteVersioned>>,
    setSelectedTestSuiteIds: (List<TestSuiteVersioned>) -> Unit,
) {
    showTestSuitesSelectorModal(currentOrganizationName,
        TestSuiteSelectorPurpose.PUBLIC, initTestSuites, windowOpenness, testSuitesInSelectorState, setSelectedTestSuiteIds)
}

/**
 * Browse test suites of a given organization.
 *
 * @param currentOrganizationName
 * @param initTestSuites initial value
 * @param windowOpenness state to control openness of window
 * @param testSuitesInSelectorState state for intermediate result in selector
 * @param setSelectedTestSuites consumer for result
 */
@Suppress("TYPE_ALIAS")
fun ChildrenBuilder.showPrivateTestSuitesSelectorModal(
    currentOrganizationName: String,
    initTestSuites: List<TestSuiteVersioned>,
    windowOpenness: WindowOpenness,
    testSuitesInSelectorState: StateInstance<List<TestSuiteVersioned>>,
    setSelectedTestSuites: (List<TestSuiteVersioned>) -> Unit,
) {
    showTestSuitesSelectorModal(currentOrganizationName,
        TestSuiteSelectorPurpose.PRIVATE, initTestSuites, windowOpenness, testSuitesInSelectorState,
        setSelectedTestSuites)
}

/**
 * Browse test suites for a contest.
 *
 * @param currentOrganizationName
 * @param initTestSuites initial value
 * @param windowOpenness state to control openness of window
 * @param testSuitesInSelectorState state for intermediate result in selector
 * @param setSelectedTestSuites consumer for result
 */
@Suppress("TYPE_ALIAS")
fun ChildrenBuilder.showContestTestSuitesSelectorModal(
    currentOrganizationName: String,
    initTestSuites: List<TestSuiteVersioned>,
    windowOpenness: WindowOpenness,
    testSuitesInSelectorState: StateInstance<List<TestSuiteVersioned>>,
    setSelectedTestSuites: (List<TestSuiteVersioned>) -> Unit,
) {
    showTestSuitesSelectorModal(currentOrganizationName,
        TestSuiteSelectorPurpose.CONTEST, initTestSuites, windowOpenness, testSuitesInSelectorState,
        setSelectedTestSuites)
}

@Suppress("TOO_MANY_PARAMETERS", "LongParameterList", "TYPE_ALIAS")
private fun ChildrenBuilder.showTestSuitesSelectorModal(
    currentOrganizationName: String,
    selectorPurpose: TestSuiteSelectorPurpose,
    initTestSuites: List<TestSuiteVersioned>,
    windowOpenness: WindowOpenness,
    testSuitesInSelectorState: StateInstance<List<TestSuiteVersioned>>,
    setSelectedTestSuites: (List<TestSuiteVersioned>) -> Unit,
) {
    var currentlySelectedTestSuites by testSuitesInSelectorState
    val onSubmit: () -> Unit = {
        setSelectedTestSuites(currentlySelectedTestSuites)
        windowOpenness.closeWindow()
    }
    val onTestSuiteIdUpdate: (List<TestSuiteVersioned>) -> Unit = {
        currentlySelectedTestSuites = it
    }
    val onCancel: () -> Unit = {
        currentlySelectedTestSuites = initTestSuites
        windowOpenness.closeWindow()
    }
    showTestSuitesSelectorModal(windowOpenness.isOpen(), currentOrganizationName, selectorPurpose, initTestSuites, currentlySelectedTestSuites, onSubmit, onTestSuiteIdUpdate,
        onCancel)
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "TOO_MANY_PARAMETERS",
    "LongParameterList"
)
private fun ChildrenBuilder.showTestSuitesSelectorModal(
    isOpen: Boolean,
    currentOrganizationName: String,
    selectorPurpose: TestSuiteSelectorPurpose,
    preselectedTestSuites: List<TestSuiteVersioned>,
    currentlySelectedTestSuites: List<TestSuiteVersioned>,
    onSubmit: () -> Unit,
    onTestSuitesUpdate: (List<TestSuiteVersioned>) -> Unit,
    onCancel: () -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = largeTransparentModalStyle
        div {
            className = ClassName("modal-dialog modal-lg modal-dialog-scrollable")
            div {
                className = ClassName("modal-content")
                div {
                    className = ClassName("modal-header")
                    h5 {
                        className = ClassName("modal-title mb-0")
                        +"Test suite selector"
                    }
                    button {
                        type = ButtonType.button
                        className = ClassName("close")
                        asDynamic()["data-dismiss"] = "modal"
                        ariaLabel = "Close"
                        fontAwesomeIcon(icon = faTimesCircle)
                        onClick = {
                            onCancel()
                        }
                    }
                }

                div {
                    className = ClassName("modal-body")
                    testSuiteSelector {
                        this.onTestSuiteUpdate = onTestSuitesUpdate
                        this.preselectedTestSuites = preselectedTestSuites
                        this.selectorPurpose = selectorPurpose
                        this.currentOrganizationName = currentOrganizationName
                    }
                }

                div {
                    className = ClassName("modal-footer")
                    div {
                        className = ClassName("d-flex justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-outline-primary mt-4")
                            +"Apply"
                            disabled = currentlySelectedTestSuites.isEmpty()
                            onClick = {
                                onSubmit()
                            }
                        }
                    }
                    div {
                        className = ClassName("d-flex justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-secondary mt-4")
                            +"Cancel"
                            onClick = {
                                onCancel()
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun testSuiteSelector() = FC<TestSuiteSelectorProps> { props ->
    val (currentMode, setCurrentMode) = useState(if (props.preselectedTestSuites.isEmpty()) {
        TestSuiteSelectorMode.BROWSER
    } else {
        TestSuiteSelectorMode.MANAGER
    })
    div {
        className = ClassName("d-flex align-self-center justify-content-around mb-2")
        buttonWithIcon(faAlignJustify, currentMode == TestSuiteSelectorMode.MANAGER, "Manage linked test suites") {
            setCurrentMode(
                TestSuiteSelectorMode.MANAGER
            )
        }
        buttonWithIcon(faPlus, currentMode == TestSuiteSelectorMode.BROWSER, "Browse public test suites") {
            setCurrentMode(
                TestSuiteSelectorMode.BROWSER
            )
        }
        buttonWithIcon(faSearch, currentMode == TestSuiteSelectorMode.SEARCH, "Search by name or tag") {
            setCurrentMode(
                TestSuiteSelectorMode.SEARCH
            )
        }
    }

    useTooltip()

    when (currentMode) {
        TestSuiteSelectorMode.MANAGER -> testSuiteSelectorManagerMode {
            this.onTestSuitesUpdate = props.onTestSuiteUpdate
            this.preselectedTestSuites = props.preselectedTestSuites
            this.selectorPurpose = props.selectorPurpose
            this.currentOrganizationName = props.currentOrganizationName
        }
        TestSuiteSelectorMode.BROWSER -> testSuiteSelectorBrowserMode {
            this.onTestSuitesUpdate = props.onTestSuiteUpdate
            this.preselectedTestSuites = props.preselectedTestSuites
            this.specificOrganizationName = props.specificOrganizationName
            this.selectorPurpose = props.selectorPurpose
            this.currentOrganizationName = props.currentOrganizationName
        }
        TestSuiteSelectorMode.SEARCH -> testSuiteSelectorSearchMode {
            this.onTestSuitesUpdate = props.onTestSuiteUpdate
            this.preselectedTestSuites = props.preselectedTestSuites
            this.selectorPurpose = props.selectorPurpose
            this.currentOrganizationName = props.currentOrganizationName
        }
    }
}
