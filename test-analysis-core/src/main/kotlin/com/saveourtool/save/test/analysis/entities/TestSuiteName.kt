package com.saveourtool.save.test.analysis.entities

import com.saveourtool.common.entities.TestSuite

/**
 * Test suite name, intended to be assignment-incompatible with the regular
 * string.
 *
 * @property value the underlying string value.
 */
@JvmInline
value class TestSuiteName(val value: String) {
    override fun toString(): String =
            value
}

/**
 * @return the name of this test suite.
 */
fun TestSuite.name(): TestSuiteName =
        TestSuiteName(name)
