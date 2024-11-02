/**
 * On Index view we should have a small personal feed for the user with all useful links. It will be implemented here.
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.common.entities.OrganizationDto
import com.saveourtool.common.entities.OrganizationStatus
import com.saveourtool.common.validation.FrontendCosvRoutes
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.basic.renderAvatar
import com.saveourtool.frontend.common.components.basic.renderOrganizationAvatar
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.save.frontend.externals.i18next.useTranslation

import io.ktor.util.*
import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.p
import react.router.dom.Link
import react.router.useNavigate
import react.useState
import web.cssom.ClassName
import web.cssom.TextAlign
import web.cssom.rem

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

// FixMe: Some statistics: may be how many users used your demo or how many contests you created,
// FixMe: Statistics about demo

@Suppress(
    "PARAMETER_NAME_IN_OUTER_LAMBDA",
    "LONG_LINE",
)
val cardUser: FC<UserInfoAwareProps> = FC { props ->
    val (t) = useTranslation("index")
    val (organizations, setOrganizations) = useState(emptyList<OrganizationDto>())
    val (errorAvatars, setErrorAvatars) = useState(emptySet<String>())
    val navigate = useNavigate()

    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    useRequest {
        props.userInfo?.name?.let {
            val organizationsNew: List<OrganizationDto> = get(
                "$apiUrl/organizations/get/list-by-user-name?userName=$it",
                jsonHeaders,
                loadingHandler = ::loadingHandler,
            )
                .decodeFromJsonString()

            setOrganizations(organizationsNew)
        }
    }

    div {
        className = ClassName("col-3 shadow mx-3 mt-2")
        div {
            className = ClassName("row d-flex justify-content-center")
            cardImage("/img/icon1.png")
        }

        div {
            className = ClassName("row d-flex justify-content-center text-gray-900 mt-2")
            h5 {
                style = jso {
                    textAlign = TextAlign.center
                }
                +"${"Welcome".t()}${props.userInfo?.name?.let { ", " } ?: ""}"
                Link {
                    to = "/${FrontendRoutes.PROFILE}/${props.userInfo?.name}"
                    b {
                        +(props.userInfo?.name?.let { " @$it " } ?: "")
                    }
                }
                +"!"
            }
        }

        div {
            className = ClassName("row d-flex justify-content-center")
            @Suppress("MAGIC_NUMBER")
            div {
                className = ClassName("col-3")
                renderAvatar(
                    props.userInfo,
                    "align-self-center avatar avatar-user width-full border color-bg-default rounded-circle fas mx-2"
                ) {
                    height = 4.rem
                    width = 4.rem
                }
            }

            div {
                className = ClassName("col-9")
                props.userInfo?.let {
                    p {
                        +"${"Registered since".t()}: ${it.createDate?.toInstant(TimeZone.UTC)?.toLocalDateTime(TimeZone.UTC)
                            .let { date ->
                                "${date?.dayOfMonth} ${date?.month?.name?.toLowerCasePreservingASCIIRules()} ${date?.year}"
                            }} !"
                    }
                    p {
                        +"The easiest way to start working with our Ecosystem is to create your organization.".t()
                    }

                    div {
                        className = ClassName("row d-flex justify-content-center mt-1")
                        buttonBuilder(
                            "Profile Settings".t(),
                            style = "primary rounded-pill",
                            isOutline = false
                        ) {
                            navigate(to = "/${FrontendRoutes.SETTINGS_PROFILE}")
                        }
                    }
                }
                    ?: run {
                        p {
                            +"For the better User Experience we recommend you to register.".t()
                        }

                        div {
                            className = ClassName("row d-flex justify-content-center")

                            buttonBuilder(
                                "Vulnerabilities Archive".t(),
                                style = "primary rounded-pill",
                                isOutline = false
                            ) {
                                navigate(to = "/${FrontendCosvRoutes.VULNERABILITIES}")
                            }
                        }

                        div {
                            className = ClassName("row d-flex justify-content-center mt-1")
                            buttonBuilder(
                                "Save Cloud Platform".t(),
                                style = "primary rounded-pill",
                                isOutline = false
                            ) {
                                navigate(to = "/${FrontendRoutes.PROJECTS}")
                            }
                        }
                    }
            }
        }

        hr {
            className = ClassName("mt-3 px-3")
        }

        props.userInfo?.let {
            div {
                className = ClassName("mt-2")
                div {
                    className = ClassName("row d-flex justify-content-center text-gray-900 mt-2")
                    h5 {
                        style = jso {
                            textAlign = TextAlign.center
                        }
                        +"${"Your organizations".t()}:"
                    }
                }
                if (organizations.isEmpty()) {
                    div {
                        className = ClassName("row d-flex justify-content-center mt-1")
                        buttonBuilder(
                            "Create".t(),
                            style = "primary rounded-pill",
                            isOutline = false
                        ) {
                            navigate(to = "/${FrontendRoutes.CREATE_ORGANIZATION}")
                        }
                    }
                } else {
                    organizations.forEach { organization ->
                        div {
                            className = ClassName("row")
                            div {
                                className = ClassName("col-12 mt-2 pl-4")
                                val renderImg: ChildrenBuilder.() -> Unit = {
                                    renderOrganizationAvatar(
                                        organization,
                                        errorAvatars = errorAvatars,
                                        setErrorAvatars = { name -> setErrorAvatars(errorAvatars.plus(name)) }
                                    ) {
                                        height = 2.rem
                                        width = 2.rem
                                    }
                                    +" ${organization.name}"
                                }
                                if (organization.status != OrganizationStatus.DELETED) {
                                    Link {
                                        to = "/${organization.name}"
                                        renderImg()
                                    }
                                } else {
                                    renderImg()
                                }
                            }
                        }
                    }
                }
            }

            hr {
                className = ClassName("mt-3 px-3")
            }

            div {
                className = ClassName("mt-2")
                div {
                    className = ClassName("row d-flex justify-content-center text-gray-900 mt-2 mb-2")
                    h5 {
                        style = jso {
                            textAlign = TextAlign.center
                        }
                        +"${"Your statistics".t()}:"
                    }
                }

                div {
                    className = ClassName("row text-muted border-bottom border-gray mx-3 mt-2")
                    div {
                        className = ClassName("col-9")
                        p {
                            +"${"Top rating".t()}: "
                        }
                    }

                    div {
                        className = ClassName("col-3")
                        p {
                            Link {
                                to = "/${FrontendCosvRoutes.VULN_TOP_RATING}"
                                +"${props.userInfo?.rating ?: 0}"
                            }
                        }
                    }
                }
            }
        }
    }
}
