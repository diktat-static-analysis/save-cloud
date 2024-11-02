package com.saveourtool.save.test.analysis.api

import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.domain.TestResultStatus.FAILED
import com.saveourtool.common.domain.TestResultStatus.IGNORED
import com.saveourtool.common.domain.TestResultStatus.PASSED
import com.saveourtool.save.test.analysis.internal.ExtendedTestRun
import com.saveourtool.save.test.analysis.internal.MemoryBacked
import com.saveourtool.save.test.analysis.internal.MutableTestStatisticsStorage
import com.saveourtool.common.test.analysis.metrics.NoDataAvailable
import com.saveourtool.common.test.analysis.metrics.RegularTestMetrics
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.DurationUnit.SECONDS
import kotlin.time.toDuration

/**
 * @see TestStatisticsStorage
 */
class TestStatisticsStorageTest {
    private lateinit var storage: MutableTestStatisticsStorage

    @BeforeEach
    fun beforeEach() {
        storage = MemoryBacked(slidingWindowSize = Int.MAX_VALUE)
    }

    @AfterEach
    fun afterEach() {
        storage.clear()
    }

    @Test
    fun `execution statistics should get updated`() {
        assertThat(storage.getExecutionStatistics(testId)).hasSize(0)

        storage[testId] += TestRun(PASSED, null)
        assertThat(storage.getExecutionStatistics(testId)).hasSize(1)

        storage[testId] += TestRun(FAILED, null)
        assertThat(storage.getExecutionStatistics(testId)).hasSize(2)
    }

    @Test
    fun `test metrics should get updated`() {
        assertThat(storage.getTestMetrics(testId)).isEqualTo(NoDataAvailable.instance)

        storage[testId] += TestRun(PASSED, null)
        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.failureRatePercentage).isZero
            assertThat(metrics.flipRatePercentage).isZero

            assertThat(metrics.failureRate).isZero
            assertThat(metrics.flipRate).isZero

            assertThat(metrics.runCount).isEqualTo(1)
            assertThat(metrics.successCount).isEqualTo(1)
            assertThat(metrics.failureCount).isEqualTo(0)
            assertThat(metrics.ignoredCount).isZero

