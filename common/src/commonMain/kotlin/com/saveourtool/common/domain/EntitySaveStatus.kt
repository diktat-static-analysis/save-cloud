package com.saveourtool.common.domain

import kotlinx.serialization.Serializable

/**
 * A common enum of statuses for saving entity
 */
@Serializable
enum class EntitySaveStatus {
    /**
     * Conflict while saving entity
     */
    CONFLICT,

    /**
     * Entity exists already
     */
    EXIST,

    /**
     * New entity saved successfully
     */
    NEW,

    /**
     * Updated entity
     */
    UPDATED,
    ;
}
