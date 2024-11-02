package com.saveourtool.common.filters

import kotlinx.serialization.Serializable

/**
 * @property organizationName name of organization
 * @property projectName name of project
 * @property isClosed flag is project problem closed or not
 */
@Serializable
data class ProjectProblemFilter(
    val organizationName: String,
    val projectName: String,
    val isClosed: Boolean = false,
) {
    companion object {
        val stub = ProjectProblemFilter(
            organizationName = "",
            projectName = "",
        )
    }
}
