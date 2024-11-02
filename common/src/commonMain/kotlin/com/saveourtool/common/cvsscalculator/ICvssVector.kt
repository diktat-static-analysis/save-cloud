@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.common.cvsscalculator

/**
 * Base interface for CvssVector classes
 */
@Suppress("CLASS_NAME_INCORRECT")
interface ICvssVector {
    /**
     * Version of CVSS
     **/
    val version: CvssVersion

    /**
     * Base metrics of CVSS
     **/
    val baseMetrics: ICvssMetrics

    /**
     * @return base score for cvss
     */
    fun calculateBaseScore(): Float

    /**
     * @return severity score vector
     */
    fun scoreVectorString(): String
}
