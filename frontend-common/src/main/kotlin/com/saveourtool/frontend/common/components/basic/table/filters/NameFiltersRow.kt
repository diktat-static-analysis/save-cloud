@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.basic.table.filters

import com.saveourtool.frontend.common.externals.fontawesome.faFilter
import com.saveourtool.frontend.common.externals.fontawesome.faSearch
import com.saveourtool.frontend.common.externals.fontawesome.faWindowClose
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.buttonBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.useEffect
import react.useState
import web.cssom.ClassName
import web.html.InputType

val nameFiltersRow: FC<NameFilterRowProps> = FC { props ->
    val (filtersName, setFiltersName) = useState(props.name)
    useEffect(props.name) {
        if (filtersName != props.name) {
            setFiltersName(props.name)
        }
    }

    div {
        className = ClassName("container-fluid")
        div {
            className = ClassName("row d-flex")

            div {
                className = ClassName("col-0 mr-3 align-self-center")
                fontAwesomeIcon(icon = faFilter)
            }
            div {
                className = ClassName("col-auto align-self-center")
                +"Name: "
            }
            div {
                className = ClassName("col-8")
                input {
                    type = InputType.text
                    className = ClassName("form-control")
                    value = filtersName ?: ""
                    required = false
                    onChange = {
                        setFiltersName(it.target.value)
                    }
                }
            }

            div {
                className = ClassName("col-auto ml-auto d-flex justify-content-between align-items-center")
                buttonBuilder(faSearch, "secondary", isOutline = true, classes = "mr-1 btn-sm") {
                    props.onChangeFilters(filtersName)
                }

                buttonBuilder(faWindowClose, "secondary", isOutline = true, classes = "ml-1 btn-sm") {
                    setFiltersName(null)
                    props.onChangeFilters(null)
                }
            }
        }
    }
}

/**
 * [Props] for filters name
 */
external interface NameFilterRowProps : Props {
    /**
     * All filters in one class property [name]
     */
    var name: String?

    /**
     * lambda to change [name]
     */
    var onChangeFilters: (String?) -> Unit
}
