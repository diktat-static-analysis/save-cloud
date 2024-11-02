package com.saveourtool.save.test.analysis.metrics

import com.saveourtool.common.test.analysis.metrics.*
import com.saveourtool.save.test.analysis.TestAnalysisApplication
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import com.fasterxml.jackson.core.type.TypeReference
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.DurationUnit.SECONDS
import kotlin.time.toDuration
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * @see TestMetrics
 */
@SpringBootTest(classes = [TestAnalysisApplication::class])
@Import(TestMetricsConfig::class)
class TestMetricsJvmTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `encode single`() {
        assertThat(objectMapper.writeValueAsString(NoDataAvailable.instance))
            .isNotBlank

        assertThat(
            objectMapper.writeValueAsString(
                RegularTestMetrics(
                    successCount = 1,
                    failureCount = 2,
                    flipCount = 3,
                    ignoredCount = 4,
                    averageDurationOrNull = 5.toDuration(MINUTES),
                    medianDurationOrNull = 6.toDuration(SECONDS),
                )
            )
        ).isNotBlank
            .containsSubsequence("\"PT5M\"", "\"PT6S\"")

        assertThat(
            objectMapper.writeValueAsString(
                RegularTestMetrics(
                    successCount = 1,
                    failureCount = 2,
                    flipCount = 3,
                    ignoredCount = 4,
                    averageDurationOrNull = null,
                    medianDurationOrNull = null,
                )
            )
        ).isNotBlank
    }

    @Test
    fun `encode multiple`() {
        val results: List<TestMetrics> = listOf(
            NoDataAvailable.instance,
            RegularTestMetrics(1, 2, 3, 4, null, null),
        )

        /*
         * Prevent type erasure.
         */
        val listType = object : TypeReference<List<TestMetrics>>() {}
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
            NoDataAvailable.instance,
            RegularTestMetrics(1, 2, 3, 4, null, null),
            RegularTestMetrics(1, 2, 3, 4, 5.toDuration(MINUTES), 6.toDuration(SECONDS)),
        ).forEach { result ->
            val encoded = objectMapper.writeValueAsString(result)
            val decoded: TestMetrics = Json.decodeFromString(encoded)
            assertThat(decoded).isEqualTo(result)
        }
    }

    /**
     * `kotlinx.serialization` should be able to read values serialized with
     * _Jackson_.
     */
    @Test
    fun `decode multiple`() {
        val results: List<TestMetrics> = listOf(
            NoDataAvailable.instance,
            RegularTestMetrics(1, 2, 3, 4, null, null),
            RegularTestMetrics(1, 2, 3, 4, 5.toDuration(MINUTES), 6.toDuration(SECONDS)),
        )

        /*
         * Prevent type erasure.
         */
        val listType = object : TypeReference<List<TestMetrics>>() {}
        val encoded = objectMapper.writerFor(listType).writeValueAsString(results)
        val decoded: List<TestMetrics> = Json.decodeFromString(encoded)
        assertThat(decoded).containsExactlyElementsOf(results)
    }
}
