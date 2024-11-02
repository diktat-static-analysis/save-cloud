/**
 * card with newly added contests
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong
import react.router.dom.Link
import react.useState
import web.cssom.ClassName
import web.cssom.rem

/**
 * rendering of newly added contests
 */
internal val newContests: FC<Props> = FC {
    val (newContests, setNewContests) = useState<List<ContestDto>>(emptyList())
    useRequest {
        val contests: List<ContestDto> = get(
            url = "$apiUrl/contests/newest?pageSize=3",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler
        )
            .unsafeMap { it.decodeFromJsonString() }
        setNewContests(contests.sortedByDescending { it.creationTime })
    }

    div {
        className = ClassName("col-5")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                @Suppress("MAGIC_NUMBER")
                height = 19.rem
            }

            div {
                className = ClassName("card-body d-flex flex-column align-items-start")
                strong {
                    className = ClassName("d-inline-block mb-2 text-success")
                    +"""New contests"""
                }
                h3 {
                    className = ClassName("mb-0")

                    p {
                        className = ClassName("text-dark")
                        +"Hurry up! 🔥"
                    }
                }
                p {
                    className = ClassName("card-text mb-auto")
                    +"Checkout and participate in newest contests"
                }
                newContests.forEach { contest ->
                    Link {
                        to = "/contests/${contest.name}"
                        +contest.name
                    }
                }
            }

            img {
                className = ClassName("card-img-right flex-auto d-none d-md-block")
                src = "/img/undraw_exciting_news_re_y1iw.svg"
                style = jso {
                    @Suppress("MAGIC_NUMBER")
                    width = 12.rem
                }
            }
        }
    }
}
