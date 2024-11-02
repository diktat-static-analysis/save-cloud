/**
 * Paging control utils
 */

package com.saveourtool.frontend.common.components.tables

import react.ChildrenBuilder
import react.StateSetter
import react.dom.aria.ariaDescribedBy
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.em
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import tanstack.table.core.RowData
import tanstack.table.core.Table
import tanstack.table.core.Updater
import web.cssom.ClassName
import web.html.ButtonType
import web.html.InputType

/**
 * @param tableInstance
 * @param setPageIndex
 * @param initialPageSize
 * @return set entries block
 */
fun <D : RowData> ChildrenBuilder.setEntries(tableInstance: Table<D>, setPageIndex: StateSetter<Int>, initialPageSize: Int) = div {
    className = ClassName("row mt-3")
    div {
        className = ClassName("col-0 pt-1 pr-0")
        +"Show "
    }
    div {
        className = ClassName("col-5 pr-0")
        div {
            className = ClassName("input-group-sm input-group")
            select {
                className = ClassName("form-control custom-select")
                val defaultEntriesSizes = listOf(10, 25, 50, 100)
                val adjustedEntriesSizes = if (defaultEntriesSizes.contains(initialPageSize)) defaultEntriesSizes else defaultEntriesSizes.plus(initialPageSize).sorted()
                adjustedEntriesSizes.forEach {
                    option {
                        className = ClassName("list-group-item")
                        val entries = it.toString()
                        value = entries
                        +entries
                    }
                }
                defaultValue = initialPageSize.toString()
                onChange = {
                    val entries = it.target.value
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, 0)
                    tableInstance.setPageSize(
                        Updater(entries.toInt())
                    )
                }
            }
        }
    }
    div {
        className = ClassName("col-0 pt-1 pl-2")
        +" entries"
    }
}

/**
 * @param tableInstance
 * @param setPageIndex
 * @param pageIndex
 * @param pageCount
 * @param initialPageSize
 * @return paging control block
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun <D : RowData> ChildrenBuilder.pagingControl(
    tableInstance: Table<D>,
    setPageIndex: StateSetter<Int>,
    pageIndex: Int,
    pageCount: Int,
    initialPageSize: Int,
) =
        div {
            className = ClassName("row")
            // First page
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, 0)
                }
                disabled = !tableInstance.canPreviousPage
                +js("String.fromCharCode(171)").unsafeCast<String>()
            }
            // Previous page icon <
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex - 1)
                }
                disabled = !tableInstance.canPreviousPage
                +js("String.fromCharCode(8249)").unsafeCast<String>()
            }
            // Previous before previous page
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                val index = pageIndex - 2
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, index)
                }
                hidden = (index < 0)
                em {
                    +"${index + 1}"
                }
            }
            // Previous page number
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex - 1)
                }
                hidden = !tableInstance.canPreviousPage
                em {
                    +pageIndex.toString()
                }
            }
            // Current page number
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                disabled = true
                em {
                    +"${pageIndex + 1}"
                }
            }
            // Next page number
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex + 1)
                }
                hidden = !tableInstance.canNextPage
                em {
                    +"${pageIndex + 2}"
                }
            }
            // Next after next page
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                val index = pageIndex + 2
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, index)
                }
                hidden = (index > pageCount - 1)
                em {
                    +"${index + 1}"
                }
            }
            // Next page icon >
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex + 1)
                }
                disabled = !tableInstance.canNextPage
                +js("String.fromCharCode(8250)").unsafeCast<String>()
            }
            // Last page
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageCount - 1)
                }
                disabled = !tableInstance.canNextPage
                +js("String.fromCharCode(187)").unsafeCast<String>()
            }
            // Jump to the concrete page
            jumpToPage(tableInstance, setPageIndex, pageCount)

            setEntries(tableInstance, setPageIndex, initialPageSize)
        }

/**
 * @param tableInstance
 * @param setPageIndex
 * @param pageCount
 * @return jump to page block
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun <D : RowData> ChildrenBuilder.jumpToPage(tableInstance: Table<D>, setPageIndex: StateSetter<Int>, pageCount: Int) =
        form {
            var number = 0
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-7 pr-0")
                    div {
                        className = ClassName("input-group input-group-sm mb-3 mt-3")
                        input {
                            type = InputType.text
                            className = ClassName("form-control")
                            ariaDescribedBy = "basic-addon2"
                            placeholder = "Jump to the page"
                            onChange = {
                                // TODO: Provide validation of non int types
                                number = it.target.value.toInt() - 1
                                if (number < 0) {
                                    number = 0
                                }
                                if (number > pageCount - 1) {
                                    number = pageCount - 1
                                }
                            }
                        }
                    }
                }

                div {
                    className = ClassName("col-sm-offset-10 mr-3 justify-content-start")
                    div {
                        className = ClassName("input-group input-group-sm mb-6")
                        div {
                            className = ClassName("input-group-append mt-3")
                            button {
                                type = ButtonType.button
                                className = ClassName("btn btn-outline-secondary")
                                onClick = {
                                    setPageIndexAndGoToPage(tableInstance, setPageIndex, number)
                                }
                                +js("String.fromCharCode(10143)").unsafeCast<String>()
                            }
                        }
                    }
                }
            }
        }

private fun <D : RowData> setPageIndexAndGoToPage(
    tableInstance: Table<D>,
    setPageIndex: StateSetter<Int>,
    index: Int
) {
    setPageIndex(index)
    tableInstance.setPageIndex(Updater(index))
}
