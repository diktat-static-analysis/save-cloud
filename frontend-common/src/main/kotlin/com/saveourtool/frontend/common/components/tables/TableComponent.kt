/**
 * Utilities for react-tables
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.tables

import com.saveourtool.frontend.common.components.modal.displayModal
import com.saveourtool.frontend.common.components.modal.mediumTransparentModalStyle
import com.saveourtool.frontend.common.http.HttpStatusException
import com.saveourtool.frontend.common.utils.WithRequestStatusContext
import com.saveourtool.frontend.common.utils.buttonBuilder
import com.saveourtool.frontend.common.utils.spread
import com.saveourtool.frontend.common.utils.useRequestStatusContext

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.em
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import react.router.NavigateFunction
import react.router.useNavigate
import tanstack.react.table.renderCell
import tanstack.react.table.renderHeader
import tanstack.react.table.useReactTable
import tanstack.table.core.Column
import tanstack.table.core.ColumnDef
import tanstack.table.core.Header
import tanstack.table.core.Row
import tanstack.table.core.RowData
import tanstack.table.core.SortDirection
import tanstack.table.core.SortingState
import tanstack.table.core.Table
import tanstack.table.core.TableOptions
import tanstack.table.core.TableState
import tanstack.table.core.getCoreRowModel
import tanstack.table.core.getSortedRowModel
import web.cssom.ClassName
import web.cssom.Cursor
import web.cssom.Overflow
import web.cssom.rem

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val TABLE_HEADERS_LOCALE_NAMESPACE = "table-headers"
const val INITIAL_TABLE_PAGE_SIZE = 10
const val VULNERABILITIES_COLLECTION_TABLE_PAGE_SIZE = 7

private typealias TableHeaderBuilder<T> = (ChildrenBuilder, Table<T>, NavigateFunction) -> Unit

/**
 * [Props] of a data table
 */
external interface TableProps<D : Any> : Props {
    /**
     * Table header
     */
    var tableHeader: String

    /**
     * Lambda to get table data
     */
    @Suppress("TYPE_ALIAS")
    var getData: suspend WithRequestStatusContext.(pageIndex: Int, pageSize: Int) -> Array<out D>

    /**
     * Lambda to update number of pages
     */
    var getPageCount: (suspend (pageSize: Int) -> Int)?

    /**
     * [ClassName] that is applied to card-body section of a table (table itself)
     */
    var cardBodyClassName: String

    /**
     * Builder function for table common header, which will be placed above individual column headers
     */
    var commonHeaderBuilder: TableHeaderBuilder<D>?
}

