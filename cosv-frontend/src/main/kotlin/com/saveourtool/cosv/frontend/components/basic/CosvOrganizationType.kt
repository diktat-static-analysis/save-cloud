package com.saveourtool.cosv.frontend.components.basic

import com.saveourtool.frontend.common.components.views.organization.OrganizationMenuBar
import com.saveourtool.frontend.common.components.views.organization.OrganizationType

object CosvOrganizationType : OrganizationType {
    override val listTab: Array<OrganizationMenuBar> = arrayOf(
        OrganizationMenuBar.INFO,
        OrganizationMenuBar.VULNERABILITIES,
        OrganizationMenuBar.SETTINGS,
        OrganizationMenuBar.ADMIN,
    )
}
