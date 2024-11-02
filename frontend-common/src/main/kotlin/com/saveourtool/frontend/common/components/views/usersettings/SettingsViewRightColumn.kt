/**
 * In settings view we have two columns: this one is the right one
 */

package com.saveourtool.frontend.common.components.views.usersettings

import com.saveourtool.common.validation.FrontendRoutes.*
import com.saveourtool.frontend.common.components.views.usersettings.right.deleteSettingsCard
import com.saveourtool.frontend.common.components.views.usersettings.right.emailSettingsCard
import com.saveourtool.frontend.common.components.views.usersettings.right.organizationsSettingsCard
import com.saveourtool.frontend.common.components.views.usersettings.right.profile.profileSettingsCard
import com.saveourtool.frontend.common.components.views.usersettings.right.tokenSettingsCard

import react.FC
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

val rightSettingsColumn: FC<SettingsProps> = FC { props ->
    div {
        className = ClassName("card card-body mt-0 pt-0 px-0 text-gray-800 shadow")
        style = cardHeight
        when (props.type) {
            SETTINGS_PROFILE -> profileSettingsCard {
                this.userInfo = props.userInfo
                this.type = props.type
                this.userInfoSetter = props.userInfoSetter
            }
            SETTINGS_EMAIL -> emailSettingsCard {
                this.userInfo = props.userInfo
                this.type = props.type
                this.userInfoSetter = props.userInfoSetter
            }
            SETTINGS_TOKEN -> tokenSettingsCard {
                this.userInfo = props.userInfo
                this.type = props.type
            }
            SETTINGS_ORGANIZATIONS -> organizationsSettingsCard {
                this.userInfo = props.userInfo
                this.type = props.type
            }
            SETTINGS_DELETE -> deleteSettingsCard {
                this.userInfo = props.userInfo
                this.type = props.type
            }
            else -> {
                // FixMe: finish stub here
            }
        }
    }
}
