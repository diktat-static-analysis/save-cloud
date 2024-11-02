/**
 * View for displaying individual execution results
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.common.agent.TestExecutionDto
import com.saveourtool.common.agent.TestExecutionExtDto
import com.saveourtool.common.domain.TestResultDebugInfo
import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.execution.ExecutionUpdateDto
import com.saveourtool.common.filters.TestExecutionFilter
import com.saveourtool.common.utils.ELLIPSIS
import com.saveourtool.frontend.common.components.RequestStatusContext
import com.saveourtool.frontend.common.components.basic.*
import com.saveourtool.frontend.common.components.requestStatusContext
import com.saveourtool.frontend.common.components.tables.TableProps
import com.saveourtool.frontend.common.components.tables.columns
import com.saveourtool.frontend.common.components.tables.enableExpanding
import com.saveourtool.frontend.common.components.tables.isExpanded
import com.saveourtool.frontend.common.components.tables.pageIndex
import com.saveourtool.frontend.common.components.tables.pageSize
import com.saveourtool.frontend.common.components.tables.tableComponent
import com.saveourtool.frontend.common.components.tables.value
import com.saveourtool.frontend.common.components.tables.visibleColumnsCount
import com.saveourtool.frontend.common.components.views.AbstractView
import com.saveourtool.frontend.common.http.getDebugInfoFor
import com.saveourtool.frontend.common.http.getExecutionInfoFor
import com.saveourtool.frontend.common.themes.Colors
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.result.CountWarnings
import com.saveourtool.save.frontend.components.basic.displayExecutionInfoHeader
import com.saveourtool.save.frontend.components.basic.displayTestNotFound
import com.saveourtool.save.frontend.components.basic.executionStatusComponent
import com.saveourtool.save.frontend.components.basic.table.filters.testExecutionFiltersRow
import com.saveourtool.save.frontend.components.basic.testStatusComponent
import com.saveourtool.save.frontend.components.views.test.analysis.analysisResultsView
import com.saveourtool.save.frontend.components.views.test.analysis.testMetricsView

import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr
import web.cssom.*

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The maximum length of a test name (in chars), before it gets shortened.
 */
private const val MAX_TEST_NAME_LENGTH = 35

/**
 * The infix of the shortened text label.
 */
private const val TEXT_LABEL_INFIX = " $ELLIPSIS "

/**
 * [Props] for execution results view
 */
external interface ExecutionProps : PropsWithChildren {
    /**
     * Organization name
     */
    var organization: String

    /**
     * Project name
     */
    var project: String

    /**
     * ID of execution
     */
    var executionId: String

    /**
     * All filters in one value [filters]
     */
    var filters: TestExecutionFilter

    /**
     * Indicates whether test analysis is enabled or not.
     */
    var testAnalysisEnabled: Boolean
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
     * All filters in one value [filters]
     */
    var filters: TestExecutionFilter
}

/**
 * [Props] of a data table with status and testSuite
 */
external interface StatusProps<D : Any> : TableProps<D> {
    /**
     * All filters in one value [filters]
     */
    var filters: TestExecutionFilter
}

