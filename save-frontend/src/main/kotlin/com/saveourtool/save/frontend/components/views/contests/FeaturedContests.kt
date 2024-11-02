/**
 * Preparation for a participation card
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.frontend.common.components.basic.carousel
import com.saveourtool.frontend.common.components.modal.displayModal
import com.saveourtool.frontend.common.components.modal.mediumTransparentModalStyle
import com.saveourtool.frontend.common.externals.fontawesome.faArrowRight
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.save.frontend.components.basic.ContestNameProps
import com.saveourtool.save.frontend.components.basic.showContestEnrollerModal

import js.core.jso
import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong
import react.router.dom.Link
import web.cssom.*
import web.html.ButtonType

@Suppress("MAGIC_NUMBER")
internal val featuredContests: FC<Props> = FC {
    val (featuredContests, setFeaturedContests) = useState<List<ContestDto>>(emptyList())
    useRequest {
        val contests: List<ContestDto> = get(
            url = "$apiUrl/contests/featured/list-active",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap { it.decodeFromJsonString() }
        setFeaturedContests(contests)
    }

    val (selectedContest, setSelectedContest) = useState(ContestDto.empty)
    val (isConfirmationWindowOpen, setIsConfirmationWindowOpen) = useState(false)
    val (isContestEnrollerModalOpen, setIsContestEnrollerModalOpen) = useState(false)
    val (enrollerResponseMessage, setEnrollerResponseMessage) = useState("")
    showContestEnrollerModal(
        isContestEnrollerModalOpen,
        ContestNameProps(selectedContest.name),
        { setIsContestEnrollerModalOpen(false) }
    ) {
        setIsConfirmationWindowOpen(true)
        setIsContestEnrollerModalOpen(false)
        setEnrollerResponseMessage(it)
    }

    displayModal(
        isConfirmationWindowOpen,
        "Contest enroller",
        enrollerResponseMessage,
        mediumTransparentModalStyle,
        { setIsConfirmationWindowOpen(false) }
    ) {
        buttonBuilder("Close", "secondary") {
            setIsConfirmationWindowOpen(false)
        }
    }

    div {
        className = ClassName("col-8")
        if (featuredContests.isEmpty()) {
            div {
                className = ClassName("card flex-md-row box-shadow")
                style = jso {
                    height = 15.rem
                }
                stayTuned()
            }
        } else {
            carousel(featuredContests, "contestCarousel", jso { height = 15.rem }, "card") { contestToShow ->
                div {
                    className = ClassName("col-3 ml-auto")
                    img {
                        style = jso {
                            width = 12.rem
                        }
                        // FixMe: we need to have information about the programming language in contest and show an icon
                        src = "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/devicon/devicon-original.svg"
                    }
                }

                div {
                    className = ClassName("col-6 mr-auto")
                    contestDescription(contestToShow, setSelectedContest, setIsContestEnrollerModalOpen)
                }
            }
        }
    }
}

private fun ChildrenBuilder.stayTunedImage() {
    img {
        className = ClassName("card-img-right flex-auto d-none d-md-block")
        src = "/img/undraw_notify_re_65on.svg"
        style = jso {
            @Suppress("MAGIC_NUMBER")
            width = 24.rem
        }
    }
}

private fun ChildrenBuilder.stayTuned() {
    div {
        className = ClassName("card-body d-flex flex-column align-items-start")
        strong {
            className = ClassName("d-inline-block mb-2 text-primary")
            +"Featured Contest"
        }
        h3 {
            className = ClassName("mb-0 text-dark")
            +"Stay turned..."
        }
        p {
            className = ClassName("card-text mb-auto")
            +("Right now there is no contest that we would recommend you to participate in, but it is going to change soon. " +
                    "Stay turned and soon we will find good contests for you and your tools!")
        }
    }
    stayTunedImage()
}

private fun ChildrenBuilder.contestDescription(
    contestToShow: ContestDto,
    setSelectedContest: StateSetter<ContestDto>,
    setIsContestEnrollerModalOpen: StateSetter<Boolean>
) {
    div {
        className = ClassName("card-body d-flex flex-column align-items-start")
        strong {
            className = ClassName("d-inline-block mb-2 text-info")
            +"Featured Contest"
        }
        h3 {
            className = ClassName("mb-0")
            Link {
                className = ClassName("text-dark")
                to = "/contests/${contestToShow.name}"
                +contestToShow.name
            }
        }
        p {
            className = ClassName("card-text mb-auto")
            +(contestToShow.description ?: "No description provided yet.")
        }
        p {
            className = ClassName("card-text mb-auto")
            +("Created by: ${contestToShow.organizationName}")
        }
        div {
            className = ClassName("row mt-1")
            button {
                type = ButtonType.button
                className = ClassName("btn btn-sm btn-outline-primary mr-1")
                onClick = {
                    setSelectedContest(contestToShow)
                    setIsContestEnrollerModalOpen(true)
                }
                +"Enroll"
            }

            Link {
                className = ClassName("btn btn-sm btn-outline-success")
                to = "/contests/${contestToShow.name}"
                +"Description "
                fontAwesomeIcon(icon = faArrowRight)
            }
        }
    }
}
