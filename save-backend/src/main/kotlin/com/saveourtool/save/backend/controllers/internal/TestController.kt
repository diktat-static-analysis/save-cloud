package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.backend.service.TestExecutionService
import com.saveourtool.save.backend.service.TestService
import com.saveourtool.save.entities.Test
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 *  Controller used to initialize tests
 */
@RestController
@RequestMapping("/internal")
class TestController(
    private val testService: TestService,
    private val testExecutionService: TestExecutionService,
    private val meterRegistry: MeterRegistry,
) {
    /**
     * @param testDtos list of [TestDto]s to save into the DB
     * @param executionId ID of the [Execution], during which these tests will be initialized
     * @return list of initialized [Tests][Test]
     */
    @PostMapping("/initializeTests")
    fun initializeTests(@RequestBody testDtos: List<TestDto>, @RequestParam executionId: Long) {
        log.debug { "Received the following tests for initialization under executionId=$executionId: $testDtos" }
        meterRegistry.timer("save.backend.saveTests").record<List<Test>> {
            testService.saveTests(testDtos)
        }!!
    }

    /**
     * @param executionId ID of the [Execution][com.saveourtool.save.entities.Execution],
     * all tests are initialized for this execution will be executed (creating an instance of [TestExecution][com.saveourtool.save.entities.TestExecution])
     */
    @PostMapping("/executeTestsByExecutionId")
    fun executeTestsByExecutionId(@RequestParam executionId: Long) {
        val tests = getTestsByExecutionId(executionId)
        log.debug { "Received the following test ids for saving test execution under executionId=$executionId: ${tests.map { it.id }}" }
        meterRegistry.timer("save.backend.saveTestExecution").record {
            testExecutionService.updateExecutionAndSaveTestExecutions(executionId, tests)
        }
    }

    /**
     * @param testSuiteId ID of the [TestSuite], for which all corresponding tests will be returned
     * @return list of tests
     */
    @GetMapping("/getTestsByTestSuiteId")
    fun getTestsByTestSuiteId(@RequestParam testSuiteId: Long): List<Test> = testService.findTestsByTestSuiteId(testSuiteId)

    /**
     * @param executionId ID of the [Execution][com.saveourtool.save.entities.Execution], for which all corresponding tests will be returned
     * @return list of tests
     */
    @GetMapping("/getTestsByExecutionId")
    fun getTestsByExecutionId(@RequestParam executionId: Long): List<Test> = testService.findTestsByExecutionId(executionId)

    /**
     * @param agentId
     * @return test batches
     */
    @GetMapping("/getTestBatches")
    fun testBatches(@RequestParam agentId: String) = testService.getTestBatches(agentId)

    companion object {
        private val log: Logger = getLogger<TestController>()
    }
}
