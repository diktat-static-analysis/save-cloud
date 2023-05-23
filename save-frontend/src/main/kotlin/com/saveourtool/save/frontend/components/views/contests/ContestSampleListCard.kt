@file:Suppress(
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.contest.ContestSampleDto
import com.saveourtool.save.frontend.components.tables.*
import com.saveourtool.save.frontend.externals.fontawesome.faCode
import com.saveourtool.save.frontend.utils.*
import react.FC
import react.Fragment
import react.VFC
import react.create
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import web.cssom.ClassName

val contestSampleList = contestSampleListCard()

@Suppress(
    "MAGIC_NUMBER",
    "TYPE_ALIAS",
)
private val contestSampleTable: FC<TableProps<ContestSampleDto>> = tableComponent(
    columns = {
        columns {
            column(id = "name", header = "Name", { name }) { cellContext ->
                Fragment.create {
                    td {
                        Link {
                            // FixMe: need add link to = ""
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
    initialPageSize = 10,
    useServerPaging = false,
    usePageSelection = false,
)

private fun contestSampleListCard() = VFC {
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
                            .decodeFromJsonString<Array<ContestSampleDto>>()
                    }
                }
            }
        }
    }
}
