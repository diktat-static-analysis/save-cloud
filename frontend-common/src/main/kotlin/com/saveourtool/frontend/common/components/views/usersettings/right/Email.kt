/**
 * rendering for Email and Login management card
 */

package com.saveourtool.frontend.common.components.views.usersettings.right

import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.views.usersettings.SettingsProps
import com.saveourtool.frontend.common.components.views.usersettings.inputForm
import com.saveourtool.frontend.common.components.views.usersettings.right.validation.validateLogin
import com.saveourtool.frontend.common.components.views.usersettings.right.validation.validateUserEmail
import com.saveourtool.frontend.common.components.views.usersettings.useSaveUser
import com.saveourtool.frontend.common.externals.i18next.useTranslation
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.img
import react.router.dom.Link
import react.useState
import web.cssom.ClassName
import web.cssom.rem

val emailSettingsCard: FC<SettingsProps> = FC { props ->
    val (settingsInputFields, setSettingsInputFields) = useState(SettingsInputFields())
    val saveUser = useSaveUser(props, settingsInputFields, setSettingsInputFields)
    val (t) = useTranslation("profile")

    div {
        className = ClassName("row justify-content-center mt-5")
        img {
            src = "/img/settings_icon1.png"
            style = jso {
                height = 10.rem
                width = 10.rem
            }
        }
    }

    div {
        className = ClassName("d-sm-flex align-items-center justify-content-center mb-4 mt-4")
        h3 {
            className = ClassName("mt-2 mr-2 text-gray-800")
            +"Login and Email"
        }

        Link {
            to = "/${FrontendRoutes.TERMS_OF_USE}"
            buttonBuilder(
                "terms of usage",
                style = "outline-secondary",
                classes = "rounded-pill btn-sm",
                isOutline = false
            ) {
                }
        }
    }

    div {
        className = ClassName("col mt-2 px-5")
        inputForm(
            props.userInfo?.name,
            InputTypes.LOGIN,
            settingsInputFields,
            setSettingsInputFields,
            translate = t,
            colRatio = "col-2" to "col-6",
        ) { validateLogin() }

        inputForm(
            props.userInfo?.email,
            InputTypes.USER_EMAIL,
            settingsInputFields,
            setSettingsInputFields,
            translate = t,
            colRatio = "col-2" to "col-6"
        ) { validateUserEmail() }

        div {
            className = ClassName("row justify-content-center")
            div {
                className = ClassName("col-8")
                @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
                hr { }
            }
        }

        div {
            className = ClassName("row justify-content-center")
            buttonBuilder("Save changes", style = "primary", isDisabled = settingsInputFields.containsError()) {
                saveUser()
            }
        }
    }
}
