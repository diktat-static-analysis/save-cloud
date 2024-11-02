/**
 * Rating of projects linked to current user (where this user is added)
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.frontend.common.externals.fontawesome.faUser
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.p
import web.cssom.*

/**
 * [FC] that displays user's project ratings
 */
val myProjectsRating: FC<ContestListViewProps> = FC { props ->
    val (myProjects, setMyProjects) = useState(emptySet<ProjectDto>())
    val getMyProjects = useDeferredRequest {
        setMyProjects(
            get(
                url = "$apiUrl/projects/get-for-current-user",
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
            )
                .unsafeMap { it.decodeFromJsonString<Set<ProjectDto>>() }
        )
    }

    props.currentUserInfo?.let {
        getMyProjects()
    }

    div {
        className = ClassName("col-2")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                @Suppress("MAGIC_NUMBER")
                minHeight = 20.rem
                height = "100%".unsafeCast<Height>()
            }
            div {
                className = ClassName("col text-center")
                style = jso {
                    @Suppress("MAGIC_NUMBER")
                    minHeight = 7.rem
                }
                title(" Your stats ", icon = faUser)
                if (myProjects.isEmpty()) {
                    div {
                        className = ClassName("row text-center")
                        p {
                            +"You don't have any projects"
                        }
                    }
                }
                myProjects.forEach {
                    div {
                        className = ClassName("row justify-content-center align-items-center")
                        h4 {
                            +it.contestRating.toFixedStr(2)
                        }
                    }
                    div {
                        className = ClassName("row justify-content-center align-items-center")
                        p {
                            +it.name
                        }
                    }
                }
            }
        }
    }
}
