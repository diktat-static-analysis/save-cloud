@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.frontend.common.components

import generated.SAVE_CLOUD_VERSION
import react.*
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.footer
import react.dom.html.ReactHTML.span
import web.cssom.ClassName

/**
 * A web page footer component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
val footer: FC<Props> = FC {
    footer {
        className = ClassName("sticky-footer bg-white")
        div {
            className = ClassName("container my-auto")
            div {
                className = ClassName("copyright text-center my-auto")
                span {
                    +"Copyright ${js("String.fromCharCode(169)")} SAVE 2021-2022"
                    br {}
                    +"Version $SAVE_CLOUD_VERSION"
                }
            }
        }
    }
}
