@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.frontend.common.components.basic

import com.saveourtool.common.frontend.PlatformLanguages
import com.saveourtool.frontend.common.externals.cookie.cookie
import com.saveourtool.frontend.common.externals.cookie.getLanguageCode
import com.saveourtool.frontend.common.externals.cookie.isAccepted
import com.saveourtool.frontend.common.externals.i18next.changeLanguage
import com.saveourtool.frontend.common.externals.i18next.language
import com.saveourtool.frontend.common.externals.i18next.useTranslation

import js.core.jso
import react.FC
import react.Props
import react.dom.aria.AriaHasPopup
import react.dom.aria.ariaExpanded
import react.dom.aria.ariaHasPopup
import react.dom.aria.ariaLabelledBy
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import react.useEffect
import react.useState
import web.cssom.ClassName
import web.cssom.Cursor

private const val LANG_DROPDOWN_ID = "lang-dropdown"

/**
 * A [FC] that is responsible for language selection
 */
val languageSelector: FC<Props> = FC {
    val (_, i18n) = useTranslation("topbar")
    val languageFromCookie = if (cookie.isAccepted()) {
        PlatformLanguages.getByCodeOrDefault(cookie.getLanguageCode())
    } else {
        PlatformLanguages.defaultLanguage
    }
    val (language, setSelectedLanguage) = useState(languageFromCookie)

    useEffect(language) { i18n.changeLanguage(language) }

    div {
        className = ClassName("dropdown")
        a {
            className = ClassName("dropdown-toggle text-light")
            id = LANG_DROPDOWN_ID
            asDynamic()["data-toggle"] = "dropdown"
            ariaHasPopup = true.unsafeCast<AriaHasPopup>()
            ariaExpanded = false
            style = jso { cursor = "pointer".unsafeCast<Cursor>() }
            span { +i18n.language().label }
        }

        div {
            className = ClassName("dropdown-menu")
            ariaLabelledBy = LANG_DROPDOWN_ID
            PlatformLanguages.values().map { language ->
                a {
                    className = ClassName("dropdown-item")
                    style = jso { cursor = "pointer".unsafeCast<Cursor>() }
                    onClick = { setSelectedLanguage(language) }
                    span { +language.label }
                }
            }
        }
    }
}
