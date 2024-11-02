package com.saveourtool.common.cvsscalculator.v2

import com.saveourtool.common.cvsscalculator.CvssVectorAbstract
import com.saveourtool.common.cvsscalculator.CvssVersion
import com.saveourtool.common.cvsscalculator.findOrElseThrow
import com.saveourtool.common.cvsscalculator.v3.*
import kotlinx.serialization.Serializable

/**
 * @property version version of cvss
 * @property baseMetrics base metrics
 */
@Serializable
data class CvssVectorV2(
    override val version: CvssVersion,
    override val baseMetrics: BaseMetricsV2,
) : CvssVectorAbstract() {
    constructor(map: Map<String, String>) : this(
        version = map.findOrElseThrow(BaseMetricsV3Names.CVSS_VERSION.value, CvssVersion::value),
        baseMetrics = BaseMetricsV2(
            accessVector = map.findOrElseThrow(BaseMetricsV2Names.ACCESS_VECTOR.value, AccessVectorType::value),
            accessComplexity = map.findOrElseThrow(BaseMetricsV2Names.ACCESS_COMPLEXITY.value, AccessComplexityType::value),
            authentication = map.findOrElseThrow(BaseMetricsV2Names.AUTHENTICATION.value, AuthenticationType::value),
            confidentiality = map.findOrElseThrow(BaseMetricsV2Names.CONFIDENTIALITY.value, CiaTypeV2::value),
            integrity = map.findOrElseThrow(BaseMetricsV2Names.INTEGRITY.value, CiaTypeV2::value),
            availability = map.findOrElseThrow(BaseMetricsV2Names.AVAILABILITY.value, CiaTypeV2::value),
        )
    )

    @Suppress(
        "FLOAT_IN_ACCURATE_CALCULATIONS",
        "MagicNumber",
        "VARIABLE_HAS_PREFIX",
    )
    override fun calculateBaseScore(): Float {
        val impact = 10.41f * (1f - (1f - ciaImpact.getWeight(baseMetrics.confidentiality.value)) * (1f - ciaImpact.getWeight(baseMetrics.integrity.value)) * (1f -
                ciaImpact.getWeight(baseMetrics.availability.value)))

        val exploitability = 20f * accessV.getWeight(baseMetrics.accessVector.value) * accessC.getWeight(baseMetrics.accessComplexity.value) *
                auth.getWeight(baseMetrics.authentication.value)

        val fImpact = if (impact == 0f) 0f else 1.176f

        val baseScore: Float = ((0.6f * impact) + (0.4f * exploitability) - 1.5f) * fImpact

        return roundup(baseScore)
    }

    /**
     * @return severity score vector
     */
    override fun scoreVectorString() =
            "${BaseMetricsV2Names.CVSS_VERSION.value}:${version.value}/${baseMetrics.scoreVectorString()}"

    companion object {
        val empty = CvssVectorV2(
            version = CvssVersion.CVSS_V2,
            baseMetrics = BaseMetricsV2.empty,
        )
    }
}
