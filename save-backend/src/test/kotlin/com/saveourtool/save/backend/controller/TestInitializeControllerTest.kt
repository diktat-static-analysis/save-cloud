package com.saveourtool.save.backend.controller

import com.saveourtool.common.agent.AgentRunConfig
import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.TestRepository
import com.saveourtool.save.backend.repository.TestSuiteRepository
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.common.test.TestDto
import com.saveourtool.common.utils.debug
import com.saveourtool.common.utils.getLogger
import org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(InfraExtension::class)
@MockBeans(
    MockBean(ProjectController::class),
)
class TestInitializeControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var testRepository: TestRepository

    @Autowired
    lateinit var testSuiteRepository: TestSuiteRepository

    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun testConnection() {
        val testSuite = testSuiteRepository.findById(2).get()
        val test = TestDto(
            "testPath",
            "WarnPlugin",
            testSuite.id!!,
            "newHash",
            listOf("tag"),
        )

        webClient.post()
            .uri("/internal/initializeTests?executionId=2")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(test)))
            .exchange()
            .expectStatus()
            .isOk

        assertNotNull(testRepository.findAllByTestSuiteId(2))
        assertNotNull(testRepository.findByHashAndFilePathAndTestSuiteIdAndPluginName("newHash", "testPath", 2, "WarnPlugin"))
    }

    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun checkDataSave() {
        val testSuite = testSuiteRepository.findById(2).get()
        val test = TestDto(
            "testPath",
            "WarnPlugin",
            testSuite.id!!,
            "newHash2",
            listOf("tag"),
        )
        webClient.post()
            .uri("/internal/initializeTests?executionId=2")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(test)))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = testRepository.findAll()

        assertTrue(databaseData.any { it.testSuite.id == test.testSuiteId && it.filePath == test.filePath && it.hash == test.hash })
    }

    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun checkInitializeWithoutExecution() {
        val testSuite = testSuiteRepository.findById(2).get()
        val test = TestDto(
            "testWithoutExecution",
            "WarnPlugin",
            testSuite.id!!,
            "newHash123",
            listOf("tag"),
        )
        webClient.post()
            .uri("/internal/initializeTests")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(test)))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = testRepository.findAll()

        assertTrue(databaseData.any { it.testSuite.id == test.testSuiteId && it.filePath == test.filePath && it.hash == test.hash })
    }

    @Test
    fun `should return test executions in batches`() {
        webClient.get()
            .uri("/internal/agents/get-next-run-config?containerId=container-4")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<com.saveourtool.common.agent.AgentRunConfig>()
            .consumeWith { entityExchangeResult ->
                val batch = entityExchangeResult.responseBody!!
                log.debug { batch.toString() }
                assertTrue(batch.cliArgs.isNotEmpty())
                assertEquals(10, batch.cliArgs.split(" ").size)
            }

        webClient.get()
            .uri("/internal/agents/get-next-run-config?containerId=container-4")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<com.saveourtool.common.agent.AgentRunConfig>()
            .consumeWith { entityExchangeResult ->
                val body = entityExchangeResult.responseBody!!
                assertTrue(body.cliArgs.split(" ").size == 3) { "Expected 3 tests, but got $body instead" }
            }
    }

    companion object {
        private val log: Logger = getLogger<TestInitializeControllerTest>()
    }
}
