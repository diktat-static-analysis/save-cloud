/**
 * A view with organization creation details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.entities.*
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.common.validation.isValidLengthName
import com.saveourtool.frontend.common.components.basic.AVATAR_ORGANIZATION_PLACEHOLDER
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.inputform.inputTextFormRequired
import com.saveourtool.frontend.common.components.modal.displayModal
import com.saveourtool.frontend.common.components.modal.mediumTransparentModalStyle
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.span
import react.router.useNavigate
import web.cssom.ClassName
import web.cssom.rem
import web.html.ButtonType

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val createOrganizationView: FC<Props> = FC {
    useBackground(Style.SAVE_DARK)
    particles()

    val (isErrorWithOrganizationSave, setIsErrorWithOrganizationSave) = useState(false)
    val (errorMessage, setErrorMessage) = useState("")
    val (organizationDto, setOrganizationDto) = useState(OrganizationDto.empty)
    val (conflictErrorMessage, setConflictErrorMessage) = useState<String?>(null)
    val navigate = useNavigate()
    val modalWindowOpenness = useWindowOpenness()

    val saveOrganization = useDeferredRequest {
        val responseFromCreationOrganization = post(
            "$apiUrl/organizations/save",
            jsonHeaders,
            Json.encodeToString(organizationDto),
            loadingHandler = ::loadingHandler,
            responseHandler = ::responseHandlerWithValidation,
        )
        if (responseFromCreationOrganization.ok) {
            navigate(to = "/${FrontendRoutes.SETTINGS_ORGANIZATIONS}")
        } else if (responseFromCreationOrganization.isConflict()) {
            val responseText = responseFromCreationOrganization.unpackMessage()
            setConflictErrorMessage(responseText)
        } else if (!responseFromCreationOrganization.isUnauthorized()) {
            responseFromCreationOrganization.unpackMessage().let { message ->
                setIsErrorWithOrganizationSave(true)
                setErrorMessage(message)
            }
        }
    }

    displayModal(
        isErrorWithOrganizationSave,
        "Creation error",
        errorMessage,
        mediumTransparentModalStyle,
        modalWindowOpenness.closeWindowAction(),
    ) {
        buttonBuilder("Close", "secondary") {
            setIsErrorWithOrganizationSave(false)
        }
    }

    main {
        className = ClassName("main-content mt-0 ps")
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
                                +"Create new organization"
                            }
                            img {
                                className =
                                        ClassName("avatar avatar-user width-full border color-bg-default rounded-circle mb-4")
                                src = AVATAR_ORGANIZATION_PLACEHOLDER
                                style = jso {
                                    width = 8.rem
                                }
                            }
                            form {
                                className = ClassName("needs-validation")
                                div {
                                    inputTextFormRequired {
                                        form = InputTypes.ORGANIZATION_NAME
                                        conflictMessage = conflictErrorMessage
                                        textValue = organizationDto.name
                                        validInput =
                                                organizationDto.name.isNotEmpty() && organizationDto.validateName() &&
                                                        organizationDto.name.isValidLengthName() && conflictErrorMessage == null
                                        classes = ""
                                        name = "Organization name"
                                        onChangeFun = {
                                            setOrganizationDto(organizationDto.copy(name = it.target.value))
                                            setConflictErrorMessage(null)
                                        }
                                    }
                                }
                            }
                            button {
                                type = ButtonType.button
                                className = ClassName("btn btn-info mt-4")
                                +"Create organization"
                                disabled = !organizationDto.validate() || conflictErrorMessage != null
                                onClick = {
                                    saveOrganization()
                                }
                            }
                            conflictErrorMessage?.let {
                                div {
                                    className = ClassName("invalid-feedback d-block")
                                    +it
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
