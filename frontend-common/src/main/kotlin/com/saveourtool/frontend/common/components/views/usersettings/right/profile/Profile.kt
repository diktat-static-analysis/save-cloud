/**
 * rendering for Profile management card
 */

@file:Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")

package com.saveourtool.frontend.common.components.views.usersettings.right.profile

import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.views.usersettings.*
import com.saveourtool.frontend.common.components.views.usersettings.FieldsStateSetter
import com.saveourtool.frontend.common.components.views.usersettings.SettingsProps
import com.saveourtool.frontend.common.components.views.usersettings.inputForm
import com.saveourtool.frontend.common.components.views.usersettings.right.SettingsInputFields
import com.saveourtool.frontend.common.components.views.usersettings.right.validation.*
import com.saveourtool.frontend.common.components.views.usersettings.useSaveUser
import com.saveourtool.frontend.common.externals.i18next.TranslationFunction
import com.saveourtool.frontend.common.externals.i18next.useTranslation
import com.saveourtool.frontend.common.utils.*

import react.ChildrenBuilder
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.textarea
import react.useState
import web.cssom.*

const val AVATARS_PACKAGE_COUNT = 9

val profileSettingsCard: FC<SettingsProps> = FC { props ->
    val (t) = useTranslation("profile")

    // === states ===
    val (settingsInputFields, setSettingsInputFields) = useState(SettingsInputFields())
    val saveUser = useSaveUser(props, settingsInputFields, setSettingsInputFields)

    // === design ===
    div {
        className = ClassName("row px-5 mt-3")

        hr { }

        div {
            className = ClassName("col-6 text-center")
            div {
                className = ClassName("row mb-2")
                h4 {
                    +"Add bio and info:".t()
                }
            }
            div {
                className = ClassName("row pr-5")
                div {
                    className = ClassName("input-group needs-validation")
                    textarea {
                        className = ClassName("form-control shadow")
                        onChange = {
                            val newSettingsInputFields =
                                    settingsInputFields.updateValue(InputTypes.FREE_TEXT, it.target.value, "")
                            setSettingsInputFields(newSettingsInputFields)
                        }
                        placeholder = "400 characters".t()
                        defaultValue = props.userInfo?.freeText
                        rows = 10
                        maxLength = 400
                    }
                }
            }
        }

        div {
            className = ClassName("col-6")
            div {
                className = ClassName("row mb-2")
                h4 {
                    +"Upload or select avatar:".t()
                }
            }

            avatarSelector {
                this.userInfo = props.userInfo
                this.userInfoSetter = props.userInfoSetter
            }
        }
    }

    div {
        className = ClassName("row")

        div {
            className = ClassName("col mt-2 px-5")
            extraInformation(t, props, settingsInputFields, setSettingsInputFields)

            div {
                className = ClassName("row justify-content-center")
                buttonBuilder("Save changes", style = "primary", isDisabled = settingsInputFields.containsError()) {
                    saveUser()
                }
            }
        }
    }
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun ChildrenBuilder.extraInformation(
    translate: TranslationFunction,
    props: SettingsProps,
    settingsInputFields: SettingsInputFields,
    setSettingsInputFields: FieldsStateSetter
) {
    hr { }

    inputForm(
        props.userInfo?.realName,
        InputTypes.REAL_NAME,
        settingsInputFields,
        setSettingsInputFields,
        translate,
        "e.g. John Smith"
    ) { validateRealName() }

    inputForm(
        props.userInfo?.company,
        InputTypes.COMPANY,
        settingsInputFields,
        setSettingsInputFields,
        translate,
        "e.g. FutureWay Inc."
    ) { validateCompany() }

    inputForm(
        props.userInfo?.location,
        InputTypes.LOCATION,
        settingsInputFields,
        setSettingsInputFields,
        translate,
        "Beijing, China"
    ) { validateLocation() }

    inputForm(
        props.userInfo?.website,
        InputTypes.WEBSITE,
        settingsInputFields,
        setSettingsInputFields,
        translate,
        "https://saveourtool.com"
    ) { validateWebsite() }

    inputForm(
        props.userInfo?.linkedin,
        InputTypes.LINKEDIN,
        settingsInputFields,
        setSettingsInputFields,
        translate,
        "https://linkedin.com/yourname"
    ) { validateLinkedIn() }

    inputForm(
        props.userInfo?.gitHub,
        InputTypes.GITHUB,
        settingsInputFields,
        setSettingsInputFields,
        translate,
        "https://github.com/yourname"
    ) { validateGithub() }

    inputForm(
        props.userInfo?.twitter,
        InputTypes.TWITTER,
        settingsInputFields,
        setSettingsInputFields,
        translate,
        "https://x.com/yourname"
    ) { validateTwitter() }

    hr { }
}
