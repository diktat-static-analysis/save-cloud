@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.common.entities.contest.ContestStatus
import com.saveourtool.frontend.common.components.basic.contests.showContestCreationModal
import com.saveourtool.frontend.common.components.tables.TableProps
import com.saveourtool.frontend.common.components.tables.columns
import com.saveourtool.frontend.common.components.tables.tableComponent
import com.saveourtool.frontend.common.components.tables.value
import com.saveourtool.frontend.common.externals.fontawesome.faPlus
import com.saveourtool.frontend.common.externals.fontawesome.faTrash
import com.saveourtool.frontend.common.utils.*

import org.w3c.fetch.Response
import react.*
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import react.router.useNavigate
import web.cssom.ClassName
import web.html.InputType

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Suppress("MAGIC_NUMBER", "TYPE_ALIAS")
private val contestsTable: FC<OrganizationContestsTableProps<ContestDto>> = tableComponent(
    columns = { props ->
        columns {
            column(id = "name", header = "Contest Name", { name }) { cellContext ->
                Fragment.create {
                    td {
                        className = ClassName("align-middle text-center")
                        Link {
                            to = "/contests/${cellContext.row.original.name}"
                            +cellContext.value
                        }
                    }
                }
            }
            column(id = "description", header = "Description", { description }) { cellContext ->
                Fragment.create {
                    td {
                        className = ClassName("align-middle text-center")
                        +(cellContext.value ?: "Description is not provided")
                    }
                }
            }
            column(id = "start_time", header = "Start Time", { startTime.toString() }) { cellContext ->
                Fragment.create {
                    td {
                        className = ClassName("align-middle text-center")
                        +cellContext.value.replace("T", " ")
                    }
                }
            }
            column(id = "end_time", header = "End Time", { endTime.toString() }) { cellContext ->
                Fragment.create {
                    td {
                        className = ClassName("align-middle text-center")
                        +cellContext.value.replace("T", " ")
                    }
                }
            }
            column("checkBox", "") { cellContext ->
                Fragment.create {
                    td {
                        className = ClassName("align-middle text-center")
                        input {
                            className = ClassName("mx-auto")
                            type = InputType.checkbox
                            id = "checkbox"
                            defaultChecked = props.selectedContestDtos.contains(cellContext.row.original)
                            onChange = { event ->
                                if (event.target.checked) {
                                    props.addSelectedContests(cellContext.row.original)
                                } else {
                                    props.removeSelectedContests(cellContext.row.original)
                                }
                            }
                        }
                    }
                }
            }
        }
    },
) {
    arrayOf(it.isContestCreated)
}

@Suppress(
    "EMPTY_BLOCK_STRUCTURE_ERROR",
)
/**
 * CONTESTS tab in OrganizationView
 */
val organizationContestsMenu: FC<OrganizationContestsMenuProps> = FC { props ->
    useTooltip()
    val (isToUpdateTable, setIsToUpdateTable) = useState(false)
    val contestCreationWindowOpenness = useWindowOpenness()
    val (selectedContests, setSelectedContests) = useState<Set<ContestDto>>(setOf())
    val (contests, setContests) = useState<Set<ContestDto>>(setOf())
    val refreshTable = { setIsToUpdateTable { !it } }
    val addSelectedContestsFun: (ContestDto) -> Unit = { contest ->
        setSelectedContests { it.plus(contest) }
    }
    val removeSelectedContestsFun: (ContestDto) -> Unit = { contest ->
        setSelectedContests { it.minus(contest) }
    }
    val deleteContestsFun = useDeferredRequest {
        val deleteContests = selectedContests.map { it.copy(status = ContestStatus.DELETED) }
        val response = post(
            "$apiUrl/contests/update-all",
            jsonHeaders,
            Json.encodeToString(deleteContests),
            loadingHandler = ::noopLoadingHandler,
        )
        if (response.ok) {
            setContests(contests.minus(selectedContests))
            setSelectedContests(setOf())
            refreshTable()
        }
    }
    val navigate = useNavigate()

    useRequest {
        val response = get(
            url = "$apiUrl/contests/by-organization?organizationName=${props.organizationName}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
        val newContests = if (response.ok) {
            response.unsafeMap { it.decodeFromJsonString<List<ContestDto>>() }.toSet()
        } else {
            emptySet()
        }
        setContests(newContests)
        refreshTable()
    }

    showContestCreationModal(
        props.organizationName,
        contestCreationWindowOpenness.isOpen(),
        {
            contestCreationWindowOpenness.closeWindow()
            navigate(
                to = it,
            )
        },
        {
            contestCreationWindowOpenness.closeWindow()
            props.updateErrorMessage(it)
        }
    ) {
        contestCreationWindowOpenness.closeWindow()
    }

    div {
        className = ClassName("col-8 mx-auto")
        div {
            className = ClassName("d-flex justify-content-end mb-1")
            if (selectedContests.isNotEmpty()) {
                buttonBuilder(
                    faTrash,
                    classes = "mr-2 text-sm btn-sm",
                    title = "Delete selected contests",
                    style = "danger",
                ) {
                    if (window.confirm("Are you sure you want to delete selected contests?")) {
                        deleteContestsFun()
                    }
                }
            }
            if (props.selfRole.hasDeletePermission()) {
                buttonBuilder(
                    faPlus,
                    title = "Create contest",
                    isOutline = true,
                    classes = "text-sm btn-sm",
                ) {
                    contestCreationWindowOpenness.openWindow()
                }
            }
        }
        div {
            className = ClassName("my-3")
            if (contests.isNotEmpty()) {
                contestsTable {
                    getData = { _, _ -> contests.toTypedArray() }
                    isContestCreated = isToUpdateTable
                    addSelectedContests = addSelectedContestsFun
                    removeSelectedContests = removeSelectedContestsFun
                    selectedContestDtos = selectedContests
                }
            } else {
                renderTablePlaceholder("text-center p-4 bg-white text-sm", "dashed") {
                    +"This organization has not created any contest yet."
                    if (props.selfRole.hasDeletePermission()) {
                        br { }
                        buttonBuilder(
                            "You can be the first contest creator in ${props.organizationName}.",
                            style = "",
                            classes = "text-sm text-primary"
                        ) {
                            contestCreationWindowOpenness.openWindow()
                        }
                    }
                }
            }
        }
    }
}

/**
 * OrganizationContestsMenu component props
 */
external interface OrganizationContestsMenuProps : Props {
    /**
     * Current organization name
     */
    var organizationName: String

    /**
     * [Role] of user that is observing this component
     */
    var selfRole: Role

    /**
     * Callback to show error message
     */
    var updateErrorMessage: (Response) -> Unit
}

/**
 * Interface for table reloading after the contest creation
 */
external interface OrganizationContestsTableProps<D : Any> : TableProps<D> {
    /**
     * Flag to update table data when contest is created
     */
    var isContestCreated: Boolean

    /**
     * Fun add contest to list of selected contests
     */
    var addSelectedContests: (ContestDto) -> Unit

    /**
     * Fun remove contest from list of selected contests
     */
    var removeSelectedContests: (ContestDto) -> Unit

    /**
     * Selected contests
     */
    var selectedContestDtos: Set<ContestDto>
}
