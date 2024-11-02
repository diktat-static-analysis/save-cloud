/**
 * View with demo
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.utils.Languages
import com.saveourtool.frontend.common.components.basic.cardComponent
import com.saveourtool.frontend.common.utils.Style
import com.saveourtool.frontend.common.utils.selectorBuilder
import com.saveourtool.frontend.common.utils.useBackground
import com.saveourtool.save.frontend.components.basic.demo.run.demoRunComponent
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.utils.*

import react.*
import react.dom.html.ReactHTML.div
import web.cssom.*

private val backgroundCard = cardComponent(hasBg = true, isPaddingBottomNull = true)

/**
 * Demo View
 */
val demoView: FC<DemoViewProps> = FC { props ->
    useBackground(Style.SAVE_LIGHT)

    val (selectedTheme, setSelectedTheme) = useState(AceThemes.preferredTheme)
    div {
        className = ClassName("d-flex justify-content-center mb-2")
        div {
            className = ClassName("col-8")
            backgroundCard {
                div {
                    className = ClassName("d-flex justify-content-center")
                    div {
                        className = ClassName("mt-2 col-2")
                        selectorBuilder(
                            selectedTheme.name,
                            AceThemes.values().map { it.name },
                            "custom-select",
                        ) { setSelectedTheme(AceThemes.valueOf(it.target.value)) }
                    }
                }
                demoRunComponent {
                    // todo: add editor mode selector
                    this.selectedMode = Languages.KOTLIN
                    this.selectedTheme = selectedTheme
                    this.projectCoordinates = props.projectCoordinates
                }
            }
        }
    }
}

/**
 * [Props] for [demoView]
 */
external interface DemoViewProps : Props {
    /**
     * saveourtool [ProjectCoordinates]
     */
    var projectCoordinates: ProjectCoordinates
}
