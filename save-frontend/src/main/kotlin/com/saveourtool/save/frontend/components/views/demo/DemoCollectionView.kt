/**
 * Main view for Demos
 */

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.frontend.common.utils.Style
import com.saveourtool.frontend.common.utils.useBackground
import com.saveourtool.save.frontend.components.basic.demo.welcome.demoList
import com.saveourtool.save.frontend.components.basic.demo.welcome.featuredDemos
import com.saveourtool.save.frontend.components.basic.demo.welcome.introductionSection

import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import web.cssom.ClassName

val demoCollectionView: FC<Props> = FC {
    useBackground(Style.SAVE_DARK)

    main {
        className = ClassName("main-content mt-0 ps")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("d-flex justify-content-center")
                div {
                    className = ClassName("col-4 d-flex align-items-stretch")
                    div {
                        div {
                            className = ClassName("mb-2")
                            introductionSection()
                        }
                        div {
                            featuredDemos()
                        }
                    }
                }
                div {
                    className = ClassName("col-7 d-flex align-items-stretch")
                    demoList()
                }
            }
        }
    }
}
