package com.saveourtool.common.test.analysis.metrics

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.DurationUnit.SECONDS
import kotlin.time.toDuration
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @see TestMetrics
 */
class TestMetricsTest {
    @Test
    @JsName("encodeSingle")
    fun `encode single`() {
        @Suppress("ComplexRedundantLet")
        Json.encodeToString(NoDataAvailable.instance).let { encoded ->
            assertNotEquals(illegal = "", actual = encoded)
        }

        Json.encodeToString(
            RegularTestMetrics(
                successCount = 1,
                failureCount = 2,
                flipCount = 3,
                ignoredCount = 4,
                averageDurationOrNull = 5.toDuration(MINUTES),
                medianDurationOrNull = 6.toDuration(SECONDS),
            )
        ).let { encoded ->
            assertNotEquals(illegal = "", actual = encoded)
            assertContains(charSequence = encoded, other = "\"PT5M\"")
            assertContains(charSequence = encoded, other = "\"PT6S\"")
        }

        Json.encodeToString(
            RegularTestMetrics(
                successCount = 1,
                failureCount = 2,
                flipCount = 3,
                ignoredCount = 4,
                averageDurationOrNull = null,
                medianDurationOrNull = null,
            )
        ).let { encoded ->
            assertNotEquals(illegal = "", actual = encoded)
        }
    }

    @Test
    @JsName("encodeMultiple")
    fun `encode multiple`() {
        val results: List<TestMetrics> = listOf(
            NoDataAvailable.instance,
            RegularTestMetrics(1, 2, 3, 4, null, null),
        )

        val encoded = Json.encodeToString(results)
        assertNotEquals(illegal = "", actual = encoded)
        assertContains(charSequence = encoded, other = "\"type\"")
    }

    @Test
    @JsName("decodeSingle")
    fun `decode single`() {
        sequenceOf(
            NoDataAvailable.instance,
            RegularTestMetrics(1, 2, 3, 4, null, null),
            RegularTestMetrics(1, 2, 3, 4, 5.toDuration(MINUTES), 6.toDuration(SECONDS)),
        ).forEach { result ->
            val encoded = Json.encodeToString(result)
            val decoded: TestMetrics = Json.decodeFromString(encoded)
            assertEquals(expected = result, actual = decoded)
        }
    }

    @Test
    @JsName("decodeMultiple")
    fun `decode multiple`() {
        val results: List<TestMetrics> = listOf(
            NoDataAvailable.instance,
            RegularTestMetrics(1, 2, 3, 4, null, null),
            RegularTestMetrics(1, 2, 3, 4, 5.toDuration(MINUTES), 6.toDuration(SECONDS)),
        )

        val encoded = Json.encodeToString(results)
        val decoded: List<TestMetrics> = Json.decodeFromString(encoded)
        assertContentEquals(expected = results, actual = decoded)
    }
}
