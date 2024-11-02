package com.saveourtool.save.test.analysis.api

import com.saveourtool.common.domain.TestResultStatus.FAILED
import com.saveourtool.common.domain.TestResultStatus.IGNORED
import com.saveourtool.common.domain.TestResultStatus.PASSED
import com.saveourtool.save.test.analysis.api.TestAnalysisService.Factory.MINIMUM_RUN_COUNT
import com.saveourtool.save.test.analysis.internal.ExtendedTestRun
import com.saveourtool.save.test.analysis.internal.MemoryBacked
import com.saveourtool.save.test.analysis.internal.MutableTestStatisticsStorage
import com.saveourtool.common.test.analysis.results.FlakyTest
import com.saveourtool.common.test.analysis.results.PermanentFailure
import com.saveourtool.common.test.analysis.results.Regression
import com.saveourtool.common.test.analysis.results.RegularTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicLong

/**
 * @see TestAnalysisService
 */
class TestAnalysisServiceTest {
    private lateinit var storage: MutableTestStatisticsStorage

    @Suppress("WRONG_NEWLINES")
    private lateinit var analyzer: TestAnalysisService

    @BeforeEach
    fun beforeEach() {
        storage = MemoryBacked(slidingWindowSize = Int.MAX_VALUE)
        analyzer = TestAnalysisService(storage)
    }

    @AfterEach
    fun afterEach() {
        storage.clear()
    }

    @Test
    fun `when there's no data, a test should have the regular status`() {
        val results = analyzer.analyze(testId)
        assertThat(results)
            .hasSize(1)
            .containsExactly(RegularTest.instance)
    }

    @Test
    fun `permanent success`() {
        repeat(times = 100) {
            storage[testId] += TestRun(PASSED, null)
        }

        val results = analyzer.analyze(testId)
        assertThat(results)
            .hasSize(1)
            .containsExactly(RegularTest.instance)
    }

    @Test
    fun `ignored test`() {
        repeat(times = 100) {
            storage[testId] += TestRun(IGNORED, null)
        }

        val results = analyzer.analyze(testId)
        assertThat(results)
            .hasSize(1)
            .containsExactly(RegularTest.instance)
    }

    @Test
    fun `permanent failure`() {
        val runCount = 100

        repeat(times = runCount) {
            storage[testId] += TestRun(FAILED, null)
        }

        val results = analyzer.analyze(testId)
        assertThat(results)
            .hasSize(1)
            .allSatisfy { result ->
                assertThat(result).isInstanceOfSatisfying(PermanentFailure::class.java) { (message) ->
                    assertThat(message).isEqualTo("All $runCount run(s) have failed")
                }
            }
    }

    @Test
    fun `permanent failure status should get cleared after one successful run`() {
        val runCount = 100

        repeat(times = runCount) {
            storage[testId] += TestRun(FAILED, null)
        }
        assertThat(analyzer.analyze(testId))
            .hasSize(1)
            .allSatisfy { result ->
                assertThat(result).isInstanceOfSatisfying(PermanentFailure::class.java) { (message) ->
                    assertThat(message).isEqualTo("All $runCount run(s) have failed")
                }
            }

        storage[testId] += TestRun(PASSED, null)
        assertThat(analyzer.analyze(testId))
            .hasSize(1)
            .containsExactly(RegularTest.instance)
    }

    @Test
    fun `flaky test`() {
        val runCount = 100 * MINIMUM_RUN_COUNT

        flakyTestRuns(runCount = runCount)

        val results = analyzer.analyze(testId)
        assertThat(results)
            .hasSize(1)
            .allSatisfy { result ->
                assertThat(result).isInstanceOfSatisfying(FlakyTest::class.java) { (message) ->
                    assertThat(message).isEqualTo("Flip rate of 100% over $runCount run(s)")
                }
            }
    }

    @Test
    fun `flaky test, non-representative sample`() {
        flakyTestRuns(runCount = MINIMUM_RUN_COUNT - 1)

        val results = analyzer.analyze(testId)
        assertThat(results)
            .hasSize(1)
            .containsExactly(RegularTest.instance)
    }

