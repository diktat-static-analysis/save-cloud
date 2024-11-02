@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.common.cvsscalculator.v2

import kotlinx.serialization.Serializable

/**
 * Type of Access Vector
 *
 * @property value abbreviated value from the cvss vector
 */
@Serializable
enum class AccessVectorType(val value: String) {
    ADJACENT_NETWORK("A"),
    LOCAL("L"),
    NETWORK("N"),
    NOT_DEFINED("_"),
    ;
}

/**
 * Type of Access Complexity
 *
 * @property value abbreviated value from the cvss vector
 */
@Serializable
enum class AccessComplexityType(val value: String) {
    HIGH("H"),
    LOW("L"),
    MEDIUM("M"),
    NOT_DEFINED("_"),
    ;
}

/**
 * Type of Authentication
 *
 * @property value abbreviated value from the cvss vector
 */
@Serializable
enum class AuthenticationType(val value: String) {
    MULTIPLE("M"),
    NONE("N"),
    NOT_DEFINED("_"),
    SINGLE("S"),
    ;
}

/**
 * Type of CIA
 *
 * @property value abbreviated value from the cvss vector
 */
@Serializable
enum class CiaTypeV2(val value: String) {
    COMPLETE("C"),
    NONE("N"),
    NOT_DEFINED("_"),
    PARTIAL("P"),
    ;
}
