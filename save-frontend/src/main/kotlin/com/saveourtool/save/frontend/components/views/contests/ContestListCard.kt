/**
 * a card with contests list from ContestListView
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.modal.displayModal
import com.saveourtool.frontend.common.components.modal.mediumTransparentModalStyle
import com.saveourtool.frontend.common.externals.fontawesome.faArrowRight
import com.saveourtool.frontend.common.externals.fontawesome.faCode
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.save.frontend.components.basic.ContestNameProps
import com.saveourtool.save.frontend.components.basic.showContestEnrollerModal

import js.core.jso
import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.strong
import react.router.dom.Link
import web.cssom.ClassName
import web.cssom.Height
import web.cssom.rem
import web.html.ButtonType

/**
 * [FC] that renders the stylish table with contests
 */
val contestList: FC<Props> = FC {
    val (isContestEnrollerModalOpen, setIsContestEnrollerModalOpen) = useState(false)
    val (isConfirmationModalOpen, setIsConfirmationModalOpen) = useState(false)
    val (enrollmentResponseString, setEnrollmentResponseString) = useState("")
    val (selectedContestName, setSelectedContestName) = useState("")
    showContestEnrollerModal(
        isContestEnrollerModalOpen,
        ContestNameProps(selectedContestName),
        { setIsContestEnrollerModalOpen(false) }
    ) {
        setEnrollmentResponseString(it)
        setIsConfirmationModalOpen(true)
        setIsContestEnrollerModalOpen(false)
    }

    displayModal(
        isConfirmationModalOpen,
        "Contest Enroller",
        enrollmentResponseString,
        mediumTransparentModalStyle,
        { setIsConfirmationModalOpen(false) }
    ) {
        buttonBuilder("Close", "secondary") {
            setIsConfirmationModalOpen(false)
        }
    }

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
            .unsafeMap { it.decodeFromJsonString() }
        setFinishedContests(contests.toSet())
    }

    val (selectedTab, setSelectedTab) = useState(ContestTypesTab.ACTIVE)
    div {
        className = ClassName("col-6")
        div {
            className = ClassName("card flex-md-row flex-wrap d-block mb-1 box-shadow")
            style = jso {
                @Suppress("MAGIC_NUMBER")
                minHeight = 20.rem
                height = "100%".unsafeCast<Height>()
            }

            div {
                className = ClassName("col")

                title(" Available Contests", faCode)

                tab(selectedTab.name, ContestTypesTab.values().map { it.name }) {
                    setSelectedTab(ContestTypesTab.valueOf(it))
                }

                contests(selectedTab, activeContests, finishedContests) {
                    setSelectedContestName(it)
                    setIsContestEnrollerModalOpen(true)
                }
            }
        }
    }
}

/**
 * this enum is used in a tab for contests
 */
enum class ContestTypesTab {
    ACTIVE, FINISHED, PLANNED
}

private fun ChildrenBuilder.contests(
    selectedTab: ContestTypesTab,
    activeContests: Set<ContestDto>,
    finishedContests: Set<ContestDto>,
    onEnrollButtonPressed: (String) -> Unit,
) {
    when (selectedTab) {
        ContestTypesTab.ACTIVE -> contestListTable(activeContests, onEnrollButtonPressed)
        ContestTypesTab.FINISHED -> contestListTable(finishedContests, null)
        ContestTypesTab.PLANNED -> {
            // FixMe: Add planned contests
        }
    }
}

private fun ChildrenBuilder.contestListTable(
    contests: Set<ContestDto>,
    onEnrollButtonPressed: ((String) -> Unit)?,
) {
    contests.forEach { contest ->
        div {
            className = ClassName("media text-muted pb-3")
            img {
                className = ClassName("rounded")
                src = "/img/undraw_code_inspection_bdl7.svg"
                style = jso {
                    @Suppress("MAGIC_NUMBER")
                    width = 4.2.rem
                }
            }

            div {
                className = ClassName("media-body pb-3 mb-0 small lh-125 border-bottom border-gray text-left")
                strong {
                    className = ClassName("d-block text-gray-dark")
                    +contest.name
                }
                +(contest.description ?: "")

                div {
                    className = ClassName("navbar-landing mt-3")
                    button {
                        type = ButtonType.button
                        className = ClassName("btn btn-outline-success ml-auto mr-2")
                        disabled = onEnrollButtonPressed == null
                        onClick = {
                            onEnrollButtonPressed?.let {
                                onEnrollButtonPressed(contest.name)
                            }
                        }
                        +"Enroll"
                    }
                    Link {
                        className = ClassName("btn btn-outline-info mr-2")
                        to = "/${FrontendRoutes.CONTESTS}/${contest.name}"
                        +"Rules and more "
                        fontAwesomeIcon(icon = faArrowRight)
                    }
                }
            }
        }
    }
}
