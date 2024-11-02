@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.common.cvsscalculator.v3

import com.saveourtool.common.cvsscalculator.*
import kotlinx.serialization.Serializable

/**
 * @property attackVector
 * @property attackComplexity
 * @property privilegeRequired
 * @property userInteraction
 * @property scopeMetric
 * @property confidentiality
 * @property integrity
 * @property availability
 */
@Serializable
data class BaseMetricsV3(
    val attackVector: AttackVectorType,
    val attackComplexity: AttackComplexityType,
    val privilegeRequired: PrivilegesRequiredType,
    val userInteraction: UserInteractionType,
    val scopeMetric: ScopeType,
    val confidentiality: CiaType,
    val integrity: CiaType,
    val availability: CiaType,
) : ICvssMetrics {
    /**
     * @return true if BaseMetrics is valid, false otherwise
     */
    override fun isValid(): Boolean = attackVector != AttackVectorType.NOT_DEFINED && attackComplexity != AttackComplexityType.NOT_DEFINED &&
            privilegeRequired != PrivilegesRequiredType.NOT_DEFINED && userInteraction != UserInteractionType.NOT_DEFINED &&
            scopeMetric != ScopeType.NOT_DEFINED && confidentiality != CiaType.NOT_DEFINED &&
            integrity != CiaType.NOT_DEFINED && availability != CiaType.NOT_DEFINED

    /**
     * @return severity score vector
     */
    override fun scoreVectorString() =
            "${BaseMetricsV3Names.ATTACK_VECTOR.value}:${attackVector.value}/" +
                    "${BaseMetricsV3Names.ATTACK_COMPLEXITY.value}:${attackComplexity.value}/${BaseMetricsV3Names.PRIVILEGES_REQUIRED.value}:" +
                    "${privilegeRequired.value}/${BaseMetricsV3Names.USER_INTERACTION.value}:${userInteraction.value}/${BaseMetricsV3Names.SCOPE.value}:" +
                    "${scopeMetric.value}/${BaseMetricsV3Names.CONFIDENTIALITY.value}:${confidentiality.value}/${BaseMetricsV3Names.INTEGRITY.value}:" +
                    "${integrity.value}/${BaseMetricsV3Names.AVAILABILITY.value}:${availability.value}"

    companion object {
        val empty = BaseMetricsV3(
            attackVector = AttackVectorType.NOT_DEFINED,
            attackComplexity = AttackComplexityType.NOT_DEFINED,
            privilegeRequired = PrivilegesRequiredType.NOT_DEFINED,
            userInteraction = UserInteractionType.NOT_DEFINED,
            scopeMetric = ScopeType.NOT_DEFINED,
            confidentiality = CiaType.NOT_DEFINED,
            integrity = CiaType.NOT_DEFINED,
            availability = CiaType.NOT_DEFINED,
        )
    }
}

/**
 * Names of base metrics
 *
 * @property value abbreviated value
 */
@Serializable
enum class BaseMetricsV3Names(val value: String) {
    ATTACK_COMPLEXITY("AC"),
    ATTACK_VECTOR("AV"),
    AVAILABILITY("A"),
    CONFIDENTIALITY("C"),
    CVSS_VERSION("CVSS"),
    INTEGRITY("I"),
    PRIVILEGES_REQUIRED("PR"),
    SCOPE("S"),
    USER_INTERACTION("UI"),
    ;
}
