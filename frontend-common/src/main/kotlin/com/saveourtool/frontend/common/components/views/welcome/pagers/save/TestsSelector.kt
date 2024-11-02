package com.saveourtool.frontend.common.components.views.welcome.pagers.save

import com.saveourtool.frontend.common.components.views.welcome.pagers.WelcomePager
import com.saveourtool.frontend.common.externals.animations.*

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.img
import web.cssom.Height
import web.cssom.rem

@Suppress("CUSTOM_GETTERS_SETTERS", "MAGIC_NUMBER")
object TestsSelector : WelcomePager {
    override val animation: Animation
        get() = batch(fade(), sticky())

    override fun renderPage(childrenBuilder: ChildrenBuilder) {
        childrenBuilder.renderAnimatedPage()
    }

    private fun ChildrenBuilder.renderAnimatedPage() {
        img {
            style = jso {
                width = 40.rem
                height = "auto".unsafeCast<Height>()
            }
            src = "/img/tests_selector.png"
        }
    }
}
