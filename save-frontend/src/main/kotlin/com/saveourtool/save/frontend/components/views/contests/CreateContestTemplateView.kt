@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.common.entities.contest.ContestSampleDto
import com.saveourtool.common.entities.contest.ContestSampleFieldDto
import com.saveourtool.common.entities.contest.ContestSampleFieldType
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.inputform.inputTextFormRequired
import com.saveourtool.frontend.common.utils.*

import react.FC
import react.Props
import react.dom.aria.ariaDescribedBy
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.textarea
import react.router.useNavigate
import react.useState
import web.cssom.ClassName
import web.html.InputType

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val createContestTemplateView: FC<Props> = FC {
    useBackground(Style.SAVE_DARK)

    val (contestTemplate, setContestTemplate) = useState(ContestSampleDto.empty)
    val (mapField) = useState<MutableMap<Int, ContestSampleFieldDto>>(mutableMapOf())
    val (number, setNumber) = useState(0)

    val navigate = useNavigate()

    val enrollRequest = useDeferredRequest {
        val contestSampleWithField = contestTemplate.copy(fields = mapField.map { it.value })
        val response = post(
            url = "$apiUrl/contests/sample/save",
            headers = jsonHeaders,
            body = Json.encodeToString(contestSampleWithField),
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            navigate(to = "/${FrontendRoutes.CONTESTS}")
        }
    }

    div {
        className = ClassName("page-header align-items-start min-vh-100")
        span {
            className = ClassName("mask bg-gradient-dark opacity-6")
        }
        div {
            className = ClassName("row justify-content-center")
            div {
                className = ClassName("col-sm-4 mt-5")
                div {
                    className = ClassName("container card o-hidden border-0 shadow-lg my-2 card-body p-0")
                    div {
                        className = ClassName("p-5 text-center")
                        h1 {
                            className = ClassName("h4 text-gray-900 mb-4")
                            +"Create a new contest template"
                        }
                        form {
                            className = ClassName("needs-validation")
                            div {
                                className = ClassName("row-3")

                                inputTextFormRequired {
                                    form = InputTypes.CONTEST_TEMPLATE_NAME
                                    conflictMessage = "Name must not be empty"
                                    textValue = contestTemplate.name
                                    validInput = contestTemplate.name.isNotBlank()
                                    classes = "col-12 pl-2 pr-2 mt-3 text-left"
                                    name = "Contest template name:"
                                    onChangeFun = { event ->
                                        setContestTemplate { it.copy(name = event.target.value) }
                                    }
                                }

                                div {
                                    className = ClassName("col-12 mt-3 mb-3 pl-2 pr-2 text-left")
                                    label {
                                        className = ClassName("form-label")
                                        +"Description"
                                    }
                                    div {
                                        className = ClassName("input-group needs-validation")
                                        textarea {
                                            className = ClassName("form-control")
                                            onChange = { event ->
                                                setContestTemplate { it.copy(description = event.target.value) }
                                            }
                                            ariaDescribedBy = "${InputTypes.DESCRIPTION.name}Span"
                                            rows = 2
                                            id = InputTypes.DESCRIPTION.name
                                        }
                                    }
                                }

                                buttonBuilder("Add field", classes = "mt-3") {
                                    setNumber { it + 1 }
                                    mapField[number + 1] = ContestSampleFieldDto.empty
                                }

                                mapField.forEach { sample ->

                                    div {
                                        className = ClassName("row")

                                        div {
                                            className = ClassName("col-8 pl-2 pr-2 mt-3 text-left")

                                            input {
                                                type = InputType.text
                                                className = ClassName("form-control")
                                                defaultValue = sample.value.name
                                                required = false
                                                onChange = {
                                                    mapField[sample.key] = ContestSampleFieldDto(
                                                        it.target.value,
                                                        mapField[sample.key]?.type ?: ContestSampleFieldType.NUMBER,
                                                    )
                                                }
                                            }
                                        }

                                        div {
                                            className = ClassName("col-4 pl-2 pr-2 mt-3 input-group-sm input-group")
                                            select {
                                                className = ClassName("form-control")
                                                ContestSampleFieldType.values().map { it.toString() }.forEach {
                                                    option {
                                                        className = ClassName("list-group-item")
                                                        val entries = it
                                                        value = entries
                                                        +entries
                                                    }
                                                }
                                                onChange = { event ->
                                                    val entries = event.target.value
                                                    mapField[sample.key] = ContestSampleFieldDto(
                                                        mapField[sample.key]?.name ?: "",
                                                        ContestSampleFieldType.valueOf(entries.uppercase())
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            buttonBuilder(
                                "Create a contest template",
                                style = "info",
                                classes = "mt-4",
                                isDisabled = contestTemplate.name.isEmpty()
                            ) {
                                enrollRequest()
                            }
                        }
                    }
                }
            }
        }
    }
}
