/**
 * View for Ban
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.common.info.UserStatus
import com.saveourtool.frontend.common.utils.Style
import com.saveourtool.frontend.common.utils.UserInfoAwareProps
import com.saveourtool.frontend.common.utils.useBackground
import com.saveourtool.frontend.common.utils.useRedirectToIndexIf

import js.core.jso
import react.FC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong
import web.cssom.ClassName
import web.cssom.rem

private const val BAN = """
    BAN
"""

private const val SUPPORT = """
    You have been banned from our platform. If you believe this was a mistake, please contact our support team at
"""

val banView: FC<UserInfoAwareProps> = FC { props ->
    useBackground(Style.SAVE_LIGHT)

    useRedirectToIndexIf(props.userInfo?.status) {
        // life hack ot be sure that props are loaded
        props.key != null && props.userInfo?.status != UserStatus.BANNED
    }

    div {
        className = ClassName("col text-center")
        style = jso {
            height = 40.rem
        }

        div {
            className = ClassName("col error mx-auto mt-5")
            asDynamic()["data-text"] = BAN
            +BAN
        }

        p {
            className = ClassName("lead text-gray-800 mb-3")
            +SUPPORT
            a {
                href = "mailto:saveourtool@gmail.com"
                strong {
                    className = ClassName("d-inline-block mb-2")
                    +" saveourtool@gmail.com."
                }
            }
        }
    }
}
