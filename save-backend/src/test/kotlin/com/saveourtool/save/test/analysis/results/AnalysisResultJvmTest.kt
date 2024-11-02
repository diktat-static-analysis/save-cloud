package com.saveourtool.save.test.analysis.results

import com.saveourtool.common.test.analysis.results.*
import com.saveourtool.save.test.analysis.TestAnalysisApplication
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * @see AnalysisResult
 */
@SpringBootTest(classes = [TestAnalysisApplication::class])
@Import(AnalysisResultConfig::class)
class AnalysisResultJvmTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `encode single`() {
        assertThat(objectMapper.writeValueAsString(RegularTest.instance))
            .isNotBlank

        assertThat(objectMapper.writeValueAsString(FlakyTest("flaky")))
            .isNotBlank
            .contains("flaky")

        assertThat(objectMapper.writeValueAsString(Regression("regression")))
            .isNotBlank
            .contains("regression")

        assertThat(objectMapper.writeValueAsString(PermanentFailure("permanent failure")))
            .isNotBlank
            .contains("permanent failure")
    }

    @Test
    fun `encode multiple 1`() {
        val results: List<AnalysisResult> = listOf(
            RegularTest.instance,
            FlakyTest("flaky"),
            Regression("regression"),
            PermanentFailure("permanent failure"),
        )

        /*
         * Prevent type erasure.
         */
        val listType = object : TypeReference<List<AnalysisResult>>() {}
        val encoded = objectMapper.writerFor(listType).writeValueAsString(results)
        assertThat(encoded)
            .isNotBlank
            .contains("\"type\"")
    }

    @Test
    fun `encode multiple 2`() {
        val results: List<IrregularTest> = listOf(
            FlakyTest("flaky"),
            Regression("regression"),
            PermanentFailure("permanent failure"),
        )

        /*
         * Prevent type erasure.
         */
        val listType = object : TypeReference<List<IrregularTest>>() {}
        val encoded = objectMapper.writerFor(listType).writeValueAsString(results)
        assertThat(encoded)
            .isNotBlank
            .contains("\"type\"")
    }

    /**
     * `kotlinx.serialization` should be able to read values serialized with
     * _Jackson_.
     */
    @Test
    fun `decode single`() {
        sequenceOf(
            RegularTest.instance,
            FlakyTest("flaky"),
            Regression("regression"),
            PermanentFailure("permanent failure"),
        ).forEach { result ->
            val encoded = objectMapper.writeValueAsString(result)
            val decoded: AnalysisResult = Json.decodeFromString(encoded)
            assertThat(decoded).isEqualTo(result)
        }
    }

    /**
     * `kotlinx.serialization` should be able to read values serialized with
     * _Jackson_.
     */
    @Test
    fun `decode multiple 1`() {
        val results: List<AnalysisResult> = listOf(
            RegularTest.instance,
            FlakyTest("flaky"),
            Regression("regression"),
            PermanentFailure("permanent failure"),
        )

        /*
         * Prevent type erasure.
         */
        val listType = object : TypeReference<List<AnalysisResult>>() {}
        val encoded = objectMapper.writerFor(listType).writeValueAsString(results)
        val decoded: List<AnalysisResult> = Json.decodeFromString(encoded)
        assertThat(decoded).containsExactlyElementsOf(results)
    }

    /**
     * `kotlinx.serialization` should be able to read values serialized with
     * _Jackson_.
     */
    @Test
    fun `decode multiple 2`() {
        val results: List<IrregularTest> = listOf(
            FlakyTest("flaky"),
            Regression("regression"),
            PermanentFailure("permanent failure"),
        )

        /*
         * Prevent type erasure.
         */
        val listType = object : TypeReference<List<IrregularTest>>() {}
        val encoded = objectMapper.writerFor(listType).writeValueAsString(results)
        val decoded: List<IrregularTest> = Json.decodeFromString(encoded)
        assertThat(decoded).containsExactlyElementsOf(results)
    }
}
