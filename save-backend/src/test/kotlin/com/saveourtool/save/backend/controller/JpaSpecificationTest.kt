package com.saveourtool.save.backend.controller

import com.saveourtool.common.agent.AgentState
import com.saveourtool.save.backend.configs.ApplicationConfiguration
import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.common.utils.BlockingBridge
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import

@Import(ApplicationConfiguration::class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(InfraExtension::class)
@MockBeans(
    MockBean(BlockingBridge::class),
)
class JpaSpecificationTest {
    @Autowired
    lateinit var agentStatusRepository: AgentStatusRepository

    @Test
    fun testFindList() {
        val agentStatusToId = agentStatusRepository.findById(4).get()

        val agentStatusToList = agentStatusRepository.findOne { root, _, cb ->
            cb.and(
                cb.equal(root.get<Long>("id"), 4),
                cb.equal(root.get<AgentState>("state"), com.saveourtool.common.agent.AgentState.FINISHED)
            )
        }.get()

        Assertions.assertTrue(agentStatusToId.id == agentStatusToList.id)
    }
}