            assertThat(metrics.averageDurationOrNull).isNull()
            assertThat(metrics.medianDurationOrNull).isNull()
        }

        storage[testId] += TestRun(FAILED, null)
        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.failureRatePercentage).isEqualTo(50)
            assertThat(metrics.flipRatePercentage).isEqualTo(100)

            assertThat(metrics.failureRate).isBetween(0.49, 0.51)
            assertThat(metrics.flipRate).isEqualTo(1.0)

            assertThat(metrics.runCount).isEqualTo(2)
            assertThat(metrics.successCount).isEqualTo(1)
            assertThat(metrics.failureCount).isEqualTo(1)
            assertThat(metrics.ignoredCount).isZero

            assertThat(metrics.averageDurationOrNull).isNull()
            assertThat(metrics.medianDurationOrNull).isNull()
        }
    }

    @Test
    fun `flip count should be calculated correctly, case 1`() {
        storage[testId] += TestRun(PASSED, null)
        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.flipCount).isZero()
        }

        storage[testId] += TestRun(FAILED, null)
        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.flipCount).isEqualTo(1)
        }

        storage[testId] += TestRun(PASSED, null)
        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.flipCount).isEqualTo(2)
        }
    }

    @Test
    fun `flip count should be calculated correctly, case 2`() {
        storage[testId] += TestRun(FAILED, null)
        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.flipCount).isZero()
        }

        storage[testId] += TestRun(PASSED, null)
        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.flipCount).isEqualTo(1)
        }

        storage[testId] += TestRun(FAILED, null)
        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.flipCount).isEqualTo(2)
        }
    }

    @Test
    fun `average duration should be calculated correctly`() {
        storage[testId] += TestRun(PASSED, 1L.toDuration(SECONDS))
        storage[testId] += TestRun(PASSED, 2L.toDuration(SECONDS))
        storage[testId] += TestRun(PASSED, 3L.toDuration(SECONDS))
        storage[testId] += TestRun(PASSED, 4L.toDuration(SECONDS))
        storage[testId] += TestRun(PASSED, 5L.toDuration(SECONDS))

        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.averageDurationOrNull)
                .isNotNull
                .isEqualTo(3L.toDuration(SECONDS))
        }
    }

    @Test
    @Suppress("LONG_NUMERICAL_VALUES_SEPARATED")
    fun `median duration should be calculated correctly`() {
        storage[testId] += TestRun(PASSED, 1L.toDuration(SECONDS))
        storage[testId] += TestRun(PASSED, 2L.toDuration(SECONDS))
        storage[testId] += TestRun(PASSED, 3L.toDuration(SECONDS))
        storage[testId] += TestRun(PASSED, 4L.toDuration(SECONDS))
        storage[testId] += TestRun(PASSED, 100500L.toDuration(SECONDS))

        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.medianDurationOrNull)
                .isNotNull
                .isEqualTo(3L.toDuration(SECONDS))
        }

        storage[testId] += TestRun(PASSED, 100500L.toDuration(SECONDS))

        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.medianDurationOrNull)
                .isNotNull
                .isEqualTo(3_500L.toDuration(MILLISECONDS))
        }
    }

    @Test
    fun `indeterminate test runs should be ignored by default`() {
        enumValues<TestResultStatus>().asSequence()
            .filterNot { it == PASSED }
            .filterNot { it == FAILED }
            .filterNot { it == IGNORED }
            .forEach { status ->
                storage[testId] += TestRun(status, null)
            }

        assertThat(storage.getTestMetrics(testId))
            .isInstanceOf(NoDataAvailable::class.java)
        assertThat(storage.getExecutionStatistics(testId))
            .isEmpty()
        assertThat(storage.getExecutionStatistics(testId, ignoreIndeterminate = false))
            .isNotEmpty
    }

    @Test
    fun `eviction should occur if sliding window size is limited`() {
        val slidingWindowSize = 1000
        storage = MemoryBacked(slidingWindowSize = slidingWindowSize)

        repeat(times = slidingWindowSize) {
            storage[testId] += TestRun(FAILED, null)
        }
        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.runCount).isEqualTo(slidingWindowSize)
            assertThat(metrics.failureRate).isEqualTo(1.0)
            assertThat(metrics.flipCount).isZero()
        }

        repeat(times = slidingWindowSize / 2) {
            storage[testId] += TestRun(PASSED, null)
        }
        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.runCount).isEqualTo(slidingWindowSize)
            assertThat(metrics.failureRate).isBetween(0.49, 0.51)
            assertThat(metrics.flipCount).isEqualTo(1)
        }

        repeat(times = slidingWindowSize / 2) {
            storage[testId] += TestRun(PASSED, null)
        }
        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.runCount).isEqualTo(slidingWindowSize)
            assertThat(metrics.failureRate).isEqualTo(0.0)
            assertThat(metrics.flipCount).isZero()
        }
    }

    @Test
    fun `consecutive test runs with duplicate execution id should be discarded`() {
        storage[testId] += ExtendedTestRun(0L, testId, PASSED, 1L.seconds)

        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.runCount).isEqualTo(1)
        }

        storage[testId] += ExtendedTestRun(1L, testId, PASSED, 1L.seconds)

        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.runCount).isEqualTo(2)
        }

        /*
         * Will be discarded.
         */
        storage[testId] += ExtendedTestRun(1L, testId, PASSED, 1L.seconds)

        assertThat(storage.getTestMetrics(testId)).isInstanceOfSatisfying(RegularTestMetrics::class.java) { metrics ->
            assertThat(metrics.runCount).isEqualTo(2)
        }
    }

    private operator fun TestRuns.plusAssign(testRun: TestRun) {
        this += ExtendedTestRun(
            executionId.getAndIncrement(),
            testId,
            testRun,
        )
    }

    private operator fun TestRuns.plusAssign(testRun: ExtendedTestRun) {
        storage.updateExecutionStatistics(testRun)
    }

    private companion object {
        private val executionId = AtomicLong()
        private val testId = TestId("")

        /**
         * Returns the detailed statistics about all test runs for the given [id],
         * ignoring runs with an indeterminate test status.
         *
         * @param id the unique test identifier.
         * @see TestStatisticsStorage.getExecutionStatistics
         */
        private operator fun TestStatisticsStorage.get(id: TestId): TestRuns =
                getExecutionStatistics(id)
    }
}
