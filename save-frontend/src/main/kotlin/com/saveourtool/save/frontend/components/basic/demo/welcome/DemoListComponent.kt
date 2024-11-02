/**
 * Component for demo list browsing
 */

package com.saveourtool.save.frontend.components.basic.demo.welcome

import com.saveourtool.common.demo.DemoDto
import com.saveourtool.common.filters.DemoFilter
import com.saveourtool.frontend.common.components.basic.cardComponent
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopLoadingHandler

import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.ul
import react.router.dom.Link
import react.useEffect
import react.useState
import web.cssom.ClassName

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val withBackground = cardComponent(isBordered = true, hasBg = true, isPaddingBottomNull = true)

val demoList: FC<Props> = FC {
    val (filter, setFilter) = useState(DemoFilter.running)

    val (demoDtos, setDemoDtos) = useState<List<DemoDto>>(emptyList())
    val getFilteredDemos = useDebouncedDeferredRequest {
        val demos: List<DemoDto> = post(
            url = "$demoApiUrl/demo-list",
            params = jso<dynamic> { demoAmount = DemoDto.DEFAULT_FETCH_NUMBER },
            headers = jsonHeaders,
            body = Json.encodeToString(filter),
            loadingHandler = ::noopLoadingHandler,
        )
            .decodeFromJsonString()
        setDemoDtos(demos)
    }

    useEffect(filter) { getFilteredDemos() }

    withBackground {
        div {
            className = ClassName("m-5")
            div {
                className = ClassName("input-group shadow")
                input {
                    className = ClassName("form-control")
                    placeholder = "Organization"
                    value = filter.organizationName
                    onChange = { event ->
                        setFilter { oldFilter -> oldFilter.copy(organizationName = event.target.value) }
                    }
                }
                input {
                    className = ClassName("form-control")
                    placeholder = "Project"
                    value = filter.projectName
                    onChange = { event ->
                        setFilter { oldFilter -> oldFilter.copy(projectName = event.target.value) }
                    }
                }
            }
        }

        ul {
            className = ClassName("list-group list-group-flush")
            demoDtos.map { demoDto ->
                Link {
                    to = "/demo/${demoDto.projectCoordinates}"
                    className = ClassName("list-group-item")
                    +demoDto.projectCoordinates.toString()
                }
            }
        }
    }
}
