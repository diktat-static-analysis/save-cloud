package com.saveourtool.save.backend.controller

import com.saveourtool.common.agent.AgentState
import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.common.entities.AgentStatus
import com.saveourtool.common.entities.AgentStatusDto
import com.saveourtool.common.entities.AgentStatusDtoList
import com.saveourtool.common.security.ProjectPermissionEvaluator
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Month
import javax.persistence.EntityManager

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(InfraExtension::class)
@MockBeans(
    MockBean(ProjectController::class),
    MockBean(ProjectPermissionEvaluator::class),
)
class AgentsControllerTest {
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var agentStatusRepository: AgentStatusRepository

    @Autowired
    lateinit var agentRepository: AgentRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var entityManager: EntityManager
    private lateinit var transactionTemplate: TransactionTemplate

    @BeforeEach
    fun setUp() {
        transactionTemplate = TransactionTemplate(transactionManager)
    }

    @Test
    fun `should save agent statuses`() {
        webTestClient
            .method(HttpMethod.POST)
            .uri("/internal/updateAgentStatus")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(
                AgentStatusDto(AgentState.IDLE, "container-1")
            )
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `check that agent statuses are updated`() {
        updateAgentStatuses(
            AgentStatusDto(AgentState.IDLE, "container-2")
        )

        val firstAgentIdle = getLastIdleForSecondContainer()

        webTestClient
            .method(HttpMethod.POST)
            .uri("/internal/updateAgentStatus")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(
                AgentStatusDto(AgentState.IDLE, "container-2", LocalDateTime(2020, Month.MAY, 10, 16, 30, 20))
            )
            .exchange()
            .expectStatus()
            .isOk

        updateAgentStatuses(
            AgentStatusDto(AgentState.BUSY, "container-2")
        )

        assertTrue(
            transactionTemplate.execute {
                agentStatusRepository
                    .findAll()
                    .count { it.state == AgentState.IDLE && it.agent.containerId == "container-2" } == 1
            }!!
        )

        val lastUpdatedIdle = getLastIdleForSecondContainer()

        assertTrue(
            lastUpdatedIdle.startTime.withNano(0) == firstAgentIdle.startTime.withNano(0) &&
                    lastUpdatedIdle.endTime.withNano(0) != firstAgentIdle.endTime.withNano(0)
        )
    }

    @Test
    fun `should return latest status by container id`() {
        webTestClient
            .method(HttpMethod.GET)
            .uri("/internal/getAgentStatusesByExecutionId?executionId=1")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectBody<AgentStatusDtoList>()
            .consumeWith {
                val statuses = requireNotNull(it.responseBody)
                Assertions.assertEquals(2, statuses.size)
                Assertions.assertEquals(AgentState.IDLE, statuses.first().state)
                Assertions.assertEquals(AgentState.BUSY, statuses[1].state)
            }
    }

    @Test
    fun `should return latest statuses by a list of container id`() {
        webTestClient
            .method(HttpMethod.GET)
            .uri("/internal/agents/statuses?ids=container-1,container-2")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectBody<List<AgentStatusDto>>()
            .consumeWith {
                val statuses = requireNotNull(it.responseBody)
                Assertions.assertEquals(2, statuses.size)
                Assertions.assertEquals(AgentState.IDLE, statuses.first().state)
                Assertions.assertEquals(AgentState.BUSY, statuses[1].state)
            }
    }

    private fun updateAgentStatuses(body: AgentStatusDto) {
        webTestClient
            .method(HttpMethod.POST)
            .uri("/internal/updateAgentStatus")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk
    }

    private fun getLastIdleForSecondContainer() =
            transactionTemplate.execute {
                entityManager.createNativeQuery("select * from agent_status", AgentStatus::class.java)
                    .resultList
                    .first {
                        (it as AgentStatus).state == AgentState.IDLE && it.agent.containerId == "container-2"
                    }
                        as AgentStatus
            }!!
}
