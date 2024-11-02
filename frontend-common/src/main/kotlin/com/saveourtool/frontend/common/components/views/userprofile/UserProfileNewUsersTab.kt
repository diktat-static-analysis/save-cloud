@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.frontend.common.components.views.userprofile

import com.saveourtool.common.info.UserInfo
import com.saveourtool.frontend.common.components.basic.renderUserAvatarWithName
import com.saveourtool.frontend.common.components.tables.TableProps
import com.saveourtool.frontend.common.components.tables.columns
import com.saveourtool.frontend.common.components.tables.tableComponent
import com.saveourtool.frontend.common.components.tables.value
import com.saveourtool.frontend.common.utils.*

import react.FC
import react.Fragment
import react.Props
import react.create
import react.dom.html.ReactHTML.td
import web.cssom.ClassName
import web.cssom.rem

val renderNewUsersTableForProfileView: FC<Props> = FC {
    @Suppress(
        "TYPE_ALIAS",
        "MAGIC_NUMBER",
    )
    val newUsersTable: FC<TableProps<UserInfo>> = tableComponent(
        columns = {
            columns {
                column(id = "name", header = "Name", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            className = ClassName("align-middle")
                            renderUserAvatarWithName(cellContext.row.original) {
                                height = 3.rem
                                width = 3.rem
                            }
                        }
                    }
                }
                column(id = "originalName", header = "Original login") { cellContext ->
                    Fragment.create {
                        td {
                            className = ClassName("align-middle text-center")
                            +cellContext.value.originalLogins.firstNotNullOfOrNull { it.value }
                        }
                    }
                }
                column(id = "source", header = "Source") { cellContext ->
                    Fragment.create {
                        td {
                            className = ClassName("align-middle text-center")
                            +cellContext.value.originalLogins.firstNotNullOfOrNull { it.key }
                        }
                    }
                }
            }
        },
        isTransparentGrid = true,
    )

    newUsersTable {
        getData = { _, _ ->
            get(
                url = "$apiUrl/users/new-users",
                headers = jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            ).unsafeMap {
                it.decodeFromJsonString()
            }
        }
    }
}
