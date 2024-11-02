@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.frontend.common.components.basic.contests

import com.saveourtool.common.entities.contest.ContestResult
import com.saveourtool.frontend.common.utils.*

import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.router.dom.Link
import web.cssom.*

/**
 * SUMMARY tab in ContestView
 */
val contestSummaryMenu = contestSummaryMenu()

/**
 * ContestSummaryMenu component [Props]
 */
external interface ContestSummaryMenuProps : Props {
    /**
     * Name of a current contest
     */
    var contestName: String
}

private fun ChildrenBuilder.displayTopProjects(sortedResults: List<ContestResult>) {
    ul {
        className = ClassName("col-10 mb-2 list-group")
        displayResult(
            "Top position",
            "Project",
            "Organization",
            "Score",
            null
        )
        sortedResults.forEachIndexed { index, contestResult ->
            displayResult(
                "${index + 1}. ",
                contestResult.projectName,
                contestResult.organizationName,
                contestResult.score?.toFixedStr(2) ?: "-",
                "/${contestResult.organizationName}/${contestResult.projectName}"
            )
        }
    }
}

private fun ChildrenBuilder.displayResult(
    topPositionLabel: String,
    projectName: String,
    organizationName: String,
    score: String,
    linkToProject: String?,
) {
    li {
        val disabled = linkToProject?.let { "" } ?: "disabled bg-light"
        className = ClassName("list-group-item $disabled")
        linkToProject?.let {
            Link {
                to = it
                className = ClassName("stretched-link")
            }
        }
        div {
            className = ClassName("d-flex justify-content-between")
            div {
                className = ClassName("row col")
                div {
                    className = ClassName("mr-1 col text-left")
                    +topPositionLabel
                }
                div {
                    className = ClassName("ml-1 col text-center")
                    +projectName
                }
            }
            div {
                className = ClassName("col text-center")
                +organizationName
            }
            div {
                className = ClassName("col-1 text-right")
                +score
            }
        }
    }
}

/**
 * @return ReactElement
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "AVOID_NULL_CHECKS"
)
private fun contestSummaryMenu() = FC<ContestSummaryMenuProps> { props ->
    val (sortedResults, setSortedResults) = useState<List<ContestResult>>(emptyList())
    useRequest {
        val results = get(
            url = "$apiUrl/contests/${props.contestName}/scores",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<Array<ContestResult>>()
            }
            .sortedByDescending { it.score }
        setSortedResults(results)
    }
    div {
        className = ClassName("mb-3 row justify-content-center align-items-center")
        if (sortedResults.isEmpty()) {
            h6 {
                className = ClassName("text-center")
                +"There are no participants yet. You can be the first one to participate in it!"
            }
        } else {
            displayTopProjects(sortedResults)
        }
    }
}
