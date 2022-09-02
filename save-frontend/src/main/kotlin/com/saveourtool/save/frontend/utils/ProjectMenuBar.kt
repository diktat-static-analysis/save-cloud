package com.saveourtool.save.frontend.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.TabMenuBar

/**
 * A value for project menu.
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class ProjectMenuBar {
    INFO,
    RUN,
    STATISTICS,
    SETTINGS,
    ;

    companion object : TabMenuBar<ProjectMenuBar> {
        // The string is the postfix of a [regexForUrlClassification] for parsing the url
        private val postfixInRegex = values().map { it.name.lowercase() }.joinToString { "|" }
        override val defaultTab: ProjectMenuBar = INFO
        override val regexForUrlClassification = Regex("/project/[^/]+/[^/]+/($postfixInRegex)")
        override var pathDefaultTab: String
            get() = TODO("Not yet implemented")
            set(value) {}

        override var longPrefixPathAllTab: String
            get() = TODO("Not yet implemented")
            set(value) {}
        override fun valueOf(elem: String): ProjectMenuBar = ProjectMenuBar.valueOf(elem)
        override fun values(): Array<ProjectMenuBar> = ProjectMenuBar.values()
        override fun isNotAvailableWithThisRole(role: Role, elem: ProjectMenuBar?, isOrganizationCanCreateContest: Boolean?): Boolean = ((elem == SETTINGS) || (elem == RUN)) && role.isLowerThan(Role.ADMIN)
    }
}
