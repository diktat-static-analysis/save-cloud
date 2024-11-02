package com.saveourtool.common.validation

import com.saveourtool.common.validation.ValidationRegularExpressions.URL_VALIDATOR
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * @see ValidationRegularExpressions
 */
class ValidationRegularExpressionsTest {
    @Test
    fun `http url with www prefix`() {
        assertTrue(URL_VALIDATOR.value.matches("http://www.example.com"))
        assertTrue(URL_VALIDATOR.value.matches("http://www.example.com/"))
    }

    @Test
    fun `http url without www prefix`() {
        assertTrue(URL_VALIDATOR.value.matches("http://example.com"))
        assertTrue(URL_VALIDATOR.value.matches("http://example.com/"))
    }

    @Test
    fun `https url with www prefix`() {
        assertTrue(URL_VALIDATOR.value.matches("https://www.example.com"))
        assertTrue(URL_VALIDATOR.value.matches("https://www.example.com/"))
    }

    @Test
    fun `https url without www prefix`() {
        assertTrue(URL_VALIDATOR.value.matches("https://example.com"))
        assertTrue(URL_VALIDATOR.value.matches("https://example.com/"))
    }

    @Test
    fun `url with a dash in the host component`() {
        assertTrue(URL_VALIDATOR.value.matches("https://unix-junkie.github.io"))
        assertTrue(URL_VALIDATOR.value.matches("https://unix-junkie.github.io/"))
    }

    @Test
    fun `url with a path component`() {
        assertTrue(URL_VALIDATOR.value.matches("https://example.com/~user"))
        assertTrue(URL_VALIDATOR.value.matches("https://example.com/~user/"))
    }

    @Test
    fun `non-http url`() {
        assertFalse(URL_VALIDATOR.value.matches("ftp://example.com"))
        assertFalse(URL_VALIDATOR.value.matches("ssh://example.com"))
        assertFalse(URL_VALIDATOR.value.matches("gopher://example.com"))
    }

    @Test
    fun `long top-level suffixes`() {
        assertTrue(URL_VALIDATOR.value.matches("https://example.com.singles"))
        assertTrue(URL_VALIDATOR.value.matches("https://example.com.shopping"))
        assertTrue(URL_VALIDATOR.value.matches("https://example.com.software"))
        assertTrue(URL_VALIDATOR.value.matches("https://example.com.spreadbetting"))
        assertTrue(URL_VALIDATOR.value.matches("https://example.com.travelersinsurance"))
    }
}
