package com.saveourtool.common.entities

import kotlinx.serialization.Serializable

/**
 * Enum of organization status
 *
 * The order of the elements is used for sorting
 */
@Serializable
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class OrganizationStatus {
    /**
     * Organization created
     */
    CREATED,

    /**
     * Organization deleted
     */
    DELETED,

    /**
     * Organization banned
     */
    BANNED,
    ;
}
