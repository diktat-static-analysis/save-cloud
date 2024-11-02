package com.saveourtool.save.test.analysis.entities

import com.saveourtool.common.entities.Execution

/**
 * Test suite source name, intended to be assignment-incompatible with the
 * regular string.
 *
 * @property value the underlying string value.
 */
@JvmInline
value class TestSuiteSourceName(val value: String) {
    override fun toString(): String =
            value
}

/**
 * @return the test suite source name of this execution, or `null`.
 */
fun Execution.testSuiteSourceName(): TestSuiteSourceName? =
        testSuiteSourceName?.let(::TestSuiteSourceName)
