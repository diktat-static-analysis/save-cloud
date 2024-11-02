@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.common.cvsscalculator

/**
 * Base interface for all BaseMetrics classes
 */
@Suppress("CLASS_NAME_INCORRECT")
interface ICvssMetrics {
    /**
     * @return true if BaseMetrics is valid, false otherwise
     */
    fun isValid(): Boolean

    /**
     * @return severity score vector
     */
    fun scoreVectorString(): String
}
