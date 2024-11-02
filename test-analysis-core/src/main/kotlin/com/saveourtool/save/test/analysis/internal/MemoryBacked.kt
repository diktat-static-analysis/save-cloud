package com.saveourtool.save.test.analysis.internal

import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.save.test.analysis.api.TestId
import com.saveourtool.save.test.analysis.api.TestRuns
import com.saveourtool.save.test.analysis.api.TestStatisticsStorage
import com.saveourtool.save.test.analysis.api.TestStatusProvider
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap

/**
 * The memory-backed [TestStatisticsStorage] implementation.
 *
 * @param slidingWindowSize the size of the sliding window (the maximum sample
 *   size preserved in memory for any given test).
 * @property testStatusProvider the converter from the implementation-specific
 *   test status into boolean success/failure flags.
 * @see TestStatisticsStorage
 */
class MemoryBacked(
    override val testStatusProvider: TestStatusProvider<TestResultStatus> = TestStatusProvider(),
    private val slidingWindowSize: Int = DEFAULT_SLIDING_WINDOW_SIZE,
) : MutableTestStatisticsStorage {
    private val groupedTestRuns: MutableMap<TestId, ExtendedTestRuns> = ConcurrentHashMap()

    override fun getExecutionStatistics(
        id: TestId,
        ignoreIndeterminate: Boolean,
    ): TestRuns =
            with(testStatusProvider) {
                val testRuns = groupedTestRuns[id].orEmpty()

                when {
                    ignoreIndeterminate -> testRuns.asSequence().filterNot { testRun ->
                        testRun.isIndeterminate()
                    }.toList()

                    else -> testRuns
                }
            }

    override fun updateExecutionStatistics(testRunExt: ExtendedTestRun) {
        val (executionId, testId, testRun) = testRunExt
        groupedTestRuns.compute(testId) { _, oldValue: ExtendedTestRuns? ->
            when (oldValue?.lastExecutionId) {
                /*-
                 * Ignore the execution if the id of this execution has already
                 * been seen for this particular test id.
                 *
                 * This is necessary because, for the same execution,
                 * `ExecutionService.updateExecutionStatus()`
                 * may be invoked multiple times with the same status
                 * (e.g.: `FINISHED`).
                 */
                executionId -> oldValue

                /*-
                 * Create a copy instead of modifying the existing list:
                 *
                 * 1. For the mutation to be idempotent (not an issue with CHM,
                 *    but definitely an issue with CSLM).
                 * 2. To avoid a `ConcurrentModificationException` (a reader thread
                 *    may be currently iterating over this very list). If the list
                 *    is being resized, because the state may be only partially
                 *    visible, an NPE (not only a CME) may get thrown on the reader
                 *    thread.
                 *
                 * This is a poor-man's COWAL implementation (where COWAL is not
                 * actually necessary), since we won't be able to atomically
                 * add-and-evict anyway.
                 *
                 * Consider rewriting using `EvictingQueue` (Guava) or
                 * `CircularFifoQueue` (Apache commons-collections).
                 */
                else -> ExtendedTestRuns(
                    testRuns = LinkedList(oldValue.orEmpty()).apply {
                        /*
                         * A naïve implementation which expects that `add(Int, TestRun)`
                         * is never invoked.
                         */
                        val isAdded = add(testRun)
                        while (isAdded && size > slidingWindowSize) {
                            remove()
                        }
                    },
                    lastExecutionId = executionId,
                )
            }
        }
    }

    override fun clear() =
            groupedTestRuns.clear()

    private companion object {
        private const val DEFAULT_SLIDING_WINDOW_SIZE = Int.MAX_VALUE
    }
}
