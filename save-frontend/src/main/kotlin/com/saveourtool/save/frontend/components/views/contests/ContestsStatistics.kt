/**
 * Statistics of SAVE contests: active and finished contests
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.strong
import react.useState
import web.cssom.ClassName
import web.cssom.rem

internal val statistics: FC<Props> = FC {
    val (activeContests, setActiveContests) = useState<Set<ContestDto>>(emptySet())
    useRequest {
        val contests: List<ContestDto> = get(
            url = "$apiUrl/contests/active",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap { it.decodeFromJsonString() }
        setActiveContests(contests.toSet())
    }

    val (finishedContests, setFinishedContests) = useState<Set<ContestDto>>(emptySet())
    useRequest {
        val contests: List<ContestDto> = get(
            url = "$apiUrl/contests/finished",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()
        setFinishedContests(contests.toSet())
    }

    div {
        className = ClassName("col-4")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                @Suppress("MAGIC_NUMBER")
                height = 15.rem
            }
            div {
                className = ClassName("col-12")
                stats(activeContests, finishedContests)
                proposeContest()
            }
        }
    }
}

/**
 * @param activeContests
 * @param finishedContests
 */
fun ChildrenBuilder.stats(activeContests: Set<ContestDto>, finishedContests: Set<ContestDto>) {
    div {
        className = ClassName("row border-bottom mb-3 mx-3")

        div {
            className = ClassName("col-6 mt-2 mb-2")
            div {
                className = ClassName("row justify-content-center")
                strong {
                    className = ClassName("d-inline-block mb-2 card-text")
                    +"Active contests:"
                }
            }
            div {
                className = ClassName("row justify-content-center")
                h1 {
                    className = ClassName("text-dark")
                    +activeContests.size.toString()
                }
            }
        }
        div {
            className = ClassName("col-6 mt-2")
            div {
                className = ClassName("row justify-content-center")
                strong {
                    className = ClassName("d-inline-block mb-2 card-text ")
                    +"Finished contests:"
                }
            }
            div {
                className = ClassName("row justify-content-center")
                h1 {
                    className = ClassName("text-dark")
                    +finishedContests.size.toString()
                }
            }
        }
    }
}
