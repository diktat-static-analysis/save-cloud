@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.frontend.common.components.basic.contests

import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.frontend.common.components.basic.cardComponent
import com.saveourtool.frontend.common.components.basic.markdown
import com.saveourtool.frontend.common.utils.*

import react.*
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

private val columnCard = cardComponent(hasBg = true, isPaddingBottomNull = true)

/**
 * INFO tab in ContestView
 */
val contestInfoMenu = contestInfoMenu()

/**
 * ContestInfoMenu functional component props
 */
external interface ContestInfoMenuProps : Props {
    /**
     * Current contest name
     */
    var contestName: String?
}

/**
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun contestInfoMenu() = FC<ContestInfoMenuProps> { props ->
    var contest by useState<ContestDto?>(null)
    useRequest {
        val contestDto = get(
            "$apiUrl/contests/${props.contestName}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<ContestDto>()
            }
        contest = contestDto
    }

    div {
        className = ClassName("d-flex justify-content-center")
        div {
            className = ClassName("col-8")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Description"
            }
            div {
                className = ClassName("text-center")
                columnCard {
                    markdown(contest?.description ?: "No description provided **yet**")
                }
            }
        }
    }

    div {
        className = ClassName("mt-4 mb-3")
        div {
            className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
            +"Public tests"
        }
        publicTestComponent {
            this.contestTestSuites = contest?.testSuites ?: emptyList()
            this.contestName = props.contestName ?: ""
        }
    }
}
