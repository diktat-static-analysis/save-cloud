package com.saveourtool.common.test

import kotlinx.serialization.Serializable

/**
 * @property checkId the unique check id.
 * @property checkName the human-readable check name.
 * @property percentage the completion percentage (`0..100`).
 */
@Serializable
data class TestSuiteValidationProgress(
    override val checkId: String,
    override val checkName: String,
    val percentage: Int
) : TestSuiteValidationResult() {
    init {
        @Suppress("MAGIC_NUMBER")
        require(percentage in 0..100) {
            "Percentage should be in range of [0..100]: $percentage"
        }
    }

    override fun toString(): String =
            when (percentage) {
                100 -> "Check $checkName is complete."
                else -> "Check $checkName is running, $percentage% complete\u2026"
            }
}
