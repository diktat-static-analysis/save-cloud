@file:Suppress("FILE_UNORDERED_IMPORTS")

package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.ExecutionRepository
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.Project
import com.saveourtool.common.entities.TestExecution
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.service.ProjectService
import com.saveourtool.save.test.analysis.api.TestId
import com.saveourtool.save.test.analysis.api.TestIdGenerator
import com.saveourtool.save.test.analysis.api.TestRun
import com.saveourtool.save.test.analysis.api.TestStatisticsStorage
import com.saveourtool.save.test.analysis.api.testId
import com.saveourtool.save.test.analysis.entities.metadata
import com.saveourtool.save.test.analysis.internal.ExtendedTestRun
import com.saveourtool.save.test.analysis.internal.MutableTestStatisticsStorage
import com.saveourtool.common.test.analysis.metrics.TestMetrics
import com.saveourtool.common.test.analysis.results.AnalysisResult
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.utils.info
import com.saveourtool.common.utils.warn
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jmx.export.annotation.ManagedOperation
import org.springframework.jmx.export.annotation.ManagedOperationParameter
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.Comparator.comparingLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Stream
import javax.annotation.concurrent.GuardedBy
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlinx.datetime.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.UtcOffset.Companion.ZERO
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinLocalDateTime
import com.saveourtool.save.test.analysis.api.TestAnalysisService as LowLevelAnalysisService

/**
 * The high-level test analysis service.
 *
 * @see LowLevelAnalysisService
 * @see TestStatisticsStorage
 */
