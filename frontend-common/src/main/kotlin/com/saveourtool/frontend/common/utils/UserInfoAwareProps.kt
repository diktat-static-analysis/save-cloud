/**
 * Props for UserInfo
 */

package com.saveourtool.frontend.common.utils

import com.saveourtool.common.info.UserInfo
import react.FC
import react.Props
import react.PropsWithChildren
import react.StateSetter

/**
 * Property to propagate user info from App
 */
external interface UserInfoAwareProps : Props {
    /**
     * Currently logged-in user or null
     */
    var userInfo: UserInfo?
}

/**
 * Property to propagate user info from App with children
 */
external interface UserInfoAwarePropsWithChildren : UserInfoAwareProps, PropsWithChildren

/**
 * Property to propagate user info from App with ability to update it
 */
external interface UserInfoAwareMutableProps : UserInfoAwareProps {
    /**
     * Setter of user info (it can be updated in settings on several views)
     *
     * After updating user information we will update userSettings without re-rendering the page
     * PLEASE NOTE: THIS PROPERTY AFFECTS RENDERING OF WHOLE APP.KT
     * IF YOU HAVE SOME PROBLEMS WITH IT, CHECK THAT YOU HAVE PROPAGATED IT PROPERLY:
     * { this.userInfoSetter = (!) PROPS (!) .userInfoSetter }
     */
    var userInfoSetter: StateSetter<UserInfo?>
}

/**
 * Property to propagate user info from App with ability to update it with children
 */
external interface UserInfoAwareMutablePropsWithChildren : UserInfoAwareMutableProps, PropsWithChildren

/**
 * @return [FC] with [UserInfoAwareMutableProps] instead of [UserInfoAwareProps]
 */
internal fun FC<UserInfoAwareProps>.asMutable(): FC<UserInfoAwareMutableProps> = FC { props ->
    this@asMutable {
        this.userInfo = props.userInfo
    }
}
