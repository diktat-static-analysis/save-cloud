package com.saveourtool.common.entities

import kotlinx.serialization.Serializable

/**
 * Enum of project status
 *
 * The order of the elements is used for sorting
 */
@Serializable
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class ProjectStatus {
    /**
     * Project created
     */
    CREATED,

    /**
     * Project deleted
     */
    DELETED,

    /**
     * Project deleted
     */
    BANNED,
    ;
}
