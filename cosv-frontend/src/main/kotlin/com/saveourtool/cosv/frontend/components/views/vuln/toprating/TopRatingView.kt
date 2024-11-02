/**
 * View for TopRating
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.cosv.frontend.components.views.vuln.toprating

import com.saveourtool.common.frontend.TabMenuBar
import com.saveourtool.common.validation.FrontendCosvRoutes
import com.saveourtool.frontend.common.externals.fontawesome.faTrophy
import com.saveourtool.frontend.common.utils.Style
import com.saveourtool.frontend.common.utils.tab
import com.saveourtool.frontend.common.utils.title
import com.saveourtool.frontend.common.utils.useBackground

import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.useState
import web.cssom.ClassName

val topRatingView: FC<Props> = FC {
    useBackground(Style.SAVE_LIGHT)

    val (selectedMenu, setSelectedMenu) = useState(TopRatingTab.USERS)

    div {
        className = ClassName("d-sm-flex align-items-center justify-content-center mb-4")
        h1 {
            className = ClassName("h3 mb-0 text-gray-800")
            title(" Top Vuln Reporters", faTrophy)
        }
    }

    tab(selectedMenu.name, TopRatingTab.values().map { it.name }, "nav nav-tabs mt-3") { value ->
        setSelectedMenu(TopRatingTab.valueOf(value))
    }

    when (selectedMenu) {
        TopRatingTab.USERS -> userRatingTable()
        TopRatingTab.ORGANIZATIONS -> organizationRatingTab()
    }
}

/**
 * Enum that contains values for the tab that is used in top rating view
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class TopRatingTab {
    USERS,
    ORGANIZATIONS,
    ;

    companion object : TabMenuBar<TopRatingTab> {
        override val nameOfTheHeadUrlSection = ""
        override val defaultTab: TopRatingTab = USERS
        override val regexForUrlClassification = "/${FrontendCosvRoutes.VULN_TOP_RATING}"
        override fun valueOf(elem: String): TopRatingTab = TopRatingTab.valueOf(elem)
        override fun values(): Array<TopRatingTab> = TopRatingTab.values()
    }
}
