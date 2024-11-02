/**
 * Main view for Demos
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.frontend.common.utils.Style
import com.saveourtool.frontend.common.utils.UserInfoAwareProps
import com.saveourtool.frontend.common.utils.particles
import com.saveourtool.frontend.common.utils.useBackground
import com.saveourtool.save.frontend.utils.*
import js.core.jso
import react.FC

import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import web.cssom.*

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
val indexView: FC<UserInfoAwareProps> = FC { props ->
    useBackground(Style.INDEX)
    particles()
    main {
        className = ClassName("main-content")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("row justify-content-center")
                div {
                    className = ClassName("col-8 card border-secondary lg-shadow shadow-box")
                    div {
                        className = ClassName("row")
                        div {
                            className = ClassName("col")
                            @Suppress("MAGIC_NUMBER")
                            style = jso {
                                background = "rgb(0,0,0) url(img/logo-bg-p-3.png)".unsafeCast<Background>()
                                backgroundRepeat = "no-repeat".unsafeCast<BackgroundRepeat>()
                                backgroundSize = "100% auto".unsafeCast<BackgroundSize>()
                                // need to hardcode the height, as it is very tightly linked to the size of the img
                                // and to logo alignments
                                height = 33.rem
                            }
                            logoButtons { }
                        }
                    }

                    div {
                        className = ClassName("row d-flex justify-content-center")
                        div {
                            className = ClassName("col min-vh-100")
                            style = jso {
                                background = INDEX_VIEW_CUSTOM_BG.unsafeCast<Background>()
                            }

                            props.userInfo
                                ?: run {
                                    separator { }
                                    indexAuth { }
                                }

                            indexViewInfo { userInfo = props.userInfo }
                        }
                    }
                }
            }
        }
    }

    div {
        className = ClassName("row text-center")
        div {
            className = ClassName("col-3 text-center")
        }
    }
}
