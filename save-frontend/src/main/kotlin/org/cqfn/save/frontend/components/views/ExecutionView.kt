/**
 * View for displaying individual execution results
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.frontend.components.basic.SelectOption.Companion.ANY
import org.cqfn.save.frontend.components.basic.executionStatistics
import org.cqfn.save.frontend.components.basic.executionTestsNotFound
import org.cqfn.save.frontend.components.basic.testExecutionFiltersRow
import org.cqfn.save.frontend.components.basic.testStatusComponent
import org.cqfn.save.frontend.components.errorStatusContext
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.externals.fontawesome.faRedo
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.externals.table.useFilters
import org.cqfn.save.frontend.http.getDebugInfoFor
import org.cqfn.save.frontend.themes.Colors
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.frontend.utils.spread
import org.cqfn.save.frontend.utils.unsafeMap

import csstype.Background
import csstype.TextDecoration
import org.w3c.fetch.Headers
import react.*
import react.dom.*
import react.table.columns
import react.table.useExpanded
import react.table.usePagination
import react.table.useSortBy

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.html.js.onClickFunction
import kotlinx.js.jso
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * [RProps] for execution results view
 */
external interface ExecutionProps : PropsWithChildren {
    /**
     * ID of execution
     */
    var executionId: String

    /**
     * Test Result Status to filter by
     */
    var status: TestResultStatus?
}

/**
 * A state of execution view
 */
external interface ExecutionState : State {
    /**
     * Execution dto
     */
    var executionDto: ExecutionDto?

    /**
     * Count tests with executionId
     */
    var countTests: Int?

    /**
     * Test Result Status to filter by
     */
    var status: TestResultStatus?

    /**
     * Name of test suite
     */
    var testSuite: String?
}

