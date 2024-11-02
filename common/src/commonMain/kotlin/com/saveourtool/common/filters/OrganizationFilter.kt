package com.saveourtool.common.filters

import com.saveourtool.common.entities.OrganizationStatus
import kotlinx.serialization.Serializable

/**
 * @property prefix substring that match the beginning of a name
 * @property statuses current [statuses] of an organization
 */
@Serializable
data class OrganizationFilter(
    val prefix: String,
    val statuses: Set<OrganizationStatus> = setOf(OrganizationStatus.CREATED),
) {
    companion object {
        val created = OrganizationFilter("")
        val all = OrganizationFilter("", OrganizationStatus.values().toSet())
    }
}
