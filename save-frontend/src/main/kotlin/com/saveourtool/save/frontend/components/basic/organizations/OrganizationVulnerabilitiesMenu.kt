@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.columns
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.tables.value
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.FC
import react.Fragment
import react.Props
import react.create
import react.dom.html.ReactHTML.td
import react.router.dom.Link

@Suppress("MAGIC_NUMBER", "TYPE_ALIAS")
private val vulnerabilityTable: FC<TableProps<VulnerabilityDto>> = tableComponent(
    columns = {
        columns {
            column(id = "name", header = "Name", { this.name }) { cellContext ->
                Fragment.create {
                    td {
                        Link {
                            to = "/${FrontendRoutes.VULNERABILITIES}/${cellContext.row.original.name}"
                            +cellContext.value
                        }
                    }
                }
            }
            column(id = "short_description", header = "Description", { progress }) { cellContext ->
                Fragment.create {
                    td {
                        +cellContext.row.original.shortDescription
                    }
                }
            }
            column(id = "progress", header = "Criticality", { progress }) { cellContext ->
                Fragment.create {
                    td {
                        +"${ cellContext.row.original.progress }"
                    }
                }
            }
            column(id = "language", header = "Language", { language }) { cellContext ->
                Fragment.create {
                    td {
                        +"${ cellContext.row.original.language }"
                    }
                }
            }
        }
    },
    initialPageSize = 10,
    useServerPaging = false,
)

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val organizationVulnerabilitiesMenu: FC<OrganizationVulnerabilitiesMenuProps> = FC { props ->

    vulnerabilityTable {
        getData = { _, _ ->
            get(
                url = "$apiUrl/vulnerabilities/by-organization-and-status",
                params = jso<dynamic> {
                    organizationName = props.organizationName
                    status = VulnerabilityStatus.APPROVED
                },
                headers = jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            ).unsafeMap {
                it.decodeFromJsonString()
            }
        }
    }
}

/**
 * OrganizationVulnerabilitiesMenu component props
 */
external interface OrganizationVulnerabilitiesMenuProps : Props {
    /**
     * Current organization name
     */
    var organizationName: String
}
