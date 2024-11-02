@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "GENERIC_NAME",
    "TYPE_ALIAS"
)

package com.saveourtool.frontend.common.components.tables

import js.core.ReadonlyArray
import js.core.jso
import react.ReactNode
import tanstack.table.core.CellContext
import tanstack.table.core.ColumnDef
import tanstack.table.core.ColumnDefTemplate
import tanstack.table.core.RowData
import tanstack.table.core.StringOrTemplateHeader

/**
 * Inspired by (meaning copy-pasted) `ColumnBuilder` from `kotlin-react-table`,
 * which doesn't exist in `kotlin-tanstack-react-table`.
 */
@Suppress(
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION",
)
class ColumnBuilder<TData : RowData> {
    private val columns: MutableList<ColumnDef<TData, *>> = mutableListOf()

    fun column(
        id: String,
        header: String,
        render: (cellContext: CellContext<TData, TData>) -> ReactNode,
    ) = column(id, header, { this }, render)

    /**
     * Create a column definition ([ColumnDef])
     *
     * @param id unique (in this table) ID of a column
     * @param header text value of column's header
     * @param accessor function to map underlying value into another representation ([ColumnDef.accessorFn] in terms of tanstack-table)
     * @param render callback to render a cell based on its value
     * @return [ColumnDef]
     */
    fun <TValue> column(
        id: String,
        header: String,
        accessor: TData.() -> TValue,
        render: (cellContext: CellContext<TData, TValue>) -> ReactNode,
    ): ColumnDef<TData, TValue> = jso<ColumnDef<TData, TValue>> {
        this.id = id
        this.header = StringOrTemplateHeader(header)
        this.accessorFn = { data, _ -> accessor(data) }
        this.cell = ColumnDefTemplate { cellCtx ->
            render(cellCtx)
        }
    }
        .also { columns.add(it) }

    fun build(): ReadonlyArray<ColumnDef<TData, *>> =
            columns.toTypedArray()
}

@Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
)
fun <TData : RowData> columns(
    block: ColumnBuilder<TData>.() -> Unit,
): ReadonlyArray<ColumnDef<TData, *>> =
        ColumnBuilder<TData>().apply(block).build()
