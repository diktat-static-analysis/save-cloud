package org.cqfn.save.backend.controller

import org.cqfn.save.agent.AgentState
import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.AgentStatus
import org.cqfn.save.entities.AgentStatusDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime
import java.time.Month
import javax.persistence.EntityManager

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
class AgentsControllerTest {
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var agentStatusRepository: AgentStatusRepository

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
            .uri("/updateAgentStatusesWithDto")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                listOf(
                    AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "container-1")
                )
            ))
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `check that agent statuses are updated`() {
        webTestClient
            .method(HttpMethod.POST)
            .uri("/updateAgentStatusesWithDto")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                listOf(
                    AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "container-2")
                )
            ))
            .exchange()
            .expectStatus()
            .isOk

        val firstAgentIdle = getLastIdleForSecondContainer()

        webTestClient
            .method(HttpMethod.POST)
            .uri("/updateAgentStatusesWithDto")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                listOf(
                    AgentStatusDto(LocalDateTime.of(2022, Month.MAY, 10, 16, 30, 20), AgentState.IDLE, "container-2")
                )
            ))
            .exchange()
            .expectStatus()
            .isOk

        webTestClient
            .method(HttpMethod.POST)
            .uri("/updateAgentStatusesWithDto")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                listOf(
                    AgentStatusDto(LocalDateTime.now(), AgentState.BUSY, "container-2")
                )
            ))
            .exchange()
            .expectStatus()
            .isOk

        assertTrue(
            transactionTemplate.execute {
                agentStatusRepository
                    .findAll()
                    .filter { it.state == AgentState.IDLE && it.agent.containerId == "container-2" }
                    .size == 1
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
            .uri("/getAgentsStatusesForSameExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue("container-1")
            .exchange()
            .expectBody<List<AgentStatusDto?>>()
            .consumeWith {
                val statuses = it.responseBody
                requireNotNull(statuses)
                Assertions.assertEquals(2, statuses.size)
                Assertions.assertEquals(AgentState.IDLE, statuses.first()!!.state)
                Assertions.assertEquals(AgentState.IDLE, statuses[1]!!.state)
            }
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
