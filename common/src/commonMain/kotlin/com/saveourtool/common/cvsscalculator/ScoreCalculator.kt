@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.common.cvsscalculator

import com.saveourtool.common.cvsscalculator.v2.*
import com.saveourtool.common.cvsscalculator.v3.*

const val CVSS_VERSION = "CVSS"

/**
 * @return map of vector values
 */
@Suppress("MagicNumber")
fun String.parsingVectorToMap() = split("/").associate { value ->
    value.split(":")
        .also { require(it.size == 2) }
        .let { it[0] to it[1] }
}

/**
 * @param vector severity score vector
 * @param getValue
 * @return value
 */
inline fun <reified T : Enum<T>> Map<String, String>.findOrElseThrow(vector: String, getValue: (T) -> String): T =
        get(vector)?.let { value -> enumValues<T>().find { getValue(it) == value } }
            ?: throw IllegalArgumentException("No such value for $vector.")

/**
 * @param vector
 * @return base score criticality
 */
fun calculateBaseScore(vector: String): Float {
    val map = vector.parsingVectorToMap()

    return when (map.findOrElseThrow(CVSS_VERSION, CvssVersion::value)) {
        CvssVersion.CVSS_V2 -> CvssVectorV2(map).calculateBaseScore()
        CvssVersion.CVSS_V3, CvssVersion.CVSS_V3_1 -> CvssVectorV3(map).calculateBaseScore()
    }
}
