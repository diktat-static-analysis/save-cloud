@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views.projectcollection

import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.common.filters.ProjectFilter
import com.saveourtool.common.info.UserInfo
import com.saveourtool.frontend.common.components.RequestStatusContext
import com.saveourtool.frontend.common.components.requestStatusContext
import com.saveourtool.frontend.common.components.tables.TableProps
import com.saveourtool.frontend.common.components.tables.columns
import com.saveourtool.frontend.common.components.tables.tableComponent
import com.saveourtool.frontend.common.components.tables.value
import com.saveourtool.frontend.common.components.views.AbstractView
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.classLoadingHandler

import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import web.cssom.ClassName

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Enum that contains values for project
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class ProjectListTab {
    PUBLIC,
    PRIVATE,
    ;

    companion object {
        val defaultTab: ProjectListTab = PUBLIC
    }
}

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface CollectionViewProps : Props {
    var currentUserInfo: UserInfo?
}

/**
 * [State] of Collection view component
 */
external interface CollectionViewState : State {
    /**
     * All filters in one value [filters]
     */
    var filters: ProjectFilter

    /**
     * Currently selected [ProjectListTab] tab
     */
    var selectedMenu: ProjectListTab
}

/**
 * `Props` for project table
 */
external interface FiltersProps : TableProps<ProjectDto> {
    /**
     * All filters in one value [filters]
     */
    var filters: ProjectFilter
}

/**
 * A view with collection of projects
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress(
    "TYPE_ALIAS",
    "MAGIC_NUMBER",
)
class CollectionView : AbstractView<CollectionViewProps, CollectionViewState>() {
    private val projectsTable: FC<FiltersProps> = tableComponent(
        columns = {
            columns {
                column(id = "organization", header = "Organization", { organizationName }) { cellContext ->
                    Fragment.create {
                        td {
                            Link {
                                to = "/${cellContext.row.original.organizationName}"
                                +cellContext.value
                            }
                        }
                    }
                }
                column(id = "name", header = "Evaluated Tool", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            Link {
                                to = "/${cellContext.row.original.organizationName}/${cellContext.value}"
                                +cellContext.value
                            }
                            privacySpan(cellContext.row.original)
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
                column(id = "rating", header = "Contest Rating") {
                    Fragment.create {
                        td {
                            +"0"
                        }
                    }
                }
            }
        },
        initialPageSize = 10,
        useServerPaging = false,
        isTransparentGrid = true,
    ) {
        arrayOf(it.filters)
    }

    init {
        state.selectedMenu = ProjectListTab.defaultTab
        state.filters = ProjectFilter(name = "", public = true)
    }

    @Suppress(
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "TOO_LONG_FUNCTION",
        "MAGIC_NUMBER",
        "LongMethod",
    )
    override fun ChildrenBuilder.render() {
        particles()

        div {
            className = ClassName("row text-gray-800 justify-content-center")
            div {
                className = ClassName("col-10 mt-4 min-vh-100")
                div {
                    className = ClassName("row mb-2")
                    topLeftCard()
                    topRightCard()
                }

                div {
                    className = ClassName("card flex-md-row")
                    div {
                        className = ClassName("col")

                        tab(state.selectedMenu.name, ProjectListTab.values().map { it.name }, "nav nav-tabs mt-3") {
                            setState {
                                selectedMenu = ProjectListTab.valueOf(it)
                                filters = when (ProjectListTab.valueOf(it)) {
                                    ProjectListTab.PUBLIC -> ProjectFilter(name = "", public = true)
                                    ProjectListTab.PRIVATE -> ProjectFilter(name = "", public = false)
                                }
                            }
                        }

                        projectsTable {
                            filters = state.filters
                            getData = { _, _ ->
                                getProjects()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getProjects() = run {
        val response = post(
            url = "$apiUrl/projects/by-filters",
            headers = jsonHeaders,
            body = Json.encodeToString(state.filters),
            loadingHandler = ::classLoadingHandler,
            responseHandler = ::noopResponseHandler
        )
        if (response.ok) {
            response.unsafeMap {
                it.decodeFromJsonString<Array<ProjectDto>>()
            }
        } else {
            emptyArray()
        }
    }

    companion object : RStatics<CollectionViewProps, CollectionViewState, CollectionView, Context<RequestStatusContext?>>(CollectionView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
