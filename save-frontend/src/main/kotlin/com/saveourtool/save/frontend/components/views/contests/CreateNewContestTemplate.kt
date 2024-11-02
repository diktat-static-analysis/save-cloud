@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.common.validation.FrontendRoutes
import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.img
import react.router.dom.Link
import web.cssom.*

internal val createNewContestTemplate: FC<Props> = FC {
    div {
        className = ClassName("col-2 text-center")
        Link {
            to = "/${FrontendRoutes.CREATE_CONTESTS_TEMPLATE}/"
            className = ClassName("col-12 flex-column card mb-1 box-shadow btn-hov")
            style = jso {
                @Suppress("MAGIC_NUMBER")
                height = 19.rem
                textDecoration = "none".unsafeCast<TextDecoration>()
            }
            div {
                className = ClassName("row mb-auto")
                img {
                    src = "/img/undraw_create_new.svg"
                    @Suppress("MAGIC_NUMBER")
                    style = jso {
                        width = 10.rem
                    }
                }
            }
            h3 {
                className = ClassName("mb-auto")
                +"Create new contest template"
            }
        }
    }
}
