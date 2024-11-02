package com.saveourtool.common.filters

import com.saveourtool.common.entities.ProjectStatus
import kotlinx.serialization.Serializable

/**
 * @property name substring that match the beginning of a name
 * @property statuses current [statuses] of an organization
 * @property organizationName
 * @property public
 */
@Serializable
data class ProjectFilter(
    val name: String,
    val organizationName: String = "",
    val statuses: Set<ProjectStatus> = setOf(ProjectStatus.CREATED),
    val public: Boolean? = null,
) {
    companion object {
        val created = ProjectFilter(name = "")

        /**
         * The filter which returns projects with any status, not just
         * [ProjectStatus.CREATED].
         */
        val any = ProjectFilter(
            name = "",
            statuses = enumValues<ProjectStatus>().toSet(),
        )
    }
}
