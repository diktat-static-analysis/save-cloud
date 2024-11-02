package com.saveourtool.save.backend

import com.saveourtool.common.agent.AgentState
import com.saveourtool.save.backend.configs.ApplicationConfiguration
import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.repository.ProjectRepository
import com.saveourtool.common.utils.BlockingBridge
import org.junit.jupiter.api.Assertions.assertTrue
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
class DatabaseTest {
    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var testExecutionRepository: TestExecutionRepository

    @Autowired
    private lateinit var agentStatusRepository: AgentStatusRepository

    @Test
    fun checkProjectDataInDataBase() {
        val projects = projectRepository.findAll()

        assertTrue(projects.any { it.name == "huaweiName" && it.organization.name == "Huawei" && it.url == "https://huawei.com" })
    }

    @Test
    fun checkTestStatusDataInDataBase() {
        val tests = testExecutionRepository.findAll()

        assertTrue(tests.any { it.status == TestResultStatus.FAILED && it.agent?.id == 1L })
    }

    @Test
    fun checkAgentStatusDataInDataBase() {
        val agents = agentStatusRepository.findAll()

        assertTrue(agents.any { it.state == AgentState.BUSY && it.agent.id == 2L })
    }
}