/**
 * A `Component` for a data table
 *
 * @param columns columns as an array of [Column]
 * @param initialPageSize initial size of table page
 * @param getRowProps a function returning `TableRowProps` for customization of table row, defaults to empty
 * @param useServerPaging whether data is split into pages server-side or in browser
 * @param additionalOptions
 * @param renderExpandedRow how to render an expanded row if `useExpanded` plugin is used. Is invoked inside a `<tbody>` tag.
 * @param getAdditionalDependencies allows filter the table using additional components (dependencies)
 * @param isTransparentGrid
 * @param tableOptionsCustomizer can customize [TableOptions] in scope of [FC]; for example:
 * ```kotlin
 *  {
 *      val (sorting, setSorting) = useState<SortingState>(emptyArray())
 *      it.initialState!!.sorting = sorting
 *  }
 * ```
 * @return a functional react component
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "TYPE_ALIAS",
    "ComplexMethod",
    "ForbiddenComment",
    "LongMethod",
    "LongParameterList",
    "TooGenericExceptionCaught",
    "MAGIC_NUMBER",
    "LAMBDA_IS_NOT_LAST_PARAMETER",
)
fun <D : RowData, P : TableProps<D>> tableComponent(
    columns: (P) -> Array<out ColumnDef<D, *>>,
    initialPageSize: Int = INITIAL_TABLE_PAGE_SIZE,
    useServerPaging: Boolean = false,
    isTransparentGrid: Boolean = false,
    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR") tableOptionsCustomizer: ChildrenBuilder.(TableOptions<D>) -> Unit = {},
    additionalOptions: TableOptions<D>.() -> Unit = {},
    getRowProps: (Row<D>) -> PropsWithStyle = { jso() },
    renderExpandedRow: (ChildrenBuilder.(table: Table<D>, row: Row<D>) -> Unit)? = undefined,
    getAdditionalDependencies: (P) -> Array<dynamic> = { emptyArray() },
): FC<P> = FC { props ->
    require(useServerPaging xor (props.getPageCount == null)) {
        "Either use client-side paging or provide a function to get page count"
    }
    val (data, setData) = useState<Array<out D>>(emptyArray())
    val (pageCount, setPageCount) = useState(1)
    val (pageIndex, setPageIndex) = useState(0)
    val (isModalOpen, setIsModalOpen) = useState(false)
    val (dataAccessException, setDataAccessException) = useState<Exception?>(null)
    val scope = CoroutineScope(Dispatchers.Default)

    val (sorting, setSorting) = useState<SortingState>(emptyArray())
    val tableInstance: Table<D> = useReactTable(options = jso<TableOptions<D>> {
        this.columns = useMemo { columns(props) }
        this.data = data
        this.getCoreRowModel = getCoreRowModel()
        this.manualPagination = useServerPaging
        if (useServerPaging) {
            this.pageCount = pageCount
        }
        this.initialState = jso {
            this.pagination = jso {
                this.pageSize = initialPageSize
                this.pageIndex = pageIndex
            }
            this.sorting = sorting
        }
        this.asDynamic().state = jso<TableState> {
            // Apparently, setting `initialState` is not enough and examples from tanstack-react-table use `state` in `TableOptions`.
            // It's not present in kotlin-wrappers v.423 though.
            this.sorting = sorting
        }
        this.onSortingChange = { updater ->
            setSorting.invoke(updater)
        }
        this.getSortedRowModel = getSortedRowModel()
        this.getPaginationRowModel = tanstack.table.core.getPaginationRowModel()
        additionalOptions()
    }.also { tableOptionsCustomizer(it) })

    // list of entities, updates of which will cause update of the data retrieving effect
    val dependencies: Array<dynamic> = if (useServerPaging) {
        arrayOf(tableInstance.getState().pagination.pageIndex, tableInstance.getState().pagination.pageSize, pageCount)
    } else {
        // when all data is already available, we don't need to repeat `getData` calls
        emptyArray()
    } + getAdditionalDependencies(props)

    useEffect(*dependencies) {
        if (useServerPaging) {
            scope.launch {
                val newPageCount = props.getPageCount!!.invoke(tableInstance.getState().pagination.pageSize)
                if (newPageCount != pageCount) {
                    setPageCount(newPageCount)
                }
            }
        }
    }

    val context = useRequestStatusContext()

    useEffect(*dependencies) {
        scope.launch {
            try {
                setData(context.(props.getData)(
                    tableInstance.getState().pagination.pageIndex, tableInstance.getState().pagination.pageSize
                ))
            } catch (e: CancellationException) {
                // this means, that view is re-rendering while network request was still in progress
                // no need to display an error message in this case
            } catch (e: HttpStatusException) {
                // this is a normal situation which should be handled by responseHandler in `getData` itself.
                // no need to display an error message in this case
            } catch (e: Exception) {
                // other exceptions are not handled by `responseHandler` and should be displayed separately
                setIsModalOpen(true)
                setDataAccessException(e)
            }
        }
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }

    val navigate = useNavigate()

    val commonHeader = useMemo {
        Fragment.create {
            props.commonHeaderBuilder?.invoke(
                this,
                tableInstance,
                navigate
            )
        }
    }

    div {
        className = ClassName("${if (isTransparentGrid) "" else "card shadow"} mb-4")
        if (props.tableHeader != undefined) {
            div {
                className = ClassName("card-header py-3")
                h6 {
                    className = ClassName("m-0 font-weight-bold text-primary text-center")
                    +props.tableHeader
                }
            }
        }
        div {
            className = ClassName("${props.cardBodyClassName} card-body")
            div {
                className = ClassName("table-responsive")
                // we sometimes have strange overflow with some monitor resolution in chrome
                style = jso {
                    overflowX = Overflow.hidden
                }
                table {
                    className = ClassName("table ${if (isTransparentGrid) "" else "table-bordered"} mb-0")
                    width = 100.0
                    cellSpacing = "0"
                    thead {
                        +commonHeader
                        tableInstance.getHeaderGroups().map { headerGroup ->
                            tr {
                                id = headerGroup.id
                                headerGroup.headers.map { header: Header<D, out Any?> ->
                                    val column = header.column
                                    th {
                                        className = ClassName("m-0 font-weight-bold text-center text-nowrap")
                                        +renderHeader(header)
                                        if (column.getCanSort()) {
                                            style = style ?: jso()
                                            style?.cursor = "pointer".unsafeCast<Cursor>()
                                            span {
                                                +when (column.getIsSorted()) {
                                                    SortDirection.asc -> " 🔽"
                                                    SortDirection.desc -> " 🔼"
                                                    else -> ""
                                                }
                                            }
                                            onClick = column.getToggleSortingHandler()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    tbody {
                        tableInstance.getRowModel().rows.map { row ->
                            tr {
                                spread(getRowProps(row))
                                row.getVisibleCells().map { cell ->
                                    +renderCell(cell)
                                }
                            }
                            if (row.isExpanded) {
                                requireNotNull(renderExpandedRow) {
                                    "`useExpanded` is used, but no method for expanded row is provided"
                                }
                                renderExpandedRow.invoke(this@tbody, tableInstance, row)
                            }
                        }
                    }
                }

                if (data.isEmpty()) {
                    div {
                        className = ClassName("col mt-4 mb-4")
                        div {
                            className = ClassName("row justify-content-center")
                            h6 {
                                className = ClassName("m-0 mt-3 font-weight-bold text-primary text-center")
                                +"Nothing was found"
                            }
                        }
                        div {
                            className = ClassName("row justify-content-center")
                            img {
                                src = "/img/sad_cat.png"
                                @Suppress("MAGIC_NUMBER")
                                style = jso {
                                    width = 14.rem
                                }
                            }
                        }
                    }
                }

                if (tableInstance.getPageCount() > 1) {
                    div {
                        className = ClassName("wrapper container m-0 p-0 mt-2")
                        pagingControl(tableInstance, setPageIndex, pageIndex, pageCount, initialPageSize)

                        div {
                            className = ClassName("row ml-1")
                            +"Page "
                            em {
                                className = ClassName("ml-1")
                                +" ${tableInstance.getState().pagination.pageIndex + 1} of ${tableInstance.getPageCount()}"
                            }
                        }
                    }
                }
            }
        }
    }

    displayModal(
        isModalOpen,
        "Error",
        "Error when fetching data: ${dataAccessException?.message}",
        mediumTransparentModalStyle,
        { setIsModalOpen(false) },
    ) {
        buttonBuilder("Close", "secondary") {
            setIsModalOpen(false)
        }
    }
}
