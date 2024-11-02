/**
 * Utility methods for creation of the module window for the selection of test resources
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.common.execution.TestingType
import com.saveourtool.common.testsuite.TestSuiteVersioned
import com.saveourtool.frontend.common.components.basic.testsuiteselector.showPrivateTestSuitesSelectorModal
import com.saveourtool.frontend.common.components.basic.testsuiteselector.showPublicTestSuitesSelectorModal
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.inputform.inputTextFormRequired
import com.saveourtool.frontend.common.utils.*

import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import web.cssom.ClassName
import web.html.ButtonType

val testResourcesSelection = prepareTestResourcesSelection()

/**
 * Properties for test resources
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface TestResourcesProps : PropsWithChildren {
    var testingType: TestingType

    // properties for CONTEST_MODE
    var projectName: String
    var organizationName: String
    var onContestEnrollerResponse: (String) -> Unit
    var availableContests: List<ContestDto>
    var selectedContest: ContestDto
    var setSelectedContest: (ContestDto) -> Unit

    // properties for PRIVATE_TESTS mode
    var selectedPrivateTestSuites: List<TestSuiteVersioned>
    var setSelectedPrivateTestSuites: (List<TestSuiteVersioned>) -> Unit

    // properties for PUBLIC_TESTS mode
    var selectedPublicTestSuites: List<TestSuiteVersioned>
    var setSelectedPublicTestSuites: (List<TestSuiteVersioned>) -> Unit
    var execCmd: String
    var setExecCmd: (String) -> Unit
    var batchSizeForAnalyzer: String
    var setBatchSizeForAnalyzer: (String) -> Unit
}

@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
private fun ChildrenBuilder.addAdditionalProperty(
    value: String,
    placeholder: String,
    tooltipText: String,
    labelText: String,
    isOnlyNumbers: Boolean,
    onChangeFunc: (String) -> Unit
) = div {
    className = ClassName("input-group mb-3")
    if (labelText.isNotEmpty()) {
        div {
            className = ClassName("input-group-prepend")
            label {
                className = ClassName("input-group-text")
                +labelText
            }
        }
    }

    input {
        // workaround to have a default value for Batch field
        key = "itemText"
        className = ClassName("form-control")
        if (tooltipText.isNotBlank()) {
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "right"
            title = tooltipText
        }
        this.value = value
        this.placeholder = placeholder
        onChange = { event ->
            event.target.value.let { value ->
                if (!isOnlyNumbers || value.all { it.isDigit() }) {
                    onChangeFunc(value)
                }
            }
        }
    }
}

private fun ContestDto.label(): String = "$organizationName/$name"

@Suppress("TOO_LONG_FUNCTION", "LongMethod", "TYPE_ALIAS")
private fun ChildrenBuilder.renderForPublicAndPrivateTests(
    props: TestResourcesProps,
    testSuiteSelectorWindowOpenness: WindowOpenness,
    testSuitesInSelectorState: StateInstance<List<TestSuiteVersioned>>,
    selectedTestSuites: List<TestSuiteVersioned>,
    setSelectedTestSuites: (List<TestSuiteVersioned>) -> Unit,
) {
    div {
        className = ClassName("card shadow mb-4 w-100")

        div {
            className = ClassName("card-body ")

            addAdditionalProperty(
                props.execCmd,
                "Execution command",
                "Execution command that will be used to run the tool and tests",
                "",
                false,
                props.setExecCmd
            )
            val toolTipTextForBatchSize = "Batch size controls how many files will be processed at the same time (1 by default)." +
                    " To know more about batch size, please visit: https://github.com/saveourtool/save."
            addAdditionalProperty(
                props.batchSizeForAnalyzer,
                "",
                toolTipTextForBatchSize,
                "Batch size:",
                true,
                props.setBatchSizeForAnalyzer
            )

            when (props.testingType) {
                TestingType.PRIVATE_TESTS -> showPrivateTestSuitesSelectorModal(
                    props.organizationName,
                    selectedTestSuites,
                    testSuiteSelectorWindowOpenness,
                    testSuitesInSelectorState,
                    setSelectedTestSuites
                )
                TestingType.PUBLIC_TESTS -> showPublicTestSuitesSelectorModal(
                    props.organizationName,
                    selectedTestSuites,
                    testSuiteSelectorWindowOpenness,
                    testSuitesInSelectorState,
                    setSelectedTestSuites
                )
                else -> throw IllegalStateException("Not supported testingType ${props.testingType}")
            }

            // ==== test suite ids selector
            div {
                className = ClassName("mt-2")
                inputTextFormRequired {
                    form = InputTypes.TEST_SUITE_IDS
                    textValue = selectedTestSuites.joinToString(", ") { it.name }
                    validInput = true
                    classes = "col-12 pl-2 pr-2 text-center"
                    name = "Test Suites:"
                    conflictMessage = null
                    onClickFun = testSuiteSelectorWindowOpenness.openWindowAction()
                }
            }
        }
    }
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun ChildrenBuilder.renderForContestMode(
    props: TestResourcesProps,
    contestEnrollerWindowOpenness: WindowOpenness,
) {
    showContestEnrollerModal(
        contestEnrollerWindowOpenness.isOpen(),
        ProjectNameProps(props.organizationName, props.projectName),
        contestEnrollerWindowOpenness.closeWindowAction(),
    ) {
        contestEnrollerWindowOpenness.closeWindow()
        props.onContestEnrollerResponse(it)
    }
    div {
        className = ClassName("card shadow mb-4 w-100")
        div {
            className = ClassName("card-body d-flex justify-content-center")
            button {
                type = ButtonType.button
                className = ClassName("d-flex justify-content-center btn btn-outline-primary")
                +"Enroll for a contest"
                onClick = contestEnrollerWindowOpenness.openWindowAction().withUnusedArg()
            }
        }
    }
    label {
        className = ClassName("control-label col-auto justify-content-between justify-content-center font-weight-bold text-gray-800 mb-4 pl-0")
        +"4. Run your tool on private tests and see your score"
    }
    div {
        className = ClassName("card shadow mb-4 w-100")
        div {
            className = ClassName("input-group-prepend")

            selectorBuilder(
                props.selectedContest.label(),
                props.availableContests.map { it.label() },
                "form-control custom-select",
            ) { event ->
                val selectedContestLabel = event.target.value
                val selectedContest = requireNotNull(props.availableContests.find { it.label() == selectedContestLabel }) {
                    "Invalid contest is selected $selectedContestLabel"
                }
                props.setSelectedContest(selectedContest)
            }
        }
    }
}

/**
 * @return a Component
 */
