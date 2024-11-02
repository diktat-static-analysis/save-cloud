package com.saveourtool.common.entities.contest

import kotlinx.serialization.Serializable

/**
 * Enum of contest sample field type
 * @property value pretty name
 */
@Serializable
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class ContestSampleFieldType(val value: String) {
    /**
     * Number type
     */
    NUMBER("Number"),

    /**
     * Single string type
     */
    STRING("String"),

    /**
     * Multi string type
     */
    TEXT("Text"),

    /**
     * Boolean type
     */
    BOOLEAN("Boolean"),
    ;

    override fun toString(): String = value
}
