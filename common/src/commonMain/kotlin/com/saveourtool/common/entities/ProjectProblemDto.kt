package com.saveourtool.common.entities

import kotlinx.serialization.Serializable

/**
 * @property name
 * @property description
 * @property critical
 * @property identifier
 * @property organizationName
 * @property projectName
 * @property id
 * @property isClosed
 */
@Serializable
data class ProjectProblemDto(
    val name: String,
    val description: String,
    val critical: ProjectProblemCritical,
    val identifier: String?,
    val organizationName: String,
    val projectName: String,
    val isClosed: Boolean,
    val id: Long? = null,
) {
    companion object {
        val empty = ProjectProblemDto(
            "",
            "",
            ProjectProblemCritical.LOW,
            null,
            "",
            "",
            false,
        )
    }
}
