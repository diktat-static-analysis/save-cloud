@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
)

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.domain.Role
import com.saveourtool.common.info.UserInfo
import com.saveourtool.frontend.common.components.views.vuln.vulnerabilityTableComponent

import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

internal val renderVulnerabilitiesTab: FC<RenderVulnerabilitiesTabProps> = FC { props ->

    div {
        className = ClassName("col-7 mx-auto mb-4")

        vulnerabilityTableComponent {
            this.currentUserInfo = props.currentUserInfo
            this.organizationName = props.organizationName
            this.isCurrentUserIsAdminInOrganization = props.selfRole.isHigherOrEqualThan(Role.ADMIN)
        }
    }
}

/**
 * RenderVulnerabilitiesTab component props
 */
external interface RenderVulnerabilitiesTabProps : Props {
    /**
     * Current logged-in user
     */
    var currentUserInfo: UserInfo?

    /**
     * [Role] of user that is observing this component
     */
    var selfRole: Role

    /**
     * Name of organization
     */
    var organizationName: String
}