/**
 * A Component for execution view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress("MAGIC_NUMBER", "TYPE_ALIAS")
class ExecutionView : AbstractView<ExecutionProps, ExecutionState>(Style.SAVE_LIGHT) {
    @Suppress("TYPE_ALIAS")
    private val additionalInfo: MutableMap<String, AdditionalRowInfo> = mutableMapOf()
    private val testExecutionsTable: FC<StatusProps<com.saveourtool.common.agent.TestExecutionExtDto>> = tableComponent(
        columns = {
            columns {
                column(id = "index", header = "#") {
                    Fragment.create {
                        td {
                            +"${it.row.index + 1 + it.pageIndex * it.pageSize}"
                        }
                    }
                }
                column(id = "startTime", header = "Start time", { testExecution.startTimeSeconds }) { cellContext ->
                    Fragment.create {
                        td {
                            +"${
                                cellContext.value?.let { Instant.fromEpochSeconds(it, 0) }
                                    ?: "Running"
                            }"
                        }
                    }
                }
                column(id = "endTime", header = "End time", { testExecution.endTimeSeconds }) { cellContext ->
                    Fragment.create {
                        td {
                            +"${
                                cellContext.value?.let { Instant.fromEpochSeconds(it, 0) }
                                    ?: "Running"
                            }"
                        }
                    }
                }
                column(id = "status", header = "Status", { testExecution.status.name }) {
                    Fragment.create {
                        td {
                            +it.value
                        }
                    }
                }
                column(id = "missing", header = "Missing", { testExecution.unmatched }) {
                    Fragment.create {
                        td {
                            +formatCounter(it.value)
                        }
                    }
                }
                column(id = "matched", header = "Matched", { testExecution.matched }) {
                    Fragment.create {
                        td {
                            +formatCounter(it.value)
                        }
                    }
                }
                column(id = "path", header = "Test Name") { cellContext ->
                    Fragment.create {
                        td {
                            val testName = cellContext.value.testExecution.filePath
                            val shortTestName = testName.shorten(MAX_TEST_NAME_LENGTH)
                            +shortTestName

                            // debug info is provided by agent after the execution
                            // possibly there can be cases when this info is not available
                            if (cellContext.value.hasDebugInfo == true) {
                                style = jso {
                                    textDecoration = "underline".unsafeCast<TextDecoration>()
                                    color = "blue".unsafeCast<Color>()
                                    cursor = "pointer".unsafeCast<Cursor>()
                                }

                                onClick = {
                                    this@ExecutionView.scope.launch {
                                        if (!cellContext.row.isExpanded) {
                                            getAdditionalInfoFor(cellContext.value.testExecution, cellContext.row.id)
                                        }
                                        cellContext.row.toggleExpanded(null)
                                    }
                                }
                            }
                        }
                    }
                }
                column(id = "plugin", header = "Plugin type", { testExecution.pluginName }) {
                    Fragment.create {
                        td {
                            +it.value
                        }
                    }
                }
                column(id = "suiteName", header = "Test suite", { testExecution.testSuiteName }) {
                    Fragment.create {
                        td {
                            +it.value
                        }
                    }
                }
                column(id = "tags", header = "Tags") {
                    Fragment.create {
                        td {
                            +"${it.value.testExecution.tags}"
                        }
                    }
                }

                if (props.testAnalysisEnabled) {
                    column(id = "testMetrics", header = "Test Metrics") {
                        td.create {
                            testMetricsView {
                                testMetrics = it.value.testMetrics
                            }
                        }
                    }
                    column(id = "testAnalysis", header = "Test Analysis") {
                        td.create {
                            analysisResultsView {
                                analysisResults = it.value.analysisResults
                            }
                        }
                    }
                }
            }
        },
        useServerPaging = true,
        tableOptionsCustomizer = { tableOptions ->
            enableExpanding(tableOptions)
        },
        getRowProps = { row ->
            val color = when (row.original.testExecution.status) {
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
        },
        renderExpandedRow = { tableInstance, row ->
            val (errorDescription, trdi, trei) = additionalInfo[row.id] ?: AdditionalRowInfo()
            when {
                errorDescription != null -> tr {
                    td {
                        colSpan = tableInstance.visibleColumnsCount()
                        +"Error retrieving additional information: $errorDescription"
                    }
                }
                trei?.failReason != null || trdi != null -> {
                    trei?.failReason?.let { executionStatusComponent(it, tableInstance)() }
                    trdi?.let {
                        testStatusComponent(
                            "${props.organization}/${props.project}",
                            it,
                            tableInstance,
                            row.original.testExecution
                        )()
                    }
                }

                else -> tr {
                    td {
                        colSpan = tableInstance.visibleColumnsCount()
                        +"No info available yet for this test execution"
                    }
                }
            }
        },
    ) {
        arrayOf(it.filters)
    }

    init {
        state.executionDto = null
        state.filters = TestExecutionFilter.empty
    }

    private fun formatCounter(count: Long?): String = count?.let {
        if (CountWarnings.isNotApplicable(it.toInt())) {
            "N/A"
        } else {
            it.toString()
        }
    } ?: ""

    private suspend fun getAdditionalInfoFor(testExecution: com.saveourtool.common.agent.TestExecutionDto, id: String) {
        val trDebugInfoResponse = getDebugInfoFor(testExecution.requiredId())
        // FixMe: invalid setup of execution because of the invalid propagated ID
        val trExecutionInfoResponse = getExecutionInfoFor(testExecution)
        // there may be errors during deserialization, which will otherwise be silently ignored
        try {
            additionalInfo[id] = AdditionalRowInfo()
            if (trDebugInfoResponse.ok) {
                additionalInfo[id] = additionalInfo[id]!!
                    .copy(testResultDebugInfo = trDebugInfoResponse.decodeFromJsonString<TestResultDebugInfo>())
            }
            if (trExecutionInfoResponse.ok) {
                additionalInfo[id] = additionalInfo[id]!!
                    .copy(executionInfo = trExecutionInfoResponse.decodeFromJsonString<ExecutionUpdateDto>())
            }
        } catch (ex: SerializationException) {
            additionalInfo[id] = additionalInfo[id]!!
                .copy(errorDescription = ex.describe())
        }
    }

    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch {
            val headers = Headers().also { it.set("Accept", "application/json") }
            val executionDtoFromBackend: ExecutionDto =
                    get(
                        url = "$apiUrl/executionDto",
                        params = jso<dynamic> {
                            executionId = props.executionId
                        },
                        headers = headers,
                        loadingHandler = ::classLoadingHandler,
                    )
                        .decodeFromJsonString()
            setState {
                executionDto = executionDtoFromBackend
                filters = props.filters
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
    override fun ChildrenBuilder.render() {
        div {
            div {
                displayExecutionInfoHeader(state.executionDto, false, "row mb-2") { event ->
                    scope.launch {
                        val response = post(
                            url = "$apiUrl/run/re-trigger",
                            params = jso<dynamic> {
                                executionId = props.executionId
                            },
                            headers = Headers(),
                            body = undefined,
                            loadingHandler = ::classLoadingHandler,
                        )
                        if (response.ok) {
                            window.alert("Rerun request successfully submitted")
                            window.location.reload()
                        }
                    }
                    event.preventDefault()
                }
            }
        }

        // fixme: table is rendered twice because of state change when `executionDto` is fetched
        testExecutionsTable {
            filters = state.filters
            commonHeaderBuilder = { cb, tableInstance, navigate ->
                with(cb) {
                    tr {
                        th {
                            colSpan = tableInstance.visibleColumnsCount()
                            testExecutionFiltersRow {
                                filters = state.filters
                                onChangeFilters = { filterValue ->
                                    setState {
                                        filters = filters.copy(
                                            status = filterValue.status?.takeIf { it.name != "ANY" },
                                            fileName = filterValue.fileName?.ifEmpty { null },
                                            testSuite = filterValue.testSuite?.ifEmpty { null },
                                            tag = filterValue.tag?.ifEmpty { null },
                                        )
                                    }
                                    tableInstance.resetPageIndex(true)
                                    navigate(getUrlWithFiltersParams(filterValue))
                                }
                            }
                        }
                    }
                }
            }
            getData = { page, size ->
                post(
                    url = "$apiUrl/test-executions",
                    params = jso<dynamic> {
                        executionId = props.executionId
                        this.page = page
                        this.size = size
                        checkDebugInfo = true
                        testAnalysis = props.testAnalysisEnabled
                    },
                    headers = jsonHeaders,
                    body = Json.encodeToString(filters),
                    loadingHandler = ::classLoadingHandler,
                ).unsafeMap {
                    Json.decodeFromString<Array<com.saveourtool.common.agent.TestExecutionExtDto>>(
                        it.text().await()
                    )
                }.onEach { (testExecution: com.saveourtool.common.agent.TestExecutionDto) ->
                    /*
                     * Add empty debug info to each test execution.
                     */
                    testExecution.apply {
                        asDynamic().debugInfo = null
                    }
                }
            }
            getPageCount = { pageSize ->
                val filtersQueryString = buildString {
                    filters.status?.let {
                        append("&status=${filters.status}")
                    } ?: append("")

                    filters.testSuite?.let {
                        append("&testSuite=${filters.testSuite}")
                    } ?: append("")
                }

                val count: Int = get<dynamic>(
                    url = "$apiUrl/testExecution/count?executionId=${props.executionId}$filtersQueryString",
                    headers = Headers().also {
                        it.set("Accept", "application/json")
                    },
                    loadingHandler = ::classLoadingHandler,
                )
                    .json()
                    .await()
                    .unsafeCast<Int>()
                count / pageSize + 1
            }
        }
        displayTestNotFound(state.executionDto)
    }

    private fun getUrlWithFiltersParams(filterValue: TestExecutionFilter) =
            // fixme: relies on the usage of HashRouter, hence hash.drop leading `#`
            "${window.location.pathname}${filterValue.toQueryParams()}"

    /**
     * @param maxLength the maximum desired length of a text label.
     * @return the optionally shortened label.
     * @throws IllegalArgumentException if [maxLength] is less or equal than the length of [TEXT_LABEL_INFIX].
     */
    private fun String.shorten(maxLength: Int): String {
        val infixLength = TEXT_LABEL_INFIX.length

        require(maxLength > infixLength) {
            "The desired length: $maxLength doesn't exceed the length of the infix: $infixLength"
        }

        return when {
            length > maxLength -> {
                val usableLength = maxLength - infixLength
                val prefixLength = usableLength / 2
                val suffixLength = usableLength - prefixLength
                take(prefixLength) + TEXT_LABEL_INFIX + takeLast(suffixLength)
            }

            else -> this
        }
    }

    companion object : RStatics<ExecutionProps, ExecutionState, ExecutionView, Context<RequestStatusContext?>>(ExecutionView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}

/**
 * @property errorDescription if retrieved data can't be parsed, this field should contain description of the error
 * @property testResultDebugInfo
 * @property executionInfo
 */
private data class AdditionalRowInfo(
    val errorDescription: String? = null,
    val testResultDebugInfo: TestResultDebugInfo? = null,
    val executionInfo: ExecutionUpdateDto? = null,
)
