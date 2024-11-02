package com.saveourtool.common.cvsscalculator

import kotlin.math.roundToInt

/**
 * Base class for all CvssVector classes
 */
abstract class CvssVectorAbstract : ICvssVector {
    /**
     * @param key of severity score vector
     * @return weight for vector value
     */
    protected fun Map<String, Float>.getWeight(key: String): Float = this.getOrElse(key) {
        throw IllegalArgumentException("No such weight for value $key.")
    }

    /**
     * @param number number to round
     * @return Rounds a value up to one decimal place
     */
    @Suppress(
        "FLOAT_IN_ACCURATE_CALCULATIONS",
        "MagicNumber",
    )
    // https://www.first.org/cvss/v3.1/specification-document#Appendix-A---Floating-Point-Rounding
    protected fun roundup(number: Float): Float {
        val value = (number * 100_000).roundToInt()
        return if (value % 10_000 == 0) {
            value / 100_000f
        } else {
            (value / 10_000 + 1) / 10f
        }
    }
}
