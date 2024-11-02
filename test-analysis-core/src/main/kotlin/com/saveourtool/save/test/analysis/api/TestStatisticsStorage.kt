package com.saveourtool.save.test.analysis.api

import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.test.analysis.metrics.NoDataAvailable
import com.saveourtool.common.test.analysis.metrics.RegularTestMetrics
import com.saveourtool.common.test.analysis.metrics.TestMetrics
import kotlin.time.Duration
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.toDuration

/**
 * The storage of statistical data about test runs.
 */
interface TestStatisticsStorage : TestStatusProviderScope<TestResultStatus> {
    /**
     * Returns the detailed statistics about all test runs for the given [id].
     *
     * @param ignoreIndeterminate whether runs with an indeterminate test status
     *   should be ignored.
     *   The default is `true`.
     * @param id the unique test identifier.
     * @return the list of [TestRun] instances for the given test [id].
     */
    fun getExecutionStatistics(
        id: TestId,
        ignoreIndeterminate: Boolean = true,
    ): TestRuns

    /**
     * Returns the aggregate statistics for the given [id].
     *
     * @param id the unique test identifier.
     * @return the scalar test metrics for the given test [id].
     */
    fun getTestMetrics(id: TestId): TestMetrics =
            with(testStatusProvider) {
                val testRuns = getExecutionStatistics(id)

                when {
                    testRuns.isEmpty() -> NoDataAvailable.instance
                    else -> RegularTestMetrics(
                        successCount = testRuns.count { it.isSuccess() },
                        failureCount = testRuns.count { it.isFailure() },
                        flipCount = testRuns.flipCount(),
                        ignoredCount = testRuns.count { it.isIgnored() },
                        averageDurationOrNull = testRuns.averageDurationOrNull(),
                        medianDurationOrNull = testRuns.medianDurationOrNull(),
                    )
                }
            }

    private fun TestRuns.averageDurationOrNull(): Duration? =
            with(testStatusProvider) {
                check(!isEmpty())

                val durationsMillis = asSequence()
                    .map(TestRun::durationOrNull)
                    .filterNotNull()
                    .map(Duration::inWholeMilliseconds)
                    .toList()

                when {
                    durationsMillis.isEmpty() -> null
                    else -> durationsMillis
                        .average()
                        .toLong()
                        .toDuration(MILLISECONDS)
                }
            }

    private fun TestRuns.medianDurationOrNull(): Duration? =
            with(testStatusProvider) {
                check(!isEmpty())

                val durationsMillis: ArrayList<Long> = asSequence()
                    .map(TestRun::durationOrNull)
                    .filterNotNull()
                    .map(Duration::inWholeMilliseconds)
                    .toCollection(arrayListOf())
                    .apply(MutableList<Long>::sort)

                when {
                    durationsMillis.isEmpty() -> null

                    else -> durationsMillis
                        .median()
                        .toLong()
                        .toDuration(MILLISECONDS)
                }
            }

    private fun TestRuns.flipCount(): Int =
            when (size) {
                0 -> 0

                /*
                 * It requires at least two consecutive runs for a single flip
                 * to occur.
                 */
                1 -> 0

                else -> foldIndexed(0 to null as TestRun?) { index, (previousFlipCount, previousTestRun), testRun ->
                    val flipCount = when (index) {
                        0 -> 0
                        else -> when {
                            isFlip(checkNotNull(previousTestRun), testRun) -> previousFlipCount + 1
                            else -> previousFlipCount
                        }
                    }

                    flipCount to testRun
                }.first
            }

    private fun isFlip(previous: TestRun, current: TestRun): Boolean =
            with(testStatusProvider) {
                when {
                    previous.isSuccess() -> current.isFailure()
                    previous.isFailure() -> current.isSuccess()
                    else -> false
                }
            }

    private companion object {
        /**
         * @return the median of the _sorted_ list.
         */
        @Suppress(
            "MAGIC_NUMBER",
            "FLOAT_IN_ACCURATE_CALCULATIONS",
        )
        private fun ArrayList<Long>.median(): Double =
                when {
                    isEmpty() -> Double.NaN

                    /*
                     * Evenly-sized, non-empty.
                     */
                    size % 2 == 0 -> {
                        val rightIndex = size / 2
                        val leftIndex = rightIndex - 1
                        (this[leftIndex] + this[rightIndex]) / 2.0
                    }

                    /*
                     * Oddly-sized.
                     */
                    else -> {
                        val index = (size - 1) / 2
                        this[index].toDouble()
                    }
                }
    }
}
