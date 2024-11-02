package com.saveourtool.common.domain

import com.saveourtool.common.validation.NAMING_MAX_LENGTH
import kotlinx.serialization.Serializable

/**
 * Enum of organization save status
 * @property message
 */
@Serializable
enum class OrganizationSaveStatus(val message: String) {
    /**
     * Organization has conflicting name
     */
    CONFLICT("This name is already taken"),

    /**
     * Organization name longer than [NAMING_MAX_LENGTH] characters
     */
    INVALID_NAME("Name must not be longer than $NAMING_MAX_LENGTH characters"),

    /**
     * New organization
     */
    NEW("Organization saved successfully"),
    ;
}
