package com.saveourtool.save.backend.controller

import com.saveourtool.common.agent.TestExecutionDto
import com.saveourtool.common.agent.TestExecutionExtDto
import com.saveourtool.common.agent.TestExecutionResult
import com.saveourtool.common.agent.TestSuiteExecutionStatisticDto
import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.LnkExecutionAgentRepository
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.storage.DebugInfoStorage
import com.saveourtool.save.backend.storage.ExecutionInfoStorage
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.utils.secondsToJLocalDateTime
import com.saveourtool.common.v1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.BodyInserters
import reactor.kotlin.core.publisher.toMono
import java.time.Instant

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(InfraExtension::class)
@MockBeans(
    MockBean(ProjectController::class),
)
class TestExecutionControllerTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var testExecutionRepository: TestExecutionRepository

    @Autowired
    private lateinit var agentRepository: AgentRepository
    @Autowired
    private lateinit var lnkExecutionAgentRepository: LnkExecutionAgentRepository
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @MockBean
    private lateinit var debugInfoStorage: DebugInfoStorage

    @MockBean
    private lateinit var executionInfoStorage: ExecutionInfoStorage

    @BeforeEach
    fun setUp() {
        transactionTemplate = TransactionTemplate(transactionManager)
        whenever(debugInfoStorage.doesExist(any())).thenReturn(false.toMono())
        whenever(executionInfoStorage.doesExist(any())).thenReturn(false.toMono())
    }

    @Test
    @WithMockUser
    fun `should count TestExecutions for a particular Execution`() {
        mutateMockedUser(id = 99)

        webClient.get()
            .uri("/api/${v1}/testExecution/count?executionId=1")
            .exchange()
            .expectBody<Int>()
            .isEqualTo(28)
    }

    @Test
    @WithMockUser
    fun `should return a page of TestExecutions for a particular Execution`() {
        mutateMockedUser(id = 99)

        val expectedExecutionCount = 20
        webClient.post()
            .uri("/api/${v1}/test-executions?executionId=1&page=0&size=$expectedExecutionCount")
            .exchange()
            .expectBody<List<com.saveourtool.common.agent.TestExecutionExtDto>>()
            .consumeWith {
                assertEquals(expectedExecutionCount, it.responseBody!!.size)
            }
    }

    @Test
    @WithMockUser
    fun `should return a list test suits with number of test for executions id`() {
        mutateMockedUser(id = 99)

        webClient.get()
            .uri("/api/${v1}/testLatestExecutions?executionId=3&status=${TestResultStatus.PASSED}&page=0&size=10")
            .exchange()
            .expectBody<List<com.saveourtool.common.agent.TestSuiteExecutionStatisticDto>>()
            .consumeWith {
                assertEquals(1, it.responseBody!!.size)
            }
    }

    /**
     * Don't forget: SpringBootTest with controllers doesn't rollback transactions like test with repositories only.
     * Should be using different execution id for newly inserted data in order not to clash with tests
     * that check data read.
     */
    @Test
    @WithMockUser
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    fun `should save TestExecutionDto into the DB`() {
        val testExecutionDtoFirst = TestExecutionResult(
            "testPath29",
            "WarnPlugin",
            "container-3",
            "save-container-3",
            TestResultStatus.FAILED,
            DEFAULT_DATE_TEST_EXECUTION,
            DEFAULT_DATE_TEST_EXECUTION,
            unmatched = 3,
            matched = 2,
            expected = 0,
            unexpected = 0,
        )
        val testExecutionDtoSecond = TestExecutionResult(
            "testPath30",
            "WarnPlugin",
            "container-3",
            "save-container-3",
            TestResultStatus.PASSED,
            DEFAULT_DATE_TEST_EXECUTION,
            DEFAULT_DATE_TEST_EXECUTION,
            unmatched = 4,
            matched = 3,
            expected = 0,
            unexpected = 0,
        )
        val passedTestsBefore = getExecutionsTestsResultByAgentContainerId(testExecutionDtoSecond.agentContainerId, true)
        val failedTestsBefore = getExecutionsTestsResultByAgentContainerId(testExecutionDtoFirst.agentContainerId, false)
        webClient.post()
            .uri("/internal/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecutionDtoFirst, testExecutionDtoSecond)))
            .exchange()
            .expectBody<String>()
            .isEqualTo("Saved")
        val tests = getAllTestExecutions()
        val passedTestsAfter = getExecutionsTestsResultByAgentContainerId(testExecutionDtoSecond.agentContainerId, true)
        val failedTestsAfter = getExecutionsTestsResultByAgentContainerId(testExecutionDtoFirst.agentContainerId, false)
        assertTrue(tests.any { it.startTime == testExecutionDtoFirst.startTimeSeconds.secondsToJLocalDateTime().withNano(0) })
        assertTrue(tests.any { it.endTime == testExecutionDtoFirst.endTimeSeconds.secondsToJLocalDateTime().withNano(0) })
        assertEquals(passedTestsBefore, passedTestsAfter - 1)
        assertEquals(failedTestsBefore, failedTestsAfter - 1)
        assertTrue(tests.any {
            it.unmatched == testExecutionDtoFirst.unmatched &&
                    it.matched == testExecutionDtoFirst.matched &&
                    it.expected == testExecutionDtoFirst.expected &&
                    it.unexpected == testExecutionDtoFirst.unexpected
        })
        assertTrue(tests.any {
            it.unmatched == testExecutionDtoSecond.unmatched &&
                    it.matched == testExecutionDtoSecond.matched &&
                    it.expected == testExecutionDtoSecond.expected &&
                    it.unexpected == testExecutionDtoSecond.unexpected
        })
    }

    @Test
    @WithMockUser
    fun `should not save data if provided fields are invalid`() {
        val testExecutionDto = TestExecutionResult(
            "test-not-exists",
            "WarnPlugin",
            "container-1",
            "save-container-1",
            TestResultStatus.FAILED,
            DEFAULT_DATE_TEST_EXECUTION,
            DEFAULT_DATE_TEST_EXECUTION,
            unmatched = 0,
            matched = 0,
            expected = 0,
            unexpected = 0,
        )
        webClient.post()
            .uri("/internal/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecutionDto)))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody<String>()
            .isEqualTo("Some ids don't exist or cannot be updated")
        val testExecutions = testExecutionRepository.findAll()
        assertTrue(testExecutions.none { it.test.filePath == "test-not-exists" })
    }

    @Test
    @WithMockUser
    fun `should return error if provided empty result`() {
        val countBefore = testExecutionRepository.findAll().size
        webClient.post()
            .uri("/internal/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(emptyList<TestExecutionDto>()))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody<String>()
            .isEqualTo("Empty result cannot be saved")
        assertEquals(countBefore, testExecutionRepository.findAll().size)
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun getAllTestExecutions() =
            transactionTemplate.execute {
                testExecutionRepository.findAll()
            }!!

    @Suppress("UnsafeCallOnNullableType")
    private fun getExecutionsTestsResultByAgentContainerId(id: String, isPassed: Boolean) =
            transactionTemplate.execute {
                agentRepository.findByContainerId(id)
                    ?.requiredId()
                    ?.let { lnkExecutionAgentRepository.findByAgentId(it) }
                    ?.execution
                    ?.let {
                        if (isPassed) {
                            it.passedTests
                        } else {
                            it.failedTests
                        }
                    }!!
            }!!

    companion object {
        private val DEFAULT_DATE_TEST_EXECUTION: Long = Instant.now().epochSecond
    }
}
