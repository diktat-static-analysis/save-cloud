@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.cosv.frontend.components.views.vuln.toprating

import com.saveourtool.common.entities.OrganizationDto
import com.saveourtool.common.filters.OrganizationFilter
import com.saveourtool.frontend.common.components.basic.AVATAR_ORGANIZATION_PLACEHOLDER
import com.saveourtool.frontend.common.components.basic.avatarRenderer
import com.saveourtool.frontend.common.components.basic.table.filters.nameFiltersRow
import com.saveourtool.frontend.common.components.tables.*
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopResponseHandler

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr
import react.router.dom.Link
import web.cssom.*

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DEFAULT_PAGE_SIZE = 10_000

val organizationRatingTab: FC<Props> = FC {
    val (organizationFilter, setOrganizationFilter) = useState(OrganizationFilter.created)

    val fetchOrganizationRequest: suspend WithRequestStatusContext.(OrganizationFilter) -> OrganizationArray = { filter ->
        post(
            url = "$apiUrl/organizations/all-by-filters?pageSize=$DEFAULT_PAGE_SIZE",
            headers = jsonHeaders,
            body = Json.encodeToString(filter),
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .decodeFromJsonString<OrganizationArray>()
            .also { array -> array.sortByDescending { it.rating } }
    }

    // Temp hack for correct position displaying
    val (organizations, setOrganizations) = useState<OrganizationArray>(emptyArray())
    val doOnce = useOnceAction()

    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "MAGIC_NUMBER", "TYPE_ALIAS")
    val tableWithOrganizationRating: FC<TableProps<OrganizationDto>> = tableComponent(
        columns = {
            columns {
                column(id = "index", header = "Position") { cellContext ->
                    Fragment.create { renderRatingPosition(cellContext.value, organizations) }
                }
                column(id = "name", header = "Name", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            className = ClassName("align-middle")
                            Link {
                                img {
                                    className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                    src = cellContext.row.original.avatar?.avatarRenderer() ?: AVATAR_ORGANIZATION_PLACEHOLDER
                                    style = jso {
                                        height = 3.3.rem
                                        width = 3.3.rem
                                    }
                                }
                                to = "/${cellContext.value}"
                                +" ${cellContext.value}"
                            }
                        }
                    }
                }
                column(id = "rating", header = "Rating") { cellContext ->
                    Fragment.create {
                        td {
                            className = ClassName("align-middle")
                            +cellContext.value.rating.toString()
                        }
                    }
                }
            }
        },
        useServerPaging = false,
        isTransparentGrid = true,
    ) {
        arrayOf(it)
    }

    div {
        className = ClassName("row justify-content-center")
        div {
            className = ClassName("col-5")
            tableWithOrganizationRating {
                getData = { _, _ ->
                    fetchOrganizationRequest(organizationFilter).also {
                        doOnce { setOrganizations(it) }
                    }
                }
                commonHeaderBuilder = { cb, tableInstance, _ ->
                    with(cb) {
                        tr {
                            th {
                                colSpan = tableInstance.visibleColumnsCount()
                                nameFiltersRow {
                                    name = organizationFilter.prefix
                                    onChangeFilters = { filterValue ->
                                        val filter = if (filterValue.isNullOrEmpty()) {
                                            OrganizationFilter.created
                                        } else {
                                            OrganizationFilter(filterValue)
                                        }
                                        setOrganizationFilter(filter)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private typealias OrganizationArray = Array<OrganizationDto>
