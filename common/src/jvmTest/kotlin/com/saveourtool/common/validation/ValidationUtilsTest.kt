package com.saveourtool.common.validation

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ValidationUtilsTest {
    @Test
    fun `valid name`() {
        Assertions.assertTrue("some-name".isValidName())
        Assertions.assertTrue("some.name1".isValidName())
        Assertions.assertTrue("some_name1".isValidName())
    }

    @Test
    fun `invalid name`() {
        Assertions.assertFalse("-some-name".isValidName())
        Assertions.assertFalse("some-name-".isValidName())
        Assertions.assertFalse(".some.name1".isValidName())
        Assertions.assertFalse("some.name1.".isValidName())
        Assertions.assertFalse("_some_name1".isValidName())
        Assertions.assertFalse("some_name1_".isValidName())
        Assertions.assertFalse("some-name".isValidName(4))
    }
}