/**
 * A [RComponent] for execution view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ExecutionView : AbstractView<ExecutionProps, ExecutionState>(false) {
    private val executionStatistics = executionStatistics("mr-auto")
    private val executionTestsNotFound = executionTestsNotFound()
    private val testExecutionFiltersRow = testExecutionFiltersRow(
        initialValueStatus = state.status?.name ?: ANY,
        initialValueTestSuite = state.testSuite ?: "",
        onChangeStatus = { value ->
            if (value == "ANY") {
                setState {
                    status = null
                }
            } else {
                setState {
                    status = TestResultStatus.valueOf(value)
                }
            }
        },
        onChangeTestSuite = { testSuiteValue ->
            if (testSuiteValue == "") {
                setState {
                    testSuite = null
                }
            } else {
                setState {
                    testSuite = testSuiteValue
                }
            }
        }
    )

    @Suppress("MAGIC_NUMBER")
    private val testExecutionsTable = tableComponent(
        columns = columns<TestExecutionDto> {
            column(id = "index", header = "#") {
                buildElement {
                    td {
                        +"${it.row.index + 1 + it.state.pageIndex * it.state.pageSize}"
                    }
                }
            }
            column(id = "startTime", header = "Start time", { startTimeSeconds }) {
                buildElement {
                    td {
                        +"${
                            it.value?.let { Instant.fromEpochSeconds(it, 0) }
                            ?: "Running"
                        }"
                    }
                }
            }
            column(id = "endTime", header = "End time", { endTimeSeconds }) {
                buildElement {
                    td {
                        +"${
                            it.value?.let { Instant.fromEpochSeconds(it, 0) }
                            ?: "Running"
                        }"
                    }
                }
            }
            column(id = "status", header = "Status", { status.name }) {
                buildElement {
                    td {
                        +it.value
                    }
                }
            }
            column(id = "missing", header = "Missing", { missingWarnings }) {
                buildElement {
                    td {
                        +"${it.value ?: ""}"
                    }
                }
            }
            column(id = "matched", header = "Matched", { matchedWarnings }) {
                buildElement {
                    td {
                        +"${it.value ?: ""}"
                    }
                }
            }
            column(id = "path", header = "Test file path") { cellProps ->
                buildElement {
                    td {
                        spread(cellProps.row.getToggleRowExpandedProps())

                        attrs["style"] = jso<CSSProperties> {
                            textDecoration = "underline grey".unsafeCast<TextDecoration>()
                        }

                        val testName = cellProps.value.filePath
                        val shortTestName = if (testName.length > 35) {
                            testName.take(15) + " ... " + testName.takeLast(15)
                        } else {
                            testName
                        }

                        +shortTestName

                        attrs.onClickFunction = {
                            scope.launch {
                                val te = cellProps.value
                                val trdi = getDebugInfoFor(te)
                                if (trdi.ok) {
                                    cellProps.row.original.asDynamic().debugInfo = trdi.decodeFromJsonString<TestResultDebugInfo>()
                                }
                                cellProps.row.toggleRowExpanded()
                            }
                        }
                    }
                }
            }
            column(id = "plugin", header = "Plugin type", { pluginName }) {
                buildElement {
                    td {
                        +it.value
                    }
                }
            }
            column(id = "suiteName", header = "Test suite", { testSuiteName }) {
                buildElement {
                    td {
                        +"${it.value}"
                    }
                }
            }
            column(id = "tags", header = "Tags") {
                buildElement {
                    td {
                        +"${it.value.tags}"
                    }
                }
            }
            column(id = "agentId", header = "Agent ID") {
                buildElement {
                    td {
                        +"${it.value.agentContainerId}".takeLast(12)
                    }
                }
            }
        },
        useServerPaging = true,
        usePageSelection = true,
        plugins = arrayOf(
            useFilters,
            useSortBy,
            useExpanded,
            usePagination,
        ),
        renderExpandedRow = { tableInstance, row ->
            // todo: placeholder before, render data once it's available
            val trdi = row.original.asDynamic().debugInfo as TestResultDebugInfo?
            trdi?.let {
                child(testStatusComponent(trdi, tableInstance)) {
                    // attrs.key = trdi.testResultLocation.toString()
                }
            }
                ?: run {
                    tr {
                        td {
                            attrs.colSpan = "${tableInstance.columns.size}"
                            +"Debug info not available yet for this test execution"
                        }
                    }
                }
        },
        additionalOptions = {
            this.asDynamic().manualFilters = true
        },
        commonHeader = { tableInstance ->
            tr {
                th {
                    attrs.colSpan = "${tableInstance.columns.size}"
                    child(testExecutionFiltersRow)
                }
            }
        },
        getPageCount = { pageSize ->
            val status = state.status?.let {
                "&status=${state.status}"
            }
                ?: run {
                    ""
                }
            val testSuite = state.testSuite?.let {
                "&testSuite=${state.testSuite}"
            }
                ?: run {
                    ""
                }
            val count: Int = get(
                url = "$apiUrl/testExecution/count?executionId=${props.executionId}$status$testSuite",
                headers = Headers().also {
                    it.set("Accept", "application/json")
                },
            )
                .json()
                .await()
                .unsafeCast<Int>()
            count / pageSize + 1
        },
        getRowProps = { row ->
            val color = when (row.original.status) {
                TestResultStatus.FAILED -> Colors.RED
                TestResultStatus.IGNORED -> Colors.GOLD
                TestResultStatus.READY_FOR_TESTING, TestResultStatus.RUNNING -> Colors.GREY
                TestResultStatus.INTERNAL_ERROR, TestResultStatus.TEST_ERROR -> Colors.DARK_RED
                TestResultStatus.PASSED -> Colors.GREEN
            }
            jso {
                style = jso {
                    background = color.value.unsafeCast<Background>()
                }
            }
        }
    ) { page, size ->
        val status = state.status?.let {
            "&status=${state.status}"
        }
            ?: run {
                ""
            }
        val testSuite = state.testSuite?.let {
            "&testSuite=${state.testSuite}"
        }
            ?: run {
                ""
            }
        get(
            url = "$apiUrl/testExecutions?executionId=${props.executionId}&page=$page&size=$size$status$testSuite",
            headers = Headers().apply {
                set("Accept", "application/json")
            },
        )
            .unsafeMap {
                Json.decodeFromString<Array<TestExecutionDto>>(
                    it.text().await()
                )
            }
            .apply {
                asDynamic().debugInfo = null
            }
    }
    init {
        state.executionDto = null
        state.status = null
        state.testSuite = null
    }

    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch {
            val headers = Headers().also { it.set("Accept", "application/json") }
            val executionDtoFromBackend: ExecutionDto =
                    get("$apiUrl/executionDto?executionId=${props.executionId}", headers)
                        .decodeFromJsonString()
            val count: Int = get(
                url = "$apiUrl/testExecution/count?executionId=${props.executionId}",
                headers = Headers().also {
                    it.set("Accept", "application/json")
                },
            )
                .json()
                .await()
                .unsafeCast<Int>()
            setState {
                executionDto = executionDtoFromBackend
                status = props.status
                countTests = count
            }
        }
    }

    @Suppress(
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "TOO_LONG_FUNCTION",
        "AVOID_NULL_CHECKS",
        "MAGIC_NUMBER",
        "ComplexMethod",
        "LongMethod"
    )
    override fun RBuilder.render() {
        div {
            div("d-flex") {
                val statusVal = state.executionDto?.status
                val statusColor = when (statusVal) {
                    ExecutionStatus.ERROR -> "bg-danger"
                    ExecutionStatus.RUNNING, ExecutionStatus.PENDING -> "bg-info"
                    ExecutionStatus.FINISHED -> "bg-success"
                    else -> "bg-secondary"
                }

                div("col-md-2 mb-4") {
                    div("card $statusColor text-white h-100 shadow py-2") {
                        div("card-body") {
                            +(statusVal?.name ?: "N/A")
                            div("text-white-50 small") { +"Project version: ${(state.executionDto?.version ?: "N/A")}" }
                        }
                    }
                }

                child(executionStatistics) {
                    attrs.executionDto = state.executionDto
                    attrs.countTests = state.countTests
                }

                div("col-md-3 mb-4") {
                    div("card border-left-info shadow h-100 py-2") {
                        div("card-body") {
                            div("row no-gutters align-items-center mx-auto") {
                                a("") {
                                    +"Rerun execution"
                                    fontAwesomeIcon(icon = faRedo, classes = "ml-2")
                                    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
                                    attrs.onClickFunction = {
                                        scope.launch {
                                            post(
                                                "$apiUrl/rerunExecution?id=${props.executionId}",
                                                Headers(),
                                                undefined
                                            )
                                        }.invokeOnCompletion {
                                            window.alert("Rerun request successfully submitted")
                                            window.location.reload()
                                        }
                                        it.preventDefault()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // fixme: table is rendered twice because of state change when `executionDto` is fetched
        child(testExecutionsTable)
        child(executionTestsNotFound) {
            attrs.executionDto = state.executionDto
            attrs.countTests = state.countTests
        }
    }

    companion object : RStatics<ExecutionProps, ExecutionState, ExecutionView, Context<StateSetter<Int?>>>(ExecutionView::class) {
        init {
            contextType = errorStatusContext
        }
    }
}
