/**
 * 4 pictures with animation and screenshots from SAVE
 */

package com.saveourtool.save.frontend.components.views.welcome.pagers.save

import com.saveourtool.frontend.common.externals.animations.*
import com.saveourtool.save.frontend.components.views.welcome.pagers.WelcomePager

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import web.cssom.Color
import web.cssom.rem

@Suppress("CUSTOM_GETTERS_SETTERS")
object GeneralInfoFirstPicture : WelcomePager {
    override val animation: Animation
        get() = fadeUpTopLeft

    override fun renderPage(childrenBuilder: ChildrenBuilder) {
        childrenBuilder.renderAnimatedPage()
    }

    private fun ChildrenBuilder.renderAnimatedPage() {
        h1 {
            style = jso {
                color = "rgb(6, 7, 89)".unsafeCast<Color>()
            }
            +"Easy to start"
        }
        img {
            @Suppress("MAGIC_NUMBER")
            style = jso {
                width = 30.rem
            }
            src = "/img/run_view.png"
        }
    }
}

@Suppress("CUSTOM_GETTERS_SETTERS")
object GeneralInfoSecondPicture : WelcomePager {
    override val animation: Animation
        get() = fadeUpTopRight

    override fun renderPage(childrenBuilder: ChildrenBuilder) {
        childrenBuilder.renderAnimatedPage()
    }

    private fun ChildrenBuilder.renderAnimatedPage() {
        h1 {
            style = jso {
                color = "rgb(6, 7, 89)".unsafeCast<Color>()
            }
            +"User-friendly dashboards"
        }
        img {
            @Suppress("MAGIC_NUMBER")
            style = jso {
                width = 30.rem
            }
            src = "/img/exec_view.png"
        }
    }
}

@Suppress("CUSTOM_GETTERS_SETTERS")
object GeneralInfoThirdPicture : WelcomePager {
    override val animation: Animation
        get() = fadeUpBottomLeft

    override fun renderPage(childrenBuilder: ChildrenBuilder) {
        childrenBuilder.renderAnimatedPage()
    }

    private fun ChildrenBuilder.renderAnimatedPage() {
        h1 {
            style = jso {
                color = "rgb(6, 7, 89)".unsafeCast<Color>()
            }
            +"Statistics for your tool"
        }
        img {
            @Suppress("MAGIC_NUMBER")

            style = jso {
                width = 30.rem
            }
            src = "/img/stat_view.png"
        }
    }
}

@Suppress("CUSTOM_GETTERS_SETTERS")
object GeneralInfoFourthPicture : WelcomePager {
    override val animation: Animation
        get() = fadeUpBottomRight

    override fun renderPage(childrenBuilder: ChildrenBuilder) {
        childrenBuilder.renderAnimatedPage()
    }

    private fun ChildrenBuilder.renderAnimatedPage() {
        h1 {
            style = jso {
                color = "rgb(6, 7, 89)".unsafeCast<Color>()
            }
            +"Build a team"
        }
        img {
            @Suppress("MAGIC_NUMBER")
            style = jso {
                width = 30.rem
            }
            src = "/img/organization_view.png"
        }
    }
}
