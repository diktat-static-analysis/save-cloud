@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.frontend.common.components.mobile

import com.saveourtool.frontend.common.components.views.welcome.chevron
import com.saveourtool.frontend.common.components.views.welcome.pagers.allSaveWelcomePagers
import com.saveourtool.frontend.common.components.views.welcome.pagers.save.renderReadMorePage
import com.saveourtool.frontend.common.components.views.welcome.saveWelcomeMarketingTitle
import com.saveourtool.frontend.common.externals.animations.animator
import com.saveourtool.frontend.common.externals.animations.scrollContainer
import com.saveourtool.frontend.common.externals.animations.scrollPage
import com.saveourtool.frontend.common.utils.SAVE_LIGHT_GRADIENT
import com.saveourtool.frontend.common.utils.particles

import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import web.cssom.*

/**
 * As a temp stub it was decided to make several views to make SAVE looking nice on mobile devices
 */
val saveWelcomeMobileView: FC<Props> = FC {
    div {
        style = jso {
            background = SAVE_LIGHT_GRADIENT.unsafeCast<Background>()
        }
        particles()
        sorryYourScreenIsTooSmall()
    }
}

private fun ChildrenBuilder.sorryYourScreenIsTooSmall() {
    notSupportedMobileYet()
    title()
    chevron("rgb(6, 7, 89)")

    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
    scrollContainer {
        scrollPage {}
        allSaveWelcomePagers.forEach { pager ->
            scrollPage { }
            pager.forEach {
                scrollPage {
                    div {
                        animator {
                            animation = it.animation
                            it.renderPage(this)
                        }
                    }
                }
            }
        }
    }

    renderReadMorePage()
}

private fun ChildrenBuilder.notSupportedMobileYet() {
    div {
        className = ClassName("row d-flex justify-content-center mx-auto text-danger mt-2")
        h4 {
            className = ClassName("text-center")
            +"We do not "
            b {
                +"yet "
            }
            +"support devices with small screen size. But you can get familiar with SAVE using the information below."
        }
    }
    div {
        className = ClassName("row d-flex justify-content-center mx-auto")
        img {
            src = "/img/sad_cat.png"
            @Suppress("MAGIC_NUMBER")
            style = jso {
                width = 5.rem
            }
        }
    }
}

private fun ChildrenBuilder.title() {
    div {
        className = ClassName("row justify-content-center mx-auto")
        // Marketing information
        saveWelcomeMarketingTitle("text-primary", true)
    }
}
