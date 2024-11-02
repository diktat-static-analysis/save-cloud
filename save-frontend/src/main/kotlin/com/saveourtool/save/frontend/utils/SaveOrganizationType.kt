package com.saveourtool.save.frontend.utils

import com.saveourtool.frontend.common.components.views.organization.OrganizationMenuBar
import com.saveourtool.frontend.common.components.views.organization.OrganizationType

object SaveOrganizationType : OrganizationType {
    override val listTab: Array<OrganizationMenuBar> = arrayOf(
        OrganizationMenuBar.INFO,
        OrganizationMenuBar.TOOLS,
        OrganizationMenuBar.BENCHMARKS,
        OrganizationMenuBar.CONTESTS,
        OrganizationMenuBar.SETTINGS,
        OrganizationMenuBar.ADMIN,
    )
}
