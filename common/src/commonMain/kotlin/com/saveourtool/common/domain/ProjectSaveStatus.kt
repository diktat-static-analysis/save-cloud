package com.saveourtool.common.domain

import kotlinx.serialization.Serializable

/**
 * Enum of project save status
 * @property message
 */
@Serializable
@Suppress("CUSTOM_GETTERS_SETTERS")
enum class ProjectSaveStatus(val message: String) {
    /**
     * Project exist
     */
    EXIST("Project already exists"),

    /**
     * New project
     */
    NEW("Project saved successfully"),
    ;
}
