package com.saveourtool.save.backend.service

import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.Test
import com.saveourtool.common.entities.TestExecution
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.common.test.TestBatch
import com.saveourtool.common.test.TestDto
import com.saveourtool.common.utils.orNotFound
import com.saveourtool.save.backend.repository.ExecutionRepository
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.repository.TestRepository

import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that is used for manipulating data with tests
 */
@Service
@Suppress("LongParameterList")
class TestService(
    private val testRepository: TestRepository,
    private val executionRepository: ExecutionRepository,
    private val testExecutionRepository: TestExecutionRepository,
    private val testSuitesService: TestSuitesService,
    private val lnkExecutionTestSuiteService: LnkExecutionTestSuiteService,
    transactionManager: PlatformTransactionManager,
) {
    private val locks: ConcurrentHashMap<Long, Any> = ConcurrentHashMap()
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = PROPAGATION_REQUIRES_NEW
    }

    /**
     * @param tests
     * @return list tests id's
     */
    @Suppress("UnsafeCallOnNullableType")
    fun saveTests(tests: List<TestDto>): List<Long> {
        val (existingTests, nonExistentTests) = tests
            .map { testDto -> testDto.copy(filePath = FilenameUtils.separatorsToUnix(testDto.filePath)) }
            .map { testDto ->
                // only match fields that are present in DTO
                testRepository.findByHashAndFilePathAndTestSuiteIdAndPluginName(testDto.hash,
                    testDto.filePath, testDto.testSuiteId, testDto.pluginName).map {
                    log.debug("Test $testDto is already present with id=${it.id} and testSuiteId=${testDto.testSuiteId}")
                    it
                }
                    .orElseGet {
                        log.trace("Test $testDto is not found in the DB, will save it")
                        Test(
                            testDto.hash,
                            testDto.filePath,
                            testDto.pluginName,
                            LocalDateTime.now(),
                            testSuitesService.getById(testDto.testSuiteId),
                            additionalFiles = testDto.joinAdditionalFiles(),
                        )
                    }
            }
            .partition { it.id != null }
        testRepository.saveAll(nonExistentTests)
        return (existingTests + nonExistentTests).map { it.requiredId() }
    }

    /**
     * @param execution
     * @return Test batches
     */
    @Transactional
    @Suppress("UnsafeCallOnNullableType")
    fun getTestBatches(execution: Execution): Mono<TestBatch> {
        val lock = locks.computeIfAbsent(execution.requiredId()) { Any() }
        return synchronized(lock) {
            log.debug("Acquired lock for executionId=${execution.requiredId()}")
            val testExecutions = transactionTemplate.execute {
                getTestExecutionsBatchByExecutionIdAndUpdateStatus(execution)
            }!!
            Mono.fromCallable {
                val testBatch = testExecutions.map { it.test.toDto() }
                log.debug("Releasing lock for executionId=${execution.requiredId()}")
                testBatch
            }
        }
    }

    /**
     * @param testSuiteId
     * @return tests with provided [testSuiteId]
     */
    fun findTestsByTestSuiteId(testSuiteId: Long) =
            testRepository.findAllByTestSuiteId(testSuiteId)

    /**
     * @param testSuiteId
     * @return tests with provided [testSuiteId]
     */
    fun findFirstTestByTestSuiteId(testSuiteId: Long) =
            testRepository.findFirstByTestSuiteId(testSuiteId)

    /**
     * @param executionId
     * @return all tests which has testSuiteId from [execution][com.saveourtool.save.entities.Execution] found by provided [executionId]
     * @throws ResponseStatusException when execution is not found by [executionId] or found execution doesn't contain testSuiteIds
     */
    fun findTestsByExecutionId(executionId: Long): List<Test> {
        val testSuiteIds = lnkExecutionTestSuiteService.getAllTestSuiteIdsByExecutionId(executionId)
        return testSuiteIds.flatMap { findTestsByTestSuiteId(it) }
    }

    /**
     * Retrieves a batch of test executions with status `READY_FOR_TESTING` from the datasource and sets their statuses to `RUNNING`
     *
     * @param srcExecution execution for which a batch is requested
     * @return a batch of [batchSize] tests with status `READY_FOR_TESTING`
     */
    @Suppress("UnsafeCallOnNullableType")
    internal fun getTestExecutionsBatchByExecutionIdAndUpdateStatus(srcExecution: Execution): List<TestExecution> {
        val executionId = srcExecution.requiredId()
        val execution = executionRepository.findWithLockingById(executionId).orNotFound()
        val batchSize = execution.batchSize!!
        val pageRequest = PageRequest.of(0, batchSize)
        val testExecutions = testExecutionRepository.findByStatusAndExecutionId(
            TestResultStatus.READY_FOR_TESTING,
            executionId,
            pageRequest
        )
        log.debug("Retrieved ${testExecutions.size} tests for page request $pageRequest, test IDs: ${testExecutions.map { it.requiredId() }}")
        val newRunningTestExecutions = testExecutions.onEach { testExecution ->
            testExecutionRepository.save(testExecution.apply {
                status = TestResultStatus.RUNNING
            })
        }.count()
        executionRepository.saveAndFlush(execution.apply {
            log.debug("Updating counter for running tests: $runningTests -> ${runningTests + newRunningTestExecutions}")
            runningTests += newRunningTestExecutions
        })
        return testExecutions
    }

    /**
     * Remove execution ids from [locks] for executions that are no more running
     */
    @Scheduled(cron = "0 0/50 * * * ?")
    @Suppress("UnsafeCallOnNullableType")
    fun cleanupLocks() {
        log.debug("Starting scheduled task of `locks` map cleanup")
        executionRepository.findAllById(locks.keys).forEach {
            if (it.status != ExecutionStatus.RUNNING) {
                log.debug("Will remove key=[${it.id!!}] from the map, because execution state is ${it.status}")
                locks.remove(it.id!!)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestService::class.java)
    }
}
