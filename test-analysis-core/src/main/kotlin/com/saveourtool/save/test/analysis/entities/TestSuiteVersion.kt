package com.saveourtool.save.test.analysis.entities

import com.saveourtool.common.entities.Execution

/**
 * Test suite version, intended to be assignment-incompatible with the regular
 * string.
 *
 * @property value the underlying string value.
 */
@JvmInline
value class TestSuiteVersion(val value: String) {
    override fun toString(): String =
            value
}

/**
 * @return the test suite version of this execution, or `null`.
 */
fun Execution.version(): TestSuiteVersion? =
        version?.let(::TestSuiteVersion)
