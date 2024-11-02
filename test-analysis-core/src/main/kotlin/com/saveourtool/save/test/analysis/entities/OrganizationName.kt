package com.saveourtool.save.test.analysis.entities

import com.saveourtool.common.entities.Organization

/**
 * Organization name, intended to be assignment-incompatible with the regular
 * string.
 *
 * @property value the underlying string value.
 */
@JvmInline
value class OrganizationName(val value: String) {
    override fun toString(): String =
            value
}

/**
 * @return the name of this organization.
 */
fun Organization.name(): OrganizationName =
        OrganizationName(name)
