package com.saveourtool.common.test

import kotlinx.serialization.Serializable

/**
 * @property checkId the unique check id.
 * @property checkName the human-readable check name.
 * @property message the error message (w/o the trailing dot).
 */
@Serializable
data class TestSuiteValidationError(
    override val checkId: String,
    override val checkName: String,
    val message: String,
) : TestSuiteValidationResult() {
    override fun toString(): String =
            "$checkName: $message."
}
