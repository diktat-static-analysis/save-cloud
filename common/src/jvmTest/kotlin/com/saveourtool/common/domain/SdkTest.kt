package com.saveourtool.common.domain

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SdkTest {
    @Test
    fun `should parse SDK correctly from strings`() {
        listOf(
            "Java:9",
            "eclipse-temurin:9"
        ).forEach {
            Assertions.assertTrue(it.toSdk() is Jdk)
            Assertions.assertEquals("9", it.toSdk().version)
        }
        listOf(
            "Python:3.5",
            "python:3.5"
        ).forEach {
            Assertions.assertTrue(it.toSdk() is Python)
            Assertions.assertEquals("3.5", it.toSdk().version)
        }
    }
}
