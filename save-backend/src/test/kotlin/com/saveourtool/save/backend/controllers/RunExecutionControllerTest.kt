package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.common.domain.Jdk
import com.saveourtool.common.request.CreateExecutionRequest
import com.saveourtool.common.execution.TestingType
import com.saveourtool.common.repository.ProjectRepository
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.testutils.enqueue
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.v1
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest(classes = [SaveApplication::class])
@EnableConfigurationProperties(ConfigProperties::class)
@AutoConfigureWebTestClient
@ExtendWith(InfraExtension::class)
@Suppress("TOO_LONG_FUNCTION")
class RunExecutionControllerTest(
    @Autowired private var webClient: WebTestClient,
) {
    @Autowired private lateinit var projectRepository: ProjectRepository
    @Autowired private lateinit var executionRepository: ExecutionRepository
    @Autowired private lateinit var testRepository: TestRepository
    @Autowired private lateinit var testExecutionRepository: TestExecutionRepository
    @Autowired private lateinit var lnkExecutionFileRepository: LnkExecutionFileRepository
    @Autowired private lateinit var lnkExecutionTestSuiteRepository: LnkExecutionTestSuiteRepository

    @Suppress(
        "TOO_LONG_FUNCTION",
        "LongMethod"
    )
    @WithMockUser("admin")
    @Test
    fun trigger() {
        mutateMockedUser(id = 1)
        val project = projectRepository.findById(PROJECT_ID).get()
        val testSuiteIds = listOf(2L, 3L)
        val request = CreateExecutionRequest(
            projectCoordinates = project.toProjectCoordinates(),
            testSuiteIds = testSuiteIds,
            testsVersion = "main",
            fileIds = listOf(FILE_ID),
            sdk = Jdk("8"),
            execCmd = "execCmd",
            batchSizeForAnalyzer = "batchSizeForAnalyzer",
            testingType = TestingType.PUBLIC_TESTS,
        )

        // /initializeAgents
        mockServerOrchestrator.enqueue(
            "/initializeAgents",
            MockResponse()
                .setResponseCode(200)
        )
        val assertions = sequence {
            yield(mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS))
        }.onEach {
            log.info("Request $it")
        }

        val executionId = webClient.post()
            .uri("/api/${v1}/run/trigger?testingType={testingType}", TestingType.PRIVATE_TESTS.name)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody(String::class.java)
            .consumeWith { result ->
                val body = result.responseBody
                Assertions.assertTrue(body?.startsWith(RunExecutionController.RESPONSE_BODY_PREFIX) == true)
            }
            .returnResult()
            .responseBody
            .toString()
            .removePrefix(RunExecutionController.RESPONSE_BODY_PREFIX)
            .toLong()
        Thread.sleep(2_500)  // Time for request to create required entities

        assertions.forEach { Assertions.assertNotNull(it) }
        val testsCount = testRepository.findAll()
            .count { it.testSuite.requiredId() in testSuiteIds }
            .toLong()
        val newExecution = executionRepository.findById(executionId).get()
        Assertions.assertEquals(project, newExecution.project)
        Assertions.assertEquals("admin", newExecution.user?.name)
        Assertions.assertEquals(testsCount, newExecution.allTests)
        Assertions.assertEquals("eclipse-temurin:8", newExecution.sdk)
        Assertions.assertEquals("execCmd", newExecution.execCmd)
        Assertions.assertEquals("batchSizeForAnalyzer", newExecution.batchSizeForAnalyzer)

        val newTestExecutionsCount = testExecutionRepository.findAll()
            .count { it.execution.requiredId() == executionId }
            .toLong()
        Assertions.assertEquals(testsCount, newTestExecutionsCount)

        Assertions.assertEquals(
            testSuiteIds,
            lnkExecutionTestSuiteRepository.findByExecutionId(executionId)
                .map { it.testSuite.requiredId() }
        )
        Assertions.assertEquals(
            listOf(FILE_ID),
            lnkExecutionFileRepository.findAllByExecutionId(executionId)
                .map { it.file.requiredId() }
        )
    }

    @WithMockUser("admin")
    @Test
    fun reTrigger() {
        mutateMockedUser(id = 1)

        // /initializeAgents
        mockServerOrchestrator.enqueue(
            "/initializeAgents",
            MockResponse()
                .setResponseCode(200)
        )
        val assertions = sequence {
            yield(mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS))
        }.onEach {
            log.info("Request $it")
        }

        val testSuiteId = 11L
        val originalExecution = executionRepository.findById(EXECUTION_ID).get()
        val executionId = webClient.post()
            .uri("/api/${v1}/run/re-trigger?executionId=$EXECUTION_ID")
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody(String::class.java)
            .consumeWith { result ->
                val body = result.responseBody
                Assertions.assertTrue(body?.startsWith(RunExecutionController.RESPONSE_BODY_PREFIX) == true)
            }
            .returnResult()
            .responseBody
            .toString()
            .removePrefix(RunExecutionController.RESPONSE_BODY_PREFIX)
            .toLong()
        Thread.sleep(2_500)  // Time for request to create required entities

        assertions.forEach { Assertions.assertNotNull(it) }
        val testsCount = testRepository.findAll()
            .count { it.testSuite.requiredId() == testSuiteId }
            .toLong()
        val newExecution = executionRepository.findById(executionId).get()
        Assertions.assertEquals(originalExecution.project, newExecution.project)
        Assertions.assertEquals("admin", newExecution.user?.name)
        Assertions.assertEquals(originalExecution.allTests, newExecution.allTests)
        Assertions.assertEquals(originalExecution.sdk, newExecution.sdk)
        Assertions.assertEquals(originalExecution.execCmd, newExecution.execCmd)
        Assertions.assertEquals(originalExecution.batchSizeForAnalyzer, newExecution.batchSizeForAnalyzer)

        val newTestExecutionsCount = testExecutionRepository.findAll()
            .count { it.execution.requiredId() == executionId }
            .toLong()
        Assertions.assertEquals(testsCount, newTestExecutionsCount)

        Assertions.assertEquals(
            listOf(FILE_ID),
            lnkExecutionFileRepository.findAllByExecutionId(executionId)
                .map { it.file.requiredId() }
        )

        Assertions.assertEquals(
            listOf(testSuiteId),
            lnkExecutionTestSuiteRepository.findByExecutionId(executionId)
                .map { it.testSuite.requiredId() }
        )
    }

    companion object {
        private val log: Logger = getLogger<RunExecutionControllerTest>()
        private const val EXECUTION_ID = 6L
        private const val PROJECT_ID = 1L
        private const val FILE_ID = 1L

        @JvmStatic
        lateinit var mockServerOrchestrator: MockWebServer

        @AfterEach
        fun cleanup() {
            mockServerOrchestrator.checkQueues()
            mockServerOrchestrator.cleanup()
        }

        @AfterAll
        fun tearDown() {
            mockServerOrchestrator.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerOrchestrator = createMockWebServer()
            mockServerOrchestrator.start()
            registry.add("backend.orchestratorUrl") { "http://localhost:${mockServerOrchestrator.port}" }
        }
    }

    @TestConfiguration
    class AdditionalConfiguration {
        @Bean fun meterRegistry() = CompositeMeterRegistry()
    }
}