@Suppress(
    "LongMethod",
    "TOO_LONG_FUNCTION",
)
private fun prepareTestResourcesSelection() = FC<TestResourcesProps> { props ->
    // states for private mode
    val testSuiteSelectorWindowOpennessPrivateMode = useWindowOpenness()
    val testSuitesInSelectorStatePrivateMode = useState(emptyList<TestSuiteVersioned>())
    // states for public mode
    val testSuiteSelectorWindowOpennessPublicMode = useWindowOpenness()
    val testSuitesInSelectorStatePublicMode = useState(emptyList<TestSuiteVersioned>())
    // states for contest mode
    val contestEnrollerWindowOpenness = useWindowOpenness()

    if (props.testingType == TestingType.CONTEST_MODE) {
        label {
            className = ClassName("control-label col-auto justify-content-between justify-content-center font-weight-bold text-gray-800 mb-0 pl-0")
            +"3. Enroll for a contest"
        }
        label {
            className = ClassName("col-auto justify-content-between justify-content-center mb-4 pl-0")
            +"Note: if you've already enrolled into the desired contest, you should skip this step. Only new contests will be displayed here."
        }
    } else {
        label {
            className = ClassName("control-label col-auto justify-content-between font-weight-bold text-gray-800 mb-4 pl-0")
            +"3. Specify test-resources that will be used for testing:"
        }
    }

    when (props.testingType) {
        TestingType.PRIVATE_TESTS -> renderForPublicAndPrivateTests(
            props,
            testSuiteSelectorWindowOpennessPrivateMode,
            testSuitesInSelectorStatePrivateMode,
            props.selectedPrivateTestSuites,
            props.setSelectedPrivateTestSuites
        )
        TestingType.PUBLIC_TESTS -> renderForPublicAndPrivateTests(
            props,
            testSuiteSelectorWindowOpennessPublicMode,
            testSuitesInSelectorStatePublicMode,
            props.selectedPublicTestSuites,
            props.setSelectedPublicTestSuites
        )
        TestingType.CONTEST_MODE -> renderForContestMode(
            props,
            contestEnrollerWindowOpenness
        )
    }
}
