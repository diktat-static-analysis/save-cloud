package com.saveourtool.common.test.analysis.results

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @see AnalysisResult
 */
class AnalysisResultTest {
    @Test
    @JsName("encodeSingle")
    fun `encode single`() {
        @Suppress("ComplexRedundantLet")
        Json.encodeToString(RegularTest.instance).let { encoded ->
            assertNotEquals(illegal = "", actual = encoded)
        }

        Json.encodeToString(FlakyTest("flaky")).let { encoded ->
            assertNotEquals(illegal = "", actual = encoded)
            assertContains(charSequence = encoded, other = "flaky")
        }

        Json.encodeToString(Regression("regression")).let { encoded ->
            assertNotEquals(illegal = "", actual = encoded)
            assertContains(charSequence = encoded, other = "regression")
        }

        Json.encodeToString(PermanentFailure("permanent failure")).let { encoded ->
            assertNotEquals(illegal = "", actual = encoded)
            assertContains(charSequence = encoded, other = "permanent failure")
        }
    }

    @Test
    @JsName("encodeMultiple1")
    fun `encode multiple 1`() {
        val results: List<AnalysisResult> = listOf(
            RegularTest.instance,
            FlakyTest("flaky"),
            Regression("regression"),
            PermanentFailure("permanent failure"),
        )

        val encoded = Json.encodeToString(results)
        assertNotEquals(illegal = "", actual = encoded)
        assertContains(charSequence = encoded, other = "\"type\"")
    }

    @Test
    @JsName("encodeMultiple2")
    fun `encode multiple 2`() {
        val results: List<IrregularTest> = listOf(
            FlakyTest("flaky"),
            Regression("regression"),
            PermanentFailure("permanent failure"),
        )

        val encoded = Json.encodeToString(results)
        assertNotEquals(illegal = "", actual = encoded)
        assertContains(charSequence = encoded, other = "\"type\"")
    }

    @Test
    @JsName("decodeSingle")
    fun `decode single`() {
        sequenceOf(
            RegularTest.instance,
            FlakyTest("flaky"),
            Regression("regression"),
            PermanentFailure("permanent failure"),
        ).forEach { result ->
            val encoded = Json.encodeToString(result)
            val decoded: AnalysisResult = Json.decodeFromString(encoded)
            assertEquals(expected = result, actual = decoded)
        }
    }

    @Test
    @JsName("decodeMultiple1")
    fun `decode multiple 1`() {
        val results: List<AnalysisResult> = listOf(
            RegularTest.instance,
            FlakyTest("flaky"),
            Regression("regression"),
            PermanentFailure("permanent failure"),
        )

        val encoded = Json.encodeToString(results)
        val decoded: List<AnalysisResult> = Json.decodeFromString(encoded)
        assertContentEquals(expected = results, actual = decoded)
    }

    @Test
    @JsName("decodeMultiple2")
    fun `decode multiple 2`() {
        val results: List<IrregularTest> = listOf(
            FlakyTest("flaky"),
            Regression("regression"),
            PermanentFailure("permanent failure"),
        )

        val encoded = Json.encodeToString(results)
        val decoded: List<IrregularTest> = Json.decodeFromString(encoded)
        assertContentEquals(expected = results, actual = decoded)
    }
}
