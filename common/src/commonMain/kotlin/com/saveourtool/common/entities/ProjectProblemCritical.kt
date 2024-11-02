package com.saveourtool.common.entities

import kotlinx.serialization.Serializable

/**
 * Enum of project problem severity
 *
 * The order of the elements is used for sorting
 */
@Serializable
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class ProjectProblemCritical(private val severity: String) {
    /**
     * Low severity
     */
    LOW("Low"),

    /**
     * Middle severity
     */
    MIDDLE("Middle"),

    /**
     * High severity
     */
    HIGH("High"),

    /**
     * Critical severity
     */
    CRITICAL("Critical"),
    ;

    override fun toString(): String = severity
}
