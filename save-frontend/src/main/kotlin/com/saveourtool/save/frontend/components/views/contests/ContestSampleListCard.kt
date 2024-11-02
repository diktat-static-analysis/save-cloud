@file:Suppress(
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.common.entities.contest.ContestSampleDto
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.tables.*
import com.saveourtool.frontend.common.externals.fontawesome.faCode
import com.saveourtool.frontend.common.utils.*

import react.FC
import react.Fragment
import react.Props
import react.create
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import web.cssom.ClassName

@Suppress("TYPE_ALIAS")
private val contestSampleTable: FC<TableProps<ContestSampleDto>> = tableComponent(
    columns = {
        columns {
            column(id = "name", header = "Name", { name }) { cellContext ->
                Fragment.create {
                    td {
                        Link {
                            to = "/${FrontendRoutes.CONTESTS_TEMPLATE}/${cellContext.row.original.id}"
                            +cellContext.value
                        }
                    }
                }
            }
            column(id = "description", header = "Description", { description }) { cellContext ->
                Fragment.create {
                    td {
                        +"${ cellContext.row.original.description }"
                    }
                }
            }
        }
    },
    initialPageSize = @Suppress("MAGIC_NUMBER") 10,
    useServerPaging = false,
)

internal val contestSampleList: FC<Props> = FC {
    div {
        className = ClassName("col")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            div {
                className = ClassName("col")

                title(" Contest templates", faCode)

                contestSampleTable {
                    getData = { _, _ ->
                        get(
                            url = "$apiUrl/contests/sample/get/all",
                            headers = jsonHeaders,
                            ::loadingHandler,
                        )
                            .unsafeMap { it.decodeFromJsonString<Array<ContestSampleDto>>() }
                    }
                }
            }
        }
    }
}
