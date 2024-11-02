package com.saveourtool.common.domain

import kotlinx.serialization.Serializable

/**
 * Enum of TestSuitesSource save status
 * @property message
 */
@Serializable
@Suppress("CUSTOM_GETTERS_SETTERS")
enum class SourceSaveStatus(val message: String) {
    /**
     * Conflict while saving source
     */
    CONFLICT("Test suite source with such test root path and git id is already present"),

    /**
     * Source exists
     */
    EXIST("Test suite source already exists"),

    /**
     * New source
     */
    NEW("Test suite source saved successfully"),

    /**
     * Updated entity
     */
    UPDATED("Test suite source updated successfully"),
    ;
}
