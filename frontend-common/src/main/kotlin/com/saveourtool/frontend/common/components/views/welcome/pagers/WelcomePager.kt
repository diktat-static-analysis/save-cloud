package com.saveourtool.frontend.common.components.views.welcome.pagers

import com.saveourtool.frontend.common.components.views.welcome.pagers.save.*
import com.saveourtool.frontend.common.externals.animations.Animation
import react.ChildrenBuilder

val allSaveWelcomePagers = listOf(
    // listOf(HighLevelSave),
    listOf(SloganAboutCi),
    listOf(GeneralInfoFirstPicture, GeneralInfoSecondPicture, GeneralInfoThirdPicture, GeneralInfoFourthPicture),
    listOf(SloganAboutBenchmarks),
    listOf(AwesomeBenchmarks),
    listOf(SloganAboutTests),
    listOf(TestsSelector),
    listOf(SloganAboutContests),
    listOf(Contests)
)

val allVulnerabilityWelcomePagers: List<List<WelcomePager>> = emptyList()

/**
 * common interface for all pagers on welcome view
 */
interface WelcomePager {
    /**
     * animation for the pager
     */
    val animation: Animation

    /**
     * rendering function - place your html code here
     *
     * @param childrenBuilder
     */
    fun renderPage(childrenBuilder: ChildrenBuilder)
}
