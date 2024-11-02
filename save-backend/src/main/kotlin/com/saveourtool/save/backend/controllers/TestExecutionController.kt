package com.saveourtool.save.backend.controllers

import com.saveourtool.common.agent.TestExecutionDto
import com.saveourtool.common.agent.TestExecutionExtDto
import com.saveourtool.common.agent.TestExecutionResult
import com.saveourtool.common.agent.TestSuiteExecutionStatisticDto
import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.common.domain.TestResultLocation
import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.entities.TestExecution
import com.saveourtool.common.filters.TestExecutionFilter
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.security.ProjectPermissionEvaluator
import com.saveourtool.common.test.analysis.metrics.TestMetrics
import com.saveourtool.common.utils.*
import com.saveourtool.common.v1
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.service.TestAnalysisService
import com.saveourtool.save.backend.service.TestExecutionService
import com.saveourtool.save.backend.storage.DebugInfoStorage
import com.saveourtool.save.backend.storage.ExecutionInfoStorage
import com.saveourtool.save.backend.utils.toMonoOrNotFound
import com.saveourtool.save.test.analysis.api.TestIdGenerator
import com.saveourtool.save.test.analysis.api.testId
import com.saveourtool.save.test.analysis.entities.metadata

import arrow.core.plus
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.kotlin.extra.bool.logicalOr

import java.math.BigInteger

