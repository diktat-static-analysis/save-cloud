package com.saveourtool.common.cvsscalculator

import kotlinx.serialization.Serializable

/**
 * Version of CVSS
 *
 * @property value abbreviated value from the cvss version
 */
@Serializable
enum class CvssVersion(val value: String) {
    CVSS_V2("2.0"),
    CVSS_V3("3.0"),
    CVSS_V3_1("3.1"),
    ;
}
