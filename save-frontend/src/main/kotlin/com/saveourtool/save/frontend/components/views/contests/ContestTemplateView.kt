@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.common.entities.contest.ContestSampleDto
import com.saveourtool.common.entities.contest.ContestSampleFieldDto
import com.saveourtool.common.entities.contest.ContestSampleFieldType
import com.saveourtool.common.info.UserInfo
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopLoadingHandler

import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.textarea
import react.useState
import web.cssom.*
import web.html.InputType

val contestTemplateView: FC<ContestTemplateViewProps> = FC { props ->
    useBackground(Style.SAVE_LIGHT)

    val (contestTemplate, setContestTemplate) = useState(ContestSampleDto.empty)
    val (contestTemplateField, setContestTemplateField) = useState<List<ContestSampleFieldDto>>(emptyList())

    useRequest {
        val contestTemplateNew: ContestSampleDto = get(
            url = "$apiUrl/contests/sample/get?id=${props.id}",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }

        setContestTemplate(contestTemplateNew)

        val contestTemplateFieldNew: List<ContestSampleFieldDto> = get(
            url = "$apiUrl/contests/sample/get-fields/by-sample-id?id=${props.id}",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }

        setContestTemplateField(contestTemplateFieldNew)
    }

    div {
        className = ClassName("card card-body mt-0")
        h1 {
            className = ClassName("h3 mb-0 text-center text-gray-800")
            +contestTemplate.name
        }

        div {
            className = ClassName("row justify-content-center")

            // ===================== LEFT COLUMN =======================================================================
            div {
                className = ClassName("col-2 mr-3")
                div {
                    className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +""
                }

                div {
                    className = ClassName("card shadow mb-4")
                    div {
                        className = ClassName("card-header py-3")
                        div {
                            className = ClassName("row")
                            h6 {
                                className = ClassName("m-0 font-weight-bold text-primary")
                                style = jso {
                                    display = Display.flex
                                    alignItems = AlignItems.center
                                }
                                +"Description"
                            }
                        }
                    }
                    @Suppress("MAGIC_NUMBER", "MagicNumber")
                    div {
                        className = ClassName("card-body")
                        textarea {
                            className = ClassName("auto_height form-control-plaintext pt-0 pb-0")
                            value = "${contestTemplate.description}"
                            rows = 8
                            disabled = true
                        }
                    }
                }
            }

            // ===================== RIGHT COLUMN =======================================================================
            div {
                className = ClassName("col-6")

                div {
                    className = ClassName("mt-5 text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Contest template fields"
                }

                contestTemplateField.forEach { field ->

                    div {
                        className = ClassName("row")

                        div {
                            className = ClassName("col-8 pl-2 pr-2 mt-3 text-left")

                            input {
                                type = InputType.text
                                className = ClassName("form-control")
                                defaultValue = field.name
                                required = false
                                disabled = true
                            }
                        }

                        div {
                            className = ClassName("col-4 pl-2 pr-2 mt-3 input-group-sm input-group")
                            select {
                                className = ClassName("form-control")
                                style = jso {
                                    height = "100%".unsafeCast<Height>()
                                }
                                ContestSampleFieldType.values().map { it.toString() }.forEach {
                                    option {
                                        className = ClassName("list-group-item")
                                        val entries = it
                                        value = entries
                                        +entries
                                    }
                                    value = field.type
                                    disabled = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * [Props] for ContestTemplateView
 */
external interface ContestTemplateViewProps : Props {
    /**
     * Id of contest template
     */
    var id: Long

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo?
}