    @Test
    fun `flaky test status should get cleared after flip rate drops below the threshold`() {
        val flakyRunCount = 100 * MINIMUM_RUN_COUNT

        flakyTestRuns(runCount = flakyRunCount)
        assertThat(analyzer.analyze(testId))
            .hasSize(1)
            .allSatisfy { result ->
                assertThat(result).isInstanceOfSatisfying(FlakyTest::class.java) { (message) ->
                    assertThat(message).isEqualTo("Flip rate of 100% over $flakyRunCount run(s)")
                }
            }

        repeat(times = flakyRunCount * 3) {
            storage[testId] += TestRun(FAILED, null)
        }
        assertThat(analyzer.analyze(testId))
            .hasSize(1)
            .containsExactly(RegularTest.instance)
    }

    @Test
    fun `test regression`() {
        val runCount = 100 * MINIMUM_RUN_COUNT

        regression(runCount = runCount)

        val results = analyzer.analyze(testId)
        assertThat(results)
            .hasSize(1)
            .allSatisfy { result ->
                assertThat(result).isInstanceOfSatisfying(Regression::class.java) { (message) ->
                    assertThat(message).isEqualTo("1 regression over $runCount run(s)")
                }
            }
    }

    @Test
    fun `test regression, non-representative sample`() {
        regression(runCount = MINIMUM_RUN_COUNT - 1)

        val results = analyzer.analyze(testId)
        assertThat(results)
            .hasSize(1)
            .containsExactly(RegularTest.instance)
    }

    @Test
    fun `test regression status should get cleared after one successful run`() {
        val runCount = 100 * MINIMUM_RUN_COUNT

        regression(runCount = runCount)
        assertThat(analyzer.analyze(testId))
            .hasSize(1)
            .allSatisfy { result ->
                assertThat(result).isInstanceOfSatisfying(Regression::class.java) { (message) ->
                    assertThat(message).isEqualTo("1 regression over $runCount run(s)")
                }
            }

        storage[testId] += TestRun(PASSED, null)
        assertThat(analyzer.analyze(testId))
            .hasSize(1)
            .containsExactly(RegularTest.instance)
    }

    /**
     * Makes sure that a continuous failure which gets eventually fixed doesn't
     * trigger a [Regression] marker.
     */
    @Test
    fun `fixed failure`() {
        val runCount = 100 * MINIMUM_RUN_COUNT

        fixedFailure(runCount = runCount)

        val results = analyzer.analyze(testId)
        assertThat(results)
            .hasSize(1)
            .containsExactly(RegularTest.instance)
    }

    /**
     * Creates [runCount] flaky test runs with a flip rate of about 100%.
     */
    private fun flakyTestRuns(runCount: Int) {
        repeat(times = runCount) { actionIndex ->
            val status = when {
                actionIndex % 2 == 0 -> PASSED
                else -> FAILED
            }
            storage[testId] += TestRun(status, null)
        }
    }

    /**
     * Simulates a regression over [runCount] runs.
     */
    private fun regression(runCount: Int) {
        require(runCount >= 2)

        val successCount = runCount / 2
        val failureCount = runCount - successCount

        repeat(times = successCount) {
            storage[testId] += TestRun(PASSED, null)
        }
        repeat(times = failureCount) {
            storage[testId] += TestRun(FAILED, null)
        }
    }

    /**
     * Simulates a permanent failure which got eventually fixed.
     * This is the opposite of [regression].
     *
     * @see regression
     */
    private fun fixedFailure(@Suppress("SameParameterValue") runCount: Int) {
        val successCount = runCount / 2
        val failureCount = runCount - successCount

        repeat(times = successCount) {
            storage[testId] += TestRun(FAILED, null)
        }
        repeat(times = failureCount) {
            storage[testId] += TestRun(PASSED, null)
        }
    }

    private operator fun TestRuns.plusAssign(testRun: TestRun) {
        storage.updateExecutionStatistics(
            ExtendedTestRun(
                executionId.getAndIncrement(),
                testId,
                testRun,
            )
        )
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