/**
 * Controller to work with test execution
 *
 * @param testExecutionService service for test execution
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "test-executions"),
)
@RestController
@Transactional
@Suppress("LongParameterList")
class TestExecutionController(
    private val testExecutionService: TestExecutionService,
    private val executionService: ExecutionService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
    private val debugInfoStorage: DebugInfoStorage,
    private val executionInfoStorage: ExecutionInfoStorage,
    private val testAnalysisService: TestAnalysisService,
    private val testIdGenerator: TestIdGenerator,
) {
    /**
     * Returns a page of [TestExecutionDto]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param page a zero-based index of page of data
     * @param size size of page
     * @param filters
     * @param authentication
     * @param checkDebugInfo if true, response will contain information about whether debug info data is available for this test execution
     * @param testAnalysis if `true`, also perform test analysis.
     * @return a list of [TestExecutionDto]s
     */
    @PostMapping("/api/${com.saveourtool.common.v1}/test-executions")
    @RequiresAuthorizationSourceHeader
    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS", "TYPE_ALIAS")
    fun getTestExecutions(
        @RequestParam executionId: Long,
        @RequestParam page: Int,
        @RequestParam size: Int,
        @RequestBody(required = false) filters: TestExecutionFilter?,
        @RequestParam(required = false, defaultValue = "false") checkDebugInfo: Boolean,
        @RequestParam(required = false, defaultValue = "false") testAnalysis: Boolean,
        authentication: Authentication,
    ): Flux<TestExecutionExtDto> = blockingToMono {
        executionService.findExecution(executionId)
    }
        .switchIfEmptyToNotFound()
        .filterWhen {
            projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ)
        }
        .flatMapMany { execution ->
            val testExecutions = Flux.fromStream {
                log.debug("Request to get test executions on page $page with size $size for execution $executionId")
                testExecutionService.getTestExecutions(executionId, page, size, filters ?: TestExecutionFilter.empty).stream()
            }

            /*
             * Test analysis: collect execution metadata.
             */
            val metadata = Mono.fromCallable(execution::metadata).cache().repeat()

            testExecutions.zipWith(metadata)
        }
        .map { (testExecution, metadata) ->
            testExecution to metadata
        }
        .mapRight { testExecution, metadata ->
            /*
             * Test analysis: collect test execution metadata.
             */
            metadata.extendWith(testExecution)
        }
        .mapLeft(TestExecution::toDto)
        .mapRight(testIdGenerator::testId)
        .run {
            when {
                /*
                 * Test analysis.
                 */
                testAnalysis -> switchMap { (testExecution, testId) ->
                    Mono.just(testExecution)
                        .zipWith(
                            testAnalysisService.getTestMetrics(testId),
                            ::Pair,
                        )
                        .zipWith(
                            testAnalysisService.analyze(testId).collectList(),
                            Pair<TestExecutionDto, TestMetrics>::plus,
                        )
                }.flatMap { (testExecution, metrics, results) ->
                    if (checkDebugInfo) {
                        testExecution.hasDebugInfoAsMono()
                            .map { hasDebugInfo ->
                                testExecution.toExtended(testMetrics = metrics, analysisResults = results, hasDebugInfo = hasDebugInfo)
                            }
                    } else {
                        testExecution.toExtended(testMetrics = metrics, analysisResults = results)
                            .toMono()
                    }
                }

                else -> flatMap { (testExecution, _) ->
                    testExecution.hasDebugInfoAsMono()
                        .map { hasDebugInfo ->
                            testExecution.toExtended(hasDebugInfo = hasDebugInfo)
                        }
                }
            }
        }

    private fun TestExecutionDto.hasDebugInfoAsMono() = debugInfoStorage.doesExist(requiredId())
        .logicalOr(executionInfoStorage.doesExist(executionId))
        .switchIfEmptyToResponseException(HttpStatus.INTERNAL_SERVER_ERROR) {
            "Failure while checking for debug info availability."
        }

    /**
     * @param executionId an ID of Execution to group TestExecutions
     * @param status of test
     * @param page a zero-based index of page of data
     * @param size size of page
     * @param authentication
     * @return a list of [TestExecutionDto]s
     */
    @GetMapping(path = ["/api/$v1/testLatestExecutions"])
    @RequiresAuthorizationSourceHeader
    @Suppress("TYPE_ALIAS", "MagicNumber")
    fun getTestExecutionsByStatus(
        @RequestParam executionId: Long,
        @RequestParam status: TestResultStatus,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        authentication: Authentication,
    ): Mono<List<TestSuiteExecutionStatisticDto>> =
            executionService.findExecution(executionId)
                .toMonoOrNotFound()
                .filterWhen {
                    projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ)
                }
                .mapNotNull {
                    if (page == null || size == null) {
                        testExecutionService.getAllTestExecutions(executionId).groupBy { it.test.testSuite.name }.map { (testSuiteName, testExecutions) ->
                            TestSuiteExecutionStatisticDto(
                                testSuiteName,
                                testExecutions.count(),
                                testExecutions.count { it.status == status },
                                status
                            )
                        }
                    } else {
                        testExecutionService.getByExecutionIdGroupByTestSuite(executionId, status, page, size)?.map {
                            TestSuiteExecutionStatisticDto(
                                it[0] as String,
                                (it[1] as BigInteger).toInt(),
                                (it[2] as BigInteger).toInt(),
                                TestResultStatus.valueOf(it[3] as String)
                            )
                        }
                    }
                }

    /**
     * Finds TestExecution by test location, returns 404 if not found
     *
     * @param executionId under this executionId test has been executed
     * @param testResultLocation location of the test
     * @param authentication
     * @return TestExecution
     */
    @PostMapping(path = ["/api/${com.saveourtool.common.v1}/test-execution"])
    @RequiresAuthorizationSourceHeader
    fun getTestExecutionByLocation(@RequestParam executionId: Long,
                                   @RequestBody testResultLocation: TestResultLocation,
                                   authentication: Authentication,
    ): Mono<TestExecutionDto> = executionService.findExecution(executionId)
        .toMonoOrNotFound()
        .filterWhen {
            projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ)
        }
        .map {
            testExecutionService.getTestExecution(executionId, testResultLocation)
                ?.toDto()
                .orNotFound {
                    "Test execution not found for executionId=$executionId and $testResultLocation"
                }
        }

    /**
     * Returns number of TestExecutions with this [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param status
     * @param testSuite
     * @param authentication
     */
    @GetMapping(path = ["/api/$v1/testExecution/count"])
    @RequiresAuthorizationSourceHeader
    fun getTestExecutionsCount(
        @RequestParam executionId: Long,
        @RequestParam(required = false) status: TestResultStatus?,
        @RequestParam(required = false) testSuite: String?,
        authentication: Authentication,
    ) =
            executionService.findExecution(executionId)
                .toMonoOrNotFound()
                .filterWhen {
                    projectPermissionEvaluator.checkPermissions(authentication, it, Permission.READ)
                }
                .map {
                    testExecutionService.getTestExecutionsCount(executionId, status, testSuite)
                }

    /**
     * @param containerId id of agent's container
     * @param status status for test executions
     * @return a list of test executions
     */
    @GetMapping("/internal/test-executions/get-by-container-id")
    fun getTestExecutionsForAgentWithStatus(@RequestParam containerId: String,
                                            @RequestParam status: TestResultStatus
    ) = testExecutionService.getAllTestExecutions(containerId, status)
        .map { it.toDto() }

    /**
     * @param executionId
     */
    @PostMapping("/internal/test-executions/mark-all-as-failed-by-execution-id")
    fun markAllTestExecutionsOfExecutionAsFailed(
        @RequestParam executionId: Long,
    ) = testExecutionService.markAllTestExecutionsOfExecutionAsFailed(executionId)

    /**
     * @param containerId
     */
    @PostMapping("/internal/test-executions/mark-ready-for-testing-as-failed-by-container-id")
    fun markReadyForTestingTestExecutionsOfAgentAsFailed(
        @RequestBody containerId: String,
    ) = testExecutionService.markReadyForTestingTestExecutionsOfAgentAsFailed(containerId)

    /**
     * @param testExecutionResults
     * @return response
     */
    @PostMapping(value = ["/internal/saveTestResult"])
    fun saveTestResult(@RequestBody testExecutionResults: List<TestExecutionResult>): ResponseEntity<String> = try {
        if (testExecutionResults.isEmpty()) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Empty result cannot be saved")
        } else if (testExecutionService.saveTestResult(testExecutionResults).isEmpty()) {
            ResponseEntity.status(HttpStatus.OK).body("Saved")
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Some ids don't exist or cannot be updated")
        }
    } catch (exception: DataAccessException) {
        log.warn("Unable to save test results", exception)
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error to save")
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestExecutionController::class.java)
    }
}
