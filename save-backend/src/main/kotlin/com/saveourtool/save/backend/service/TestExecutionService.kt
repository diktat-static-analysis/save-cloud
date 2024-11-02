package com.saveourtool.save.backend.service

import com.saveourtool.common.agent.TestExecutionDto
import com.saveourtool.common.agent.TestExecutionResult
import com.saveourtool.common.domain.TestResultLocation
import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.Test
import com.saveourtool.common.entities.TestExecution
import com.saveourtool.common.filters.TestExecutionFilter
import com.saveourtool.common.test.TestDto
import com.saveourtool.common.utils.*
import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.ExecutionRepository
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.core.result.CountWarnings

import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDateTime

/**
 * Service for test result
 */
@Service
class TestExecutionService(
    private val testExecutionRepository: TestExecutionRepository,
    private val agentRepository: AgentRepository,
    private val agentService: AgentService,
    private val executionRepository: ExecutionRepository,
) {
    /**
     * Returns a page of [TestExecution]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param page a zero-based index of page of data
     * @param pageSize size of page
     * @param filters
     * @return a list of [TestExecutionDto]s
     */
    @Suppress("AVOID_NULL_CHECKS", "UnsafeCallOnNullableType")
    internal fun getTestExecutions(
        executionId: Long,
        page: Int,
        pageSize: Int,
        filters: TestExecutionFilter,
    ): List<TestExecution> {
        val wrappedFileName = wrapValue(filters.fileName)
        val wrappedTestSuiteName = wrapValue(filters.testSuite)
        val wrappedTagValue = wrapValue(filters.tag)
        return testExecutionRepository.findByExecutionIdAndStatusAndTestTestSuiteName(
            executionId,
            filters.status,
            wrappedFileName,
            wrappedTestSuiteName,
            wrappedTagValue,
            PageRequest.of(page, pageSize),
        )
    }

    private fun wrapValue(value: String?) = value?.let {
        "%$value%"
    }

    /**
     * @param executionId
     * @return a list of test executions
     */
    internal fun getAllTestExecutions(executionId: Long): List<TestExecution> =
            testExecutionRepository.findByExecutionId(executionId)

    /**
     * @param executionId
     * @param page
     * @param pageSize
     * @param status
     * @return a list of test executions
     */
    @Suppress("UnsafeCallOnNullableType")
    internal fun getByExecutionIdGroupByTestSuite(
        executionId: Long,
        status: TestResultStatus,
        page: Int,
        pageSize: Int,
    ): List<Array<*>>? =
            testExecutionRepository.findByExecutionIdGroupByTestSuite(executionId, status.name, PageRequest.of(page, pageSize))

    /**
     * Get test executions by [containerId] and [status]
     *
     * @param containerId
     * @param status
     * @return a list of test executions
     */
    internal fun getAllTestExecutions(containerId: String, status: TestResultStatus) = testExecutionRepository
        .findByAgentContainerIdAndStatus(containerId, status)

    /**
     * Finds TestExecution by test location
     *
     * @param executionId under this executionId test has been executed
     * @param testResultLocation location of the test
     * @return optional TestExecution
     */
    internal fun getTestExecution(executionId: Long, testResultLocation: TestResultLocation) = with(testResultLocation) {
        testExecutionRepository.findByExecutionIdAndTestPluginNameAndTestFilePath(
            executionId, pluginName, FilenameUtils.separatorsToUnix(testPath)
        )
    }

    /**
     * Returns number of TestExecutions with this [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param status
     * @param testSuite
     * @return number of TestExecutions
     */
    @Suppress("AVOID_NULL_CHECKS", "UnsafeCallOnNullableType")
    internal fun getTestExecutionsCount(executionId: Long, status: TestResultStatus?, testSuite: String?) =
            testExecutionRepository.countByExecutionIdAndStatusAndTestTestSuiteName(executionId, status, testSuite)

    /**
     * @param executionIds list of ids
     * @return Unit
     */
    internal fun deleteTestExecutionByExecutionIds(executionIds: List<Long>) =
            testExecutionRepository.deleteByExecutionIdIn(executionIds)

    /**
     * @param testExecutionResults
     * @return list of lost tests
     */
    @Suppress(
        "TOO_MANY_LINES_IN_LAMBDA",
        "TOO_LONG_FUNCTION",
        "UnsafeCallOnNullableType",
        "LongMethod",
        "MAGIC_NUMBER",
        "MagicNumber",
        "PARAMETER_NAME_IN_OUTER_LAMBDA",
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveTestResult(testExecutionResults: List<com.saveourtool.common.agent.TestExecutionResult>): List<com.saveourtool.common.agent.TestExecutionResult> {
        log.debug { "Saving ${testExecutionResults.size} test results from agent ${testExecutionResults.first().agentContainerId}" }
        // we take agent id only from first element, because all test executions have same execution
        val agentContainerId = requireNotNull(testExecutionResults.first().agentContainerId) {
            "Attempt to save test results without assigned agent. testExecutionResults=$testExecutionResults"
        }
        val agent = requireNotNull(agentRepository.findByContainerId(agentContainerId)) {
            "Agent with containerId=[$agentContainerId] was not found in the DB"
        }

        val executionId = agentService.getExecution(agent).requiredId()
        val lostTests: MutableList<com.saveourtool.common.agent.TestExecutionResult> = mutableListOf()
        val counters = Counters()
        testExecutionResults.forEach { testExecutionResult ->
            val foundTestExec = testExecutionRepository.findByExecutionIdAndTestPluginNameAndTestFilePath(
                executionId,
                testExecutionResult.pluginName,
                testExecutionResult.filePath
            )
            foundTestExec.also {
                it ?: log.error("Test execution $testExecutionResult for execution id=$executionId was not found in the DB")
            }
                ?.takeIf {
                    // update only those test executions, that haven't been updated before
                    it.status == TestResultStatus.RUNNING
                }
                ?.let {
                    it.startTime = testExecutionResult.startTimeSeconds.secondsToJLocalDateTime()
                    it.endTime = testExecutionResult.endTimeSeconds.secondsToJLocalDateTime()
                    it.status = testExecutionResult.status
                    when (testExecutionResult.status) {
                        TestResultStatus.PASSED -> counters.passed++
                        TestResultStatus.FAILED -> counters.failed++
                        else -> counters.skipped++
                    }
                    it.unmatched = testExecutionResult.unmatched
                    it.matched = testExecutionResult.matched
                    it.expected = testExecutionResult.expected
                    it.unexpected = testExecutionResult.unexpected

                    with(counters) {
                        unmatchedChecks += testExecutionResult.unmatched.orZeroIfNotApplicable()
                        matchedChecks += testExecutionResult.matched.orZeroIfNotApplicable()
                        expectedChecks += testExecutionResult.expected.orZeroIfNotApplicable()
                        unexpectedChecks += testExecutionResult.unexpected.orZeroIfNotApplicable()
                    }

                    testExecutionRepository.save(it)
                }
                ?: run {
                    lostTests.add(testExecutionResult)
                    val testExecutionId = foundTestExec?.requiredId()
                    log.error("Test execution $testExecutionResult with id=$testExecutionId for execution id=$executionId cannot be updated because its status is not RUNNING")
                }
        }
        val execution = executionRepository.findWithLockingById(executionId).orNotFound()
        execution.apply {
            log.debug {
                "Updating counters in execution id=$executionId: running=$runningTests-${counters.total()}, " +
                        "passed=$passedTests+${counters.passed}, failed=$failedTests+${counters.failed}, skipped=$skippedTests+${counters.skipped}"
            }
            runningTests -= counters.total()
            passedTests += counters.passed
            failedTests += counters.failed
            skippedTests += counters.skipped

            unmatchedChecks += counters.unmatchedChecks
            matchedChecks += counters.matchedChecks
            expectedChecks += counters.expectedChecks
            unexpectedChecks += counters.unexpectedChecks

            val executionScore = toDto().calculateScore(scoreType = ScoreType.F_MEASURE)

            if (!executionScore.isValidScore()) {
                log.error("Execution score for execution id $id is invalid: $executionScore")
            }
            score = executionScore
        }
        executionRepository.save(execution)
        return lostTests
    }

    /**
     * @param execution the [Execution], during which these tests will be executed
     * @param tests the tests, which will be executed
     */
    fun saveTestExecutions(execution: Execution, tests: List<Test>) {
        val executionId = execution.requiredId()
        log.debug { "Will create test executions for executionId=$executionId for tests ${tests.map { it.requiredId() }}" }
        tests.map { test ->
            val testId = test.requiredId()
            val testExecutionList = testExecutionRepository.findByExecutionIdAndTestId(executionId, testId)
            if (testExecutionList.isNotEmpty()) {
                log.debug { "For execution with id=$executionId test id=$testId already exists in DB, deleting it" }
                testExecutionRepository.deleteAllByExecutionIdAndTestId(executionId, testId)
            }
            log.debug("Creating TestExecution for test $testId")
            val testExecution = testExecutionRepository.save(
                TestExecution(
                    test = test,
                    execution = execution,
                    agent = null,
                    status = TestResultStatus.READY_FOR_TESTING,
                    startTime = null,
                    endTime = null,
                    unmatched = null,
                    matched = null,
                    expected = null,
                    unexpected = null,
                )
            )
            log.debug { "Created TestExecution ${testExecution.requiredId()} for test $testId" }
        }
    }

    /**
     * Set `agent` field of test executions corresponding to [testDtos] to [agentContainerId]
     *
     * @param agentContainerId id of an agent
     * @param testDtos test that will be executed by [agentContainerId] agent
     */
    @Transactional
    @Suppress("UnsafeCallOnNullableType")
    fun assignAgentByTest(agentContainerId: String, testDtos: List<TestDto>) {
        val agent = requireNotNull(agentRepository.findByContainerId(agentContainerId)) {
            "Agent with containerId=[$agentContainerId] was not found in the DB"
        }
        val executionId = agentService.getExecution(agent).requiredId()
        testDtos.forEach { test ->
            val testExecution = testExecutionRepository.findByExecutionIdAndTestPluginNameAndTestFilePath(
                executionId,
                test.pluginName,
                test.filePath
            ).orNotFound {
                val errorMessage = "Can't find test_execution for executionId=$executionId, test.pluginName=${test.pluginName}, test.filePath=${test.filePath}"
                log.error(errorMessage)
                errorMessage
            }
            testExecutionRepository.save(testExecution.apply {
                this.agent = agent
            })
        }
    }

    /**
     * @param containerId the container ID of agent, for which corresponding test executions should be marked as failed
     */
    @Transactional
    @Suppress("UnsafeCallOnNullableType")
    fun markReadyForTestingTestExecutionsOfAgentAsFailed(containerId: String) {
        val agent = agentService.getAgentByContainerId(containerId)
        val agentId = agent.requiredId()
        val executionId = agentService.getExecution(agent).requiredId()

        val testExecutionList = testExecutionRepository.findByExecutionIdAndAgentIdAndStatus(
            executionId,
            agentId,
            TestResultStatus.READY_FOR_TESTING,
        )

        if (testExecutionList.isEmpty()) {
            // Crashed agent could be not assigned with tests, so just warn and return
            log.warn("Can't find `test_execution`s for executionId=$executionId and agentId=$agentId")
            return
        }
        testExecutionList.doMarkTestExecutionOfAgentsAsFailed()
    }

    /**
     * @param executionId the ID of an execution, for which corresponding test executions should be marked as failed
     */
    @Transactional
    fun markAllTestExecutionsOfExecutionAsFailed(executionId: Long) {
        val testExecutionList = testExecutionRepository.findByExecutionId(executionId)
        if (testExecutionList.isEmpty()) {
            // Crashed agent could be not assigned with tests, so just warn and return
            log.warn("Can't find `test_execution`s for executionId=$executionId")
            return
        }
        testExecutionList.doMarkTestExecutionOfAgentsAsFailed()
    }

    private fun List<TestExecution>.doMarkTestExecutionOfAgentsAsFailed() = this
        .map { testExecution ->
            testExecution.apply {
                this.status = TestResultStatus.INTERNAL_ERROR
                // In case of execution without errors all information about test execution we take from
                // json report, however in case when agent is crashed, it's unavailable, so fill at least end time
                this.endTime = LocalDateTime.now()
            }
        }
        .let { testExecutionRepository.saveAll(it) }
        .also {
            log.info("Test executions with ids ${this.map { it.requiredId() }} were failed with internal error")
        }

    private fun Long?.orZeroIfNotApplicable() = this?.takeUnless { CountWarnings.isNotApplicable(it.toInt()) } ?: 0

    @Suppress(
        "KDOC_NO_CONSTRUCTOR_PROPERTY",
        "MISSING_KDOC_ON_FUNCTION",
        "LongParameterList",
        "KDOC_NO_CONSTRUCTOR_PROPERTY_WITH_COMMENT",
    )
    private class Counters(
        var passed: Int = 0,
        var failed: Int = 0,
        var skipped: Int = 0,

        // how many checks/validations are not found, but we expect them
        var unmatchedChecks: Long = 0,
        // how many checks/validations matched to expected results
        var matchedChecks: Long = 0,
        // how many checks/validations we expect
        var expectedChecks: Long = 0,
        // how many checks/validations are found, but we don't expect them
        var unexpectedChecks: Long = 0,

        // note: missedResults = expectedResults - matchedResults
    ) {
        fun total() = passed + failed + skipped
    }

    private companion object {
        private val log: Logger = getLogger<TestExecutionService>()
    }
}
