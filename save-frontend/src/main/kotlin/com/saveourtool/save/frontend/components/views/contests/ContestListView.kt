/**
 * Contests "market" - showcase for users, where they can navigate and check contests
 */

@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.common.info.UserInfo
import com.saveourtool.frontend.common.utils.Style
import com.saveourtool.frontend.common.utils.useBackground

import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import web.cssom.ClassName

/**
 * A view with collection of contests
 */
val contestListView: FC<ContestListViewProps> = FC { props ->
    useBackground(Style.SAVE_DARK)
    main {
        className = ClassName("main-content mt-0 ps text-gray-800")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("row justify-content-center")
                div {
                    className = ClassName("col-9")

                    div {
                        className = ClassName("row mb-2")
                        welcomeToSaveContests()
                        newContests()
                        createNewContestTemplate()
                    }

                    div {
                        className = ClassName("row mb-2")
                        featuredContests()
                        statistics()
                    }

                    div {
                        className = ClassName("row mb-2 d-flex align-items-stretch")
                        globalRating()
                        contestList()
                        myProjectsRating {
                            currentUserInfo = props.currentUserInfo
                        }
                    }
                    div {
                        className = ClassName("row mb-2")
                        contestSampleList()
                    }
                }
            }
        }
    }
}

/**
 * TODO:
 * 2. Link to create contests
 * 3. Grand champions
 * 4. Countdown till the end of contests
 */

/**
 * [Props] for [contestListView]
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ContestListViewProps : Props {
    var currentUserInfo: UserInfo?
}