@Service
@ManagedResource
@Suppress(
    "LongParameterList",
    "TOO_MANY_PARAMETERS",
)
class TestAnalysisService(
    private val statisticsStorage: MutableTestStatisticsStorage,
    private val testIdGenerator: TestIdGenerator,
    private val testExecutionRepository: TestExecutionRepository,
    private val executionRepository: ExecutionRepository,
    private val organizationService: OrganizationService,
    private val projectService: ProjectService,
    config: ConfigProperties,
) {
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    private val logger = getLogger<TestAnalysisService>()

    /**
     * The low-level test analysis service instance.
     */
    private val lowLevelAnalysisService = LowLevelAnalysisService(statisticsStorage)

    /**
     * Whether historical data should be read in parallel.
     * This is the default flag value, which is only used for entry points w/o
     * parameters.
     *
     * For [clearAndReplayHistoricalData] JMX operation, this flag is overridden
     * by the function parameter.
     */
    private val defaultParallel: Boolean = config.testAnalysisSettings.parallelStartup

    /**
     * Guards [statisticsStorage] access.
     *
     * This is a coarse lock intended just to prevent multiple concurrent replay
     * operations.
     * [MutableTestStatisticsStorage] implementation is internally thread-safe.
     */
    private val statisticsStorageLock = ReentrantReadWriteLock()

    /**
     * Controls running of [replayHistoricalData]
     *
     * Needs to disable it to generate springdoc openapi.
     */
    private val replayOnStartup: Boolean = config.testAnalysisSettings.replayOnStartup

    /**
     * Populates the in-memory statistics by replaying historical data.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun replayHistoricalData() {
        if (!replayOnStartup) {
            logger.warn {
                "Skip replaying historical test data..."
            }
        } else {
            logger.info {
                "Replaying historical test data..."
            }
            replayHistoricalData(defaultParallel)
        }
    }

    /**
     * Populates the in-memory statistics by replaying historical data.
     *
     * @param parallel whether historical data should be read in parallel.
     */
    private fun replayHistoricalData(parallel: Boolean) {
        val testRunCount: Long

        val nanos = statisticsStorageLock.write {
            @Suppress("Destructure")
            measureNanoTime {
                testRunCount = organizationService.findAll()
                    .parallelStreamIfEnabled(enabled = parallel)
                    .flatMap { organization ->
                        projectService.getAllByOrganizationName(organization.name)
                            .parallelStreamIfEnabled(enabled = parallel)
                    }
                    .mapToLong { project ->
                        updateStatistics(project, parallel)
                    }
                    .sum()
            }
        }

        @Suppress("MagicNumber", "FLOAT_IN_ACCURATE_CALCULATIONS")
        logger.info {
            "Started ${javaClass.simpleName} ($testRunCount test run(s)) in ${nanos / 1000L / 1e3} ms (parallel = $parallel)."
        }
    }

    /**
     * Returns the aggregate statistics for the given [testId].
     *
     * @param testId the unique test id.
     * @return the scalar test metrics for the given [testId].
     * @see TestStatisticsStorage.getTestMetrics
     */
    @Suppress("GrazieInspection")
    fun getTestMetrics(testId: TestId): Mono<TestMetrics> =
            Mono.fromCallable {
                statisticsStorageLock.read {
                    statisticsStorage.getTestMetrics(testId)
                }
            }

    /**
     * Analyzes test runs for the given test [testId] and returns the [Flux] of
     * results.
     *
     * @param testId the unique test id.
     * @return the [Flux] of analysis results for the given test [testId].
     * @see LowLevelAnalysisService.analyze
     */
    fun analyze(testId: TestId): Flux<AnalysisResult> =
            Flux.fromStream {
                statisticsStorageLock.read {
                    lowLevelAnalysisService.analyze(testId)
                }.stream()
            }

    /**
     * Clears any statistical data collected.
     */
    @ManagedOperation(
        description = "Clears any statistical data collected",
    )
    fun clear() {
        statisticsStorageLock.write {
            statisticsStorage.clear()
        }

        logger.info {
            "Test data cleared."
        }
    }

    /**
     * Clears and re-populates the in-memory statistics by replaying historical
     * data.
     *
     * @param parallel whether historical data should be read in parallel.
     */
    @ManagedOperation(
        description = "Clears and re-populates the in-memory statistics by replaying historical data",
    )
    @ManagedOperationParameter(
        name = "parallel",
        description = "Whether historical data should be read in parallel",
    )
    fun clearAndReplayHistoricalData(
        parallel: Boolean
    ) {
        statisticsStorageLock.write {
            clear()
            replayHistoricalData(parallel)
        }
    }

    /**
     * Updates the statistics with test executions from this [execution].
     *
     * @param execution a newly-finished `Execution`.
     * @see updateStatisticsInternal
     */
    fun updateStatistics(execution: Execution) {
        val testRunCount: Long

        val nanos = measureNanoTime {
            testRunCount = statisticsStorageLock.write {
                updateStatisticsInternal(execution, defaultParallel)
            }
        }

        logger.info {
            @Suppress(
                "MagicNumber",
                "FLOAT_IN_ACCURATE_CALCULATIONS",
            )
            "Test statistics updated (+$testRunCount test run(s)) in ${nanos / 1000L / 1e3} ms from execution (id = ${execution.requiredId()})"
        }
    }

    /**
     * Updates the statistics with test executions from this [execution].
     *
     * May be invoked concurrently from multiple threads, so should be
     * externally guarded.
     *
     * @param execution either a historical or a newly-finished `Execution`.
     * @param parallel whether data should be read in parallel.
     * @return the number of test executions for this [execution].
     * @see updateStatistics
     */
    @GuardedBy("statisticsStorageLock")
    private fun updateStatisticsInternal(
        execution: Execution,
        parallel: Boolean,
    ): Long {
        val executionId = execution.requiredId()
        val metadata = execution.metadata()

        /*
         * Process test executions in parallel (slightly faster).
         */
        return testExecutionRepository.findByExecutionId(executionId)
            .parallelStreamIfEnabled(enabled = parallel)
            .map { testExecution ->
                val testId = metadata.extendWith(testExecution).let(testIdGenerator::testId)

                statisticsStorage.updateExecutionStatistics(
                    ExtendedTestRun(
                        executionId,
                        testId,
                        testExecution.asTestRun()
                    )
                )
            }
            .countEagerly()
    }

    /**
     * May be invoked concurrently from multiple threads, so should be
     * externally guarded.
     *
     * @param parallel whether historical data should be read in parallel.
     * @return the number of test executions for this [project].
     * @see updateStatisticsInternal
     */
    @GuardedBy("statisticsStorageLock")
    private fun updateStatistics(
        project: Project,
        parallel: Boolean,
    ): Long {
        /*
         * Process executions sequentially.
         */
        return executionRepository.getAllByProjectNameAndProjectOrganization(project.name, project.organization)
            .stream()
            .sequential()
            .sorted(comparingLong(Execution::requiredId))
            .mapToLong { execution ->
                updateStatisticsInternal(execution, parallel)
            }
            .sum()
    }

    private companion object {
        private fun <T : Any> Collection<T>.parallelStreamIfEnabled(enabled: Boolean): Stream<out T> =
                when {
                    enabled -> parallelStream()
                    else -> stream()
                }

        /**
         * Because [Stream.count] may avoid executing the stream pipeline.
         *
         * @return the count of elements in this stream.
         * @see Stream.count
         */
        private fun <T : Any> Stream<out T>.countEagerly(): Long =
                mapToLong { 1L }.sum()

        /**
         * Represents this test execution with a smaller [TestRun] instance, allowing to
         * conserve memory.
         *
         * @return the [TestRun] view of this test execution.
         */
        private fun TestExecution.asTestRun(): TestRun =
                TestRun(status, durationOrNull())

        /**
         * @return the duration of this test execution as `kotlin.time.Duration`.
         */
        private fun TestExecution.durationOrNull(): Duration? {
            val offset = ZERO
            val startTime = startTime?.toInstant(offset) ?: return null
            val endTime = endTime?.toInstant(offset) ?: return null

            return endTime - startTime
        }

        private fun LocalDateTime.toInstant(offset: UtcOffset): Instant =
                toKotlinLocalDateTime().toInstant(offset)
    }
}
