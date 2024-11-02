package com.saveourtool.common.test

/**
 * The validation result &mdash; either a progress message (intermediate or
 * terminal) or an error message (terminal).
 */
sealed class TestSuiteValidationResult {
    /**
     * The unique check id.
     */
    abstract val checkId: String

    /**
     * The human-readable check name.
     */
    abstract val checkName: String
}
