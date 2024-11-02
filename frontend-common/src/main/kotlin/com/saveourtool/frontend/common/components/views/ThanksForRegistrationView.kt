/**
 * View for users that are being reviewed until approve status gotten
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.views

import com.saveourtool.common.info.UserStatus
import com.saveourtool.frontend.common.externals.i18next.useTranslation
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.FC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong
import web.cssom.ClassName
import web.cssom.rem

val thanksForRegistrationView: FC<UserInfoAwareProps> = FC { props ->
    useBackground(Style.INDEX)
    particles()
    val (t) = useTranslation("thanks-for-registration")

    useRedirectToIndexIf(props.userInfo?.status) {
        // life hack ot be sure that props are loaded
        props.key != null && props.userInfo?.status != UserStatus.NOT_APPROVED
    }

    div {
        className = ClassName("col text-center d-flex justify-content-center align-items-center")
        style = jso {
            height = 52.rem
        }

        div {
            className = ClassName("col-5 bg-light mb-5 p-3 rounded")
            img {
                className = ClassName("avatar avatar-user border color-bg-default rounded-circle")
                style = jso {
                    width = 20.rem
                }
                src = "/img/avatar_packs/avatar1.png"
            }

            div {
                className = ClassName("p-2")
                h2 {
                    className = ClassName("mx-auto")
                    +"${"Thank you for registration".t()}!"
                }

                h4 {
                    className = ClassName("mx-auto")
                    b {
                        +"${"You need to wait until you will be reviewed and approved by admins".t()}!"
                    }
                }

                p {
                    className = ClassName("lead text-gray-800 mb-0")
                    +"Your request is pending review".t()
                    a {
                        className = ClassName("ml-1")
                        href = "mailto:saveourtool@gmail.com"
                        strong {
                            className = ClassName("d-inline-block")
                            +"  saveourtool@gmail.com."
                        }
                    }
                }
            }
        }
    }
}
