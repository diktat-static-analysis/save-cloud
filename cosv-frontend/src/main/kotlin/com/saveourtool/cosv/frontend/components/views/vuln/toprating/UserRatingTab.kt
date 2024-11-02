@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.cosv.frontend.components.views.vuln.toprating

import com.saveourtool.common.info.UserInfo
import com.saveourtool.frontend.common.components.basic.renderUserAvatarWithName
import com.saveourtool.frontend.common.components.basic.table.filters.nameFiltersRow
import com.saveourtool.frontend.common.components.tables.*
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopResponseHandler

import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr
import web.cssom.*

private const val DEFAULT_PAGE_SIZE = 10_000

val userRatingTable: FC<Props> = FC {
    val (userNamePrefix, setUserNamePrefix) = useState("")

    val fetchUserRequest: suspend WithRequestStatusContext.(String) -> UserArray = { prefix ->
        get(
            "$apiUrl/users/by-prefix?prefix=$prefix&pageSize=$DEFAULT_PAGE_SIZE",
            jsonHeaders,
            ::loadingHandler,
            ::noopResponseHandler,
        )
            .decodeFromJsonString<UserArray>()
            .also { array -> array.sortByDescending { it.rating } }
    }

    // Temp hack for correct position displaying
    val (users, setUsers) = useState<UserArray>(emptyArray())
    val doOnce = useOnceAction()

    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "MAGIC_NUMBER")
    val userRatingTable: FC<TableProps<UserInfo>> = tableComponent(
        columns = {
            columns {
                column(id = "index", header = "Position") { cellContext ->
                    Fragment.create { renderRatingPosition(cellContext.value, users) }
                }
                column(id = "name", header = "Name", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            className = ClassName("align-middle")
                            renderUserAvatarWithName(cellContext.row.original, isHorizontal = true, classes = "mr-2") {
                                height = 3.rem
                                width = 3.rem
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
        initialPageSize = 10,
        useServerPaging = false,
        isTransparentGrid = true,
    )

    div {
        className = ClassName("row justify-content-center")
        div {
            className = ClassName("col-5")
            userRatingTable {
                getData = { _, _ ->
                    fetchUserRequest(userNamePrefix).also {
                        doOnce { setUsers(it) }
                    }
                }
                commonHeaderBuilder = { cb, tableInstance, _ ->
                    with(cb) {
                        tr {
                            th {
                                colSpan = tableInstance.visibleColumnsCount()
                                nameFiltersRow {
                                    name = userNamePrefix
                                    onChangeFilters = { setUserNamePrefix(it.orEmpty()) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private typealias UserArray = Array<UserInfo>
