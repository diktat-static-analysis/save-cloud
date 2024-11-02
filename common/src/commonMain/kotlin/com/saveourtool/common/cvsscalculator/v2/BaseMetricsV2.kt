@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.common.cvsscalculator.v2

import com.saveourtool.common.cvsscalculator.*
import com.saveourtool.common.cvsscalculator.v3.*
import kotlinx.serialization.Serializable

/**
 * @property accessVector
 * @property accessComplexity
 * @property authentication
 * @property confidentiality
 * @property integrity
 * @property availability
 */
@Serializable
data class BaseMetricsV2(
    val accessVector: AccessVectorType,
    val accessComplexity: AccessComplexityType,
    val authentication: AuthenticationType,
    val confidentiality: CiaTypeV2,
    val integrity: CiaTypeV2,
    val availability: CiaTypeV2,
) : ICvssMetrics {
    /**
     * @return true if BaseMetricsV2 is valid, false otherwise
     */
    override fun isValid(): Boolean = accessVector != AccessVectorType.NOT_DEFINED && accessComplexity != AccessComplexityType.NOT_DEFINED &&
            authentication != AuthenticationType.NOT_DEFINED && confidentiality != CiaTypeV2.NOT_DEFINED &&
            integrity != CiaTypeV2.NOT_DEFINED && availability != CiaTypeV2.NOT_DEFINED

    /**
     * @return severity score vector
     */
    override fun scoreVectorString() =
            "${BaseMetricsV2Names.ACCESS_VECTOR.value}:${accessVector.value}/" +
                    "${BaseMetricsV2Names.ACCESS_COMPLEXITY.value}:${accessComplexity.value}/${BaseMetricsV2Names.AUTHENTICATION.value}:" +
                    "${authentication.value}/${BaseMetricsV2Names.CONFIDENTIALITY.value}:${confidentiality.value}/${BaseMetricsV2Names.INTEGRITY.value}:" +
                    "${integrity.value}/${BaseMetricsV2Names.AVAILABILITY.value}:${availability.value}"

    companion object {
        val empty = BaseMetricsV2(
            accessVector = AccessVectorType.NOT_DEFINED,
            accessComplexity = AccessComplexityType.NOT_DEFINED,
            authentication = AuthenticationType.NOT_DEFINED,
            confidentiality = CiaTypeV2.NOT_DEFINED,
            integrity = CiaTypeV2.NOT_DEFINED,
            availability = CiaTypeV2.NOT_DEFINED,
        )
    }
}

/**
 * Names of base metrics
 *
 * @property value abbreviated value
 */
@Serializable
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class BaseMetricsV2Names(val value: String) {
    CVSS_VERSION("CVSS"),
    ACCESS_VECTOR("AV"),
    ACCESS_COMPLEXITY("AC"),
    AUTHENTICATION("Au"),
    CONFIDENTIALITY("C"),
    INTEGRITY("I"),
    AVAILABILITY("A"),
    ;
}
