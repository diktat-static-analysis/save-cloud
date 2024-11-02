package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.service.LnkContestExecutionService
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.common.execution.ExecutionUpdateDto
import com.saveourtool.common.execution.TestingType
import com.saveourtool.common.repository.ProjectRepository
import com.saveourtool.common.utils.debug
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.v1

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(InfraExtension::class)
@MockBeans(
    MockBean(ProjectController::class),
    MockBean(LnkContestExecutionService::class),
)
class ExecutionControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var executionRepository: ExecutionRepository

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Test
    @WithMockUser("JohnDoe")
    @Suppress("TOO_LONG_FUNCTION")
    fun testUpdateExecution() {
        val executionUpdateDto = ExecutionUpdateDto(1, ExecutionStatus.FINISHED)

        webClient.post()
            .uri("/internal/updateExecutionByDto")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionUpdateDto))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = executionRepository.findAll()

        databaseData.forEach {
            log.debug { "${it.status}" }
        }

        assertTrue(databaseData.any { it.status == executionUpdateDto.status && it.id == executionUpdateDto.id })
    }

    @Test
    fun checkStatusException() {
        val executionUpdateDto = ExecutionUpdateDto(
            -1, ExecutionStatus.FINISHED
        )
        webClient.post()
            .uri("/internal/updateExecutionByDto")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionUpdateDto))
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    @WithMockUser
    fun checkExecutionDto() {
        mutateMockedUser(id = 99)

        webClient.get()
            .uri("/api/${v1}/executionDto?executionId=1")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ExecutionDto>()
            .consumeWith {
                requireNotNull(it.responseBody)
                assertEquals(TestingType.PRIVATE_TESTS, it.responseBody!!.type)
            }
    }

    @Test
    @WithMockUser
    fun checkExecutionDtoByProject() {
        mutateMockedUser(id = 99)

        val project = projectRepository.findById(1).get()
        val executionCounts = executionRepository.findAll().count { it.project.id == project.id }
        webClient.post()
            .uri("/api/${v1}/executionDtoList?projectName=${project.name}&organizationName=${project.organization.name}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<List<ExecutionDto>>()
            .consumeWith {
                requireNotNull(it.responseBody)
                assertEquals(executionCounts, it.responseBody!!.size)
            }
    }

    companion object {
        private val log: Logger = getLogger<ExecutionControllerTest>()
    }
}
