/**
 * Card for the rendering of ratings: for organizations and tools
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.common.entities.OrganizationWithRating
import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.common.filters.OrganizationFilter
import com.saveourtool.common.filters.ProjectFilter
import com.saveourtool.common.frontend.TabMenuBar
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.externals.fontawesome.faTrophy
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong
import react.router.dom.Link
import web.cssom.*

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val MAX_AMOUNT_OF_RECORDS = 3

/**
 * @return functional component for the rating card
 */
internal val globalRating: FC<Props> = FC {
    val (selectedTab, setSelectedTab) = useState(UserRatingTab.ORGS)

    val (organizationsWithRating, setOrganizationsWithRating) = useState<Set<OrganizationWithRating>>(emptySet())
    useRequest {
        val organizationsFromBackend: List<OrganizationWithRating> = post(
            url = "$apiUrl/organizations/by-filters-with-rating",
            headers = jsonHeaders,
            body = Json.encodeToString(OrganizationFilter.created),
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap { it.decodeFromJsonString() }
        setOrganizationsWithRating(organizationsFromBackend.toSet())
    }

    val (projects, setProjects) = useState(emptySet<ProjectDto>())
    useRequest {
        val projectsFromBackend: List<ProjectDto> = post(
            url = "$apiUrl/projects/by-filters",
            headers = jsonHeaders,
            body = Json.encodeToString(ProjectFilter.created),
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()
        setProjects(projectsFromBackend.toSet())
    }

    div {
        className = ClassName("col-4")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                @Suppress("MAGIC_NUMBER")
                minHeight = 20.rem
                height = "100%".unsafeCast<Height>()
            }

            div {
                className = ClassName("col")

                title(" Global Rating", faTrophy)
                tab(selectedTab.name, UserRatingTab.values().map { it.name }) {
                    setSelectedTab(UserRatingTab.valueOf(it))
                }
                when (selectedTab) {
                    UserRatingTab.ORGS -> renderingOrganizationChampionsTable(organizationsWithRating, MAX_AMOUNT_OF_RECORDS)
                    UserRatingTab.TOOLS -> renderingProjectChampionsTable(projects, MAX_AMOUNT_OF_RECORDS)
                }

                div {
                    className = ClassName("row")
                    style = jso {
                        justifyContent = JustifyContent.center
                        display = Display.flex
                        alignItems = AlignItems.center
                        alignSelf = AlignSelf.start
                    }
                    Link {
                        className = ClassName("mb-5")
                        to = "/${FrontendRoutes.CONTESTS_GLOBAL_RATING}/${selectedTab.name.lowercase()}"
                        +"View more "
                    }
                }
            }
        }
    }
}

/**
 * Enum that contains values for the tab that is used in rating card
 */
enum class UserRatingTab {
    ORGS,
    TOOLS,
    ;

    companion object : TabMenuBar<UserRatingTab> {
        // The string is the postfix of a [regexForUrlClassification] for parsing the url
        private val postfixInRegex = values().joinToString("|") { it.name.lowercase() }
        override val nameOfTheHeadUrlSection = ""
        override val defaultTab: UserRatingTab = UserRatingTab.ORGS
        override val regexForUrlClassification = "/${FrontendRoutes.CONTESTS_GLOBAL_RATING}/($postfixInRegex)"
        override fun valueOf(elem: String): UserRatingTab = UserRatingTab.valueOf(elem)
        override fun values(): Array<UserRatingTab> = UserRatingTab.values()
    }
}

private fun ChildrenBuilder.renderingProjectChampionsTable(
    projects: Set<ProjectDto>,
    maxAmount: Int,
) {
    projects.take(maxAmount).forEachIndexed { i, project ->
        div {
            className = ClassName("row text-muted pb-3 mb-3 border-bottom border-gray mx-2")
            div {
                className = ClassName("col-2")
                h3 {
                    className = ClassName("text-info")
                    +(i + 1).toString()
                }
            }

            div {
                className = ClassName("col-6")
                p {
                    className = ClassName("media-body pb-3 mb-0 small lh-125 text-left")
                    strong {
                        className = ClassName("d-block text-gray-dark")
                        Link {
                            to = "/${project.organizationName}/${project.name}"
                            +project.name
                        }
                    }
                    +("${project.description} ")
                }
            }

            div {
                className = ClassName("col-4")
                p {
                    +project.contestRating.toFixedStr(2)
                }
            }
        }
    }
}

private fun ChildrenBuilder.renderingOrganizationChampionsTable(
    organizations: Set<OrganizationWithRating>,
    maxAmount: Int,
) {
    organizations.take(maxAmount).forEachIndexed { i, organizationWithRating ->
        div {
            className = ClassName("row text-muted pb-3 mb-3 border-bottom border-gray mx-2")
            div {
                className = ClassName("col-2")
                h3 {
                    className = ClassName("text-info")
                    +(i + 1).toString()
                }
            }

            div {
                className = ClassName("col-6")
                p {
                    className = ClassName("media-body pb-3 mb-0 small lh-125 text-left")
                    with(organizationWithRating.organization) {
                        strong {
                            className = ClassName("d-block text-gray-dark")
                            Link {
                                to = "/$name"
                                +name
                            }
                        }
                        +"$description "
                    }
                }
            }

            div {
                className = ClassName("col-4")
                p {
                    +organizationWithRating.globalRating.toFixedStr(2)
                }
            }
        }
    }
}
