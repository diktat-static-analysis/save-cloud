@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.common.entities.ProjectProblemDto
import com.saveourtool.common.filters.ProjectProblemFilter
import com.saveourtool.common.info.UserInfo
import com.saveourtool.frontend.common.components.tables.TableProps
import com.saveourtool.frontend.common.components.tables.columns
import com.saveourtool.frontend.common.components.tables.tableComponent
import com.saveourtool.frontend.common.components.tables.value
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.utils.*

import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import web.cssom.ClassName

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SECURITY tab in ProjectView
 */
val projectSecurityMenu = projectSecurityMenu()

/**
 * Enum that contains values for project
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class ProjectProblemListTab {
    OPEN,
    CLOSED,
    ;
}

/**
 * ProjectSecurityMenu component props
 */
external interface ProjectSecurityMenuProps : Props {
    /**
     * Current project settings
     */
    var project: ProjectDto

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
)
private fun projectSecurityMenu() = FC<ProjectSecurityMenuProps> { props ->

    val (projectProblems, setProjectProblems) = useState<Array<ProjectProblemDto>>(emptyArray())
    val (selectedMenu, setSelectedMenu) = useState(ProjectProblemListTab.OPEN)
    val (filters, setFilters) = useState(ProjectProblemFilter(props.project.organizationName, props.project.name, false))

    val getUsersFromGroup = useDeferredRequest {
        val problems = post(
            url = "$apiUrl/projects/problem/by-filters",
            body = Json.encodeToString(filters),
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<Array<ProjectProblemDto>>()
            }
        setProjectProblems(problems)
    }

    useRequest {
        getUsersFromGroup()
    }

    @Suppress(
        "TYPE_ALIAS",
        "MAGIC_NUMBER",
    )
    val problemsTable: FC<TableProps<ProjectProblemDto>> = tableComponent(
        columns = {
            columns {
                column(id = "name", header = "Name", { name }) { cellContext ->
                    val problem = cellContext.row.original
                    Fragment.create {
                        td {
                            Link {
                                to = "/project/${problem.organizationName}/${problem.projectName}/security/problems/${problem.id}"
                                +cellContext.value
                            }
                        }
                    }
                }
                column(id = "passed", header = "Description") { cellContext ->
                    Fragment.create {
                        td {
                            +(cellContext.value.description.ifEmpty { "Description not provided" })
                        }
                    }
                }
                column(id = "critical", header = "Critical") { cellContext ->
                    Fragment.create {
                        td {
                            +cellContext.value.critical.toString()
                        }
                    }
                }
            }
        },
        initialPageSize = 10,
        useServerPaging = false,
        isTransparentGrid = true,
    ) {
        arrayOf(filters)
    }

    div {
        className = ClassName("row justify-content-center")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-2 mr-3")
            div {
                className = ClassName("card card-body mt-0 pt-0 pr-0 pl-0 border-secondary")

                div {
                    className = ClassName("col mr-2 pr-0 pl-0")
                    nav {
                        div {
                            className = ClassName("pl-3 ui vertical menu profile-setting")
                            form {
                                div {
                                    className = ClassName("item mt-2")
                                    div {
                                        className = ClassName("header")
                                        +"Reporting"
                                    }
                                    div {
                                        className = ClassName("menu")
                                        div {
                                            className = ClassName("mt-2 item")
                                            fontAwesomeIcon(icon = faShieldVirus) {
                                                it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                                            }
                                            Link {
                                                to = "/project/${props.project.organizationName}/${props.project.name}/security"
                                                +"Problems"
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

        // ===================== RIGHT COLUMN =======================================================================
        div {
            className = ClassName("col-6")

            div {
                className = ClassName("d-flex justify-content-end")
                withNavigate { navigateContext ->
                    buttonBuilder(label = "New problem") {
                        navigateContext.navigate("/project/${props.project.organizationName}/${props.project.name}/security/problems/new")
                    }
                }
            }

            tab(selectedMenu.name, ProjectProblemListTab.values().map { it.name }, "nav nav-tabs mt-3") {
                setSelectedMenu(ProjectProblemListTab.valueOf(it))
                val filter = when (ProjectProblemListTab.valueOf(it)) {
                    ProjectProblemListTab.OPEN -> ProjectProblemFilter(props.project.organizationName, props.project.name, false)
                    ProjectProblemListTab.CLOSED -> ProjectProblemFilter(props.project.organizationName, props.project.name, true)
                }
                setFilters(filter)
                getUsersFromGroup()
            }

            problemsTable {
                getData = { _, _ ->
                    projectProblems
                }
            }
        }
    }
}
