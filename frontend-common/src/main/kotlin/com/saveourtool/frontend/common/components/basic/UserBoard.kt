/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.frontend.common.components.basic

import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.validation.FrontendCosvRoutes

import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.figure
import web.cssom.ClassName
import web.cssom.Length
import web.cssom.rem

/**
 * A functional component to display users' avatars.
 */
@Suppress("MAGIC_NUMBER")
val userBoard: FC<UserBoardProps> = FC { props ->
    div {
        className = ClassName("latest-photos")
        div {
            className = ClassName("row")
            props.users.forEach { user ->
                div {
                    className = ClassName(props.avatarOuterClasses.orEmpty())
                    figure {
                        renderAvatar(user, props.avatarInnerClasses.orEmpty(), "/${FrontendCosvRoutes.PROFILE}/${user.name}") {
                            // just some default values in case you don't want to provide value
                            // in this case you will get small avatar
                            width = props.widthAndHeight ?: 4.rem
                            height = props.widthAndHeight ?: 4.rem
                        }
                    }
                }
            }
        }
    }
}

/**
 * [Props] for [userBoard] component
 */
external interface UserBoardProps : Props {
    /**
     * list of users that should be displayed
     */
    var users: List<UserInfo>

    /**
     * Classes that are applied to [div] that contains img tag
     */
    var avatarOuterClasses: String?

    /**
     * Classes that are applied to img tag
     */
    var avatarInnerClasses: String?

    /**
     * Size of avatar or any other properties
     */
    var widthAndHeight: Length?
}
