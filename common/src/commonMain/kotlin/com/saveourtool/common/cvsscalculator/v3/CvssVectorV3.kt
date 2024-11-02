package com.saveourtool.common.cvsscalculator.v3

import com.saveourtool.common.cvsscalculator.CvssVectorAbstract
import com.saveourtool.common.cvsscalculator.CvssVersion
import com.saveourtool.common.cvsscalculator.findOrElseThrow
import com.saveourtool.common.cvsscalculator.v2.BaseMetricsV2Names

import kotlin.math.min
import kotlin.math.pow
import kotlinx.serialization.Serializable

/**
 * @property version version of cvss
 * @property baseMetrics base metrics
 */
@Serializable
data class CvssVectorV3(
    override val version: CvssVersion,
    override val baseMetrics: BaseMetricsV3,
) : CvssVectorAbstract() {
    constructor(map: Map<String, String>) : this(
        version = map.findOrElseThrow(BaseMetricsV3Names.CVSS_VERSION.value, CvssVersion::value),
        baseMetrics = BaseMetricsV3(
            attackVector = map.findOrElseThrow(BaseMetricsV3Names.ATTACK_VECTOR.value, AttackVectorType::value),
            attackComplexity = map.findOrElseThrow(BaseMetricsV3Names.ATTACK_COMPLEXITY.value, AttackComplexityType::value),
            privilegeRequired = map.findOrElseThrow(BaseMetricsV3Names.PRIVILEGES_REQUIRED.value, PrivilegesRequiredType::value),
            userInteraction = map.findOrElseThrow(BaseMetricsV3Names.USER_INTERACTION.value, UserInteractionType::value),
            scopeMetric = map.findOrElseThrow(BaseMetricsV3Names.SCOPE.value, ScopeType::value),
            confidentiality = map.findOrElseThrow(BaseMetricsV3Names.CONFIDENTIALITY.value, CiaType::value),
            integrity = map.findOrElseThrow(BaseMetricsV3Names.INTEGRITY.value, CiaType::value),
            availability = map.findOrElseThrow(BaseMetricsV3Names.AVAILABILITY.value, CiaType::value),
        )
    )

    /**
     * @return calculate value
     */
    fun calculateValidBaseScore(): Float? = if (baseMetrics.isValid()) {
        calculateBaseScore()
    } else {
        null
    }

    @Suppress(
        "FLOAT_IN_ACCURATE_CALCULATIONS",
        "MagicNumber",
    )
    override fun calculateBaseScore(): Float {
        val iss = 1f - (1f - cia.getWeight(baseMetrics.confidentiality.value)) * (1f - cia.getWeight(baseMetrics.integrity.value)) * (1f -
                cia.getWeight(baseMetrics.availability.value))
        val impact: Float = if (baseMetrics.scopeMetric == ScopeType.CHANGED) {
            7.52f * (iss - 0.029f) - 3.25f * (iss - 0.02f).pow(15f)
        } else {
            6.42f * iss
        }
        val pr = scope.getOrElse(baseMetrics.scopeMetric.value) { throw IllegalArgumentException("No such weights for Scope type ${baseMetrics.scopeMetric.value}.") }
        val exploitability = 8.22f * av.getWeight(baseMetrics.attackVector.value) * ac.getWeight(baseMetrics.attackComplexity.value) *
                pr.getWeight(baseMetrics.privilegeRequired.value) *
                ui.getWeight(baseMetrics.userInteraction.value)

        val baseScore: Float = if (impact <= 0) {
            0f
        } else {
            if (baseMetrics.scopeMetric == ScopeType.UNCHANGED) {
                min(impact + exploitability, 10f)
            } else {
                min((impact + exploitability) * 1.08f, 10f)
            }
        }

        return roundup(baseScore)
    }

    /**
     * @return severity score vector
     */
    override fun scoreVectorString() =
            "${BaseMetricsV2Names.CVSS_VERSION.value}:${version.value}/${baseMetrics.scoreVectorString()}"

    companion object {
        val empty = CvssVectorV3(
            version = CvssVersion.CVSS_V3_1,
            baseMetrics = BaseMetricsV3.empty,
        )
    }
}
