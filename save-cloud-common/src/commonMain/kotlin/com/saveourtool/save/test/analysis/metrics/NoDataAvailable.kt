package com.saveourtool.save.test.analysis.metrics

import kotlin.jvm.JvmStatic
import kotlinx.serialization.Serializable

/**
 * This is a `class` (and not an `object`) due to limitations of _JS Legacy_.
 */
@Serializable
class NoDataAvailable private constructor() : TestMetrics() {
    override fun equals(other: Any?): Boolean =
            other is NoDataAvailable

    override fun hashCode(): Int =
            this::class.hashCode()

    override fun toString(): String =
            this::class.simpleName ?: super.toString()

    companion object {
        @JvmStatic
        val instance = NoDataAvailable()
    }
}
