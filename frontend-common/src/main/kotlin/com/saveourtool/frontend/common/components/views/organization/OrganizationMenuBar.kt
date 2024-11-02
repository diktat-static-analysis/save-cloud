package com.saveourtool.frontend.common.components.views.organization

/**
 * A value for organization menu.
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class OrganizationMenuBar(private val title: String? = null) {
    INFO,
    VULNERABILITIES,
    TOOLS,
    BENCHMARKS,
    CONTESTS,
    SETTINGS,
    ADMIN,
    ;

    /**
     * @return title or name if title is not specified
     */
    fun getTitle() = title ?: name

    companion object {
        val defaultTab: OrganizationMenuBar = INFO
    }
}
