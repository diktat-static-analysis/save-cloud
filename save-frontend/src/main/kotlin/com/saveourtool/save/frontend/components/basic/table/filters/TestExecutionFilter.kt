@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.table.filters

import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.filters.TestExecutionFilter
import com.saveourtool.frontend.common.externals.fontawesome.faFilter
import com.saveourtool.frontend.common.externals.fontawesome.faSearch
import com.saveourtool.frontend.common.externals.fontawesome.faTrashAlt
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon

import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.useEffect
import react.useState
import web.cssom.ClassName
import web.html.ButtonType
import web.html.InputType

const val ANY = "ANY"

/**
 * A row of filter selectors for table with `TestExecutionDto`s. Currently, filters are "status" and "test suite".
 *
 * @return a function component
 */
@Suppress("LongMethod", "TOO_LONG_FUNCTION")
val testExecutionFiltersRow: FC<FiltersRowProps> = FC { props ->
    // Store local copy of filters in order to perform searching only by the search button, and not by any change in the filter fields
    val (filters, setFilters) = useState(props.filters)
    useEffect(props.filters) {
        if (filters !== props.filters) {
            setFilters(props.filters)
        }
    }
    div {
        className = ClassName("container-fluid")
        div {
            className = ClassName("row d-flex justify-content-between")
            div {
                className = ClassName("col-0 pr-1 align-self-center")
                fontAwesomeIcon(icon = faFilter)
            }
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-auto align-self-center")
                    +"Status: "
                }
                div {
                    className = ClassName("col-auto")
                    select {
                        className = ClassName("form-control custom-select")
                        val elements = TestResultStatus.values().map { it.name }.toMutableList()
                        elements.add(0, ANY)
                        value = filters.status?.name ?: ANY
                        elements.forEach { element ->
                            option {
                                if (element == props.filters.status?.name) {
                                    selected = true
                                }
                                +element
                            }
                        }
                        onChange = {
                            if (it.target.value == ANY) {
                                setFilters(filters.copy(status = null))
                            } else {
                                setFilters(filters.copy(status = TestResultStatus.valueOf(it.target.value)))
                            }
                        }
                    }
                }
            }
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-auto align-self-center")
                    +"File name: "
                }
                div {
                    className = ClassName("col-auto")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        value = filters.fileName ?: ""
                        required = false
                        onChange = {
                            setFilters(filters.copy(fileName = it.target.value))
                        }
                    }
                }
            }
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-auto align-self-center")
                    +"Test suite: "
                }
                div {
                    className = ClassName("col-auto")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        value = filters.testSuite ?: ""
                        required = false
                        onChange = {
                            setFilters(filters.copy(testSuite = it.target.value))
                        }
                    }
                }
            }
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-auto align-self-center")
                    +"Tags: "
                }
                div {
                    className = ClassName("col-auto")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        value = filters.tag ?: ""
                        required = false
                        onChange = {
                            setFilters(filters.copy(tag = it.target.value))
                        }
                    }
                }
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-outline-primary")
                fontAwesomeIcon(icon = faSearch, classes = "trash-alt")
                onClick = {
                    props.onChangeFilters(filters)
                }
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-outline-primary")
                fontAwesomeIcon(icon = faTrashAlt, classes = "trash-alt")
                onClick = {
                    setFilters(TestExecutionFilter.empty)
                    props.onChangeFilters(TestExecutionFilter.empty)
                }
            }
        }
    }
}

/**
 * [Props] for filters value
 */
external interface FiltersRowProps : Props {
    /**
     * All filters in one class property [filters]
     */
    var filters: TestExecutionFilter

    /**
     * lambda to change [filters]
     */
    var onChangeFilters: (TestExecutionFilter) -> Unit
}
