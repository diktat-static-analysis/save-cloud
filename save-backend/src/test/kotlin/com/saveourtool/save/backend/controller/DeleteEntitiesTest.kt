package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.backend.repository.ExecutionRepository
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.save.backend.utils.postJsonAndAssert
import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.Organization
import com.saveourtool.common.entities.OrganizationStatus
import com.saveourtool.common.entities.Project
import com.saveourtool.common.repository.OrganizationRepository
import com.saveourtool.common.repository.ProjectRepository
import com.saveourtool.common.security.ProjectPermissionEvaluator
import com.saveourtool.common.utils.DATABASE_DELIMITER
import com.saveourtool.common.v1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import reactor.core.publisher.Mono
import java.util.Optional

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient(timeout = "60000")
@ExtendWith(InfraExtension::class)
@MockBeans(
)
@Suppress("UnusedPrivateProperty")
class DeleteEntitiesTest {
    @Autowired
    lateinit var webClient: WebTestClient
    @MockBean private lateinit var testExecutionRepository: TestExecutionRepository
    @MockBean private lateinit var agentStatusRepository: AgentStatusRepository
    @MockBean private lateinit var agentRepository: AgentRepository
    @MockBean private lateinit var executionRepository: ExecutionRepository
    @MockBean private lateinit var projectRepository: ProjectRepository
    @MockBean private lateinit var organizationRepository: OrganizationRepository
    @MockBean private lateinit var projectPermissionEvaluator: ProjectPermissionEvaluator

    @BeforeEach
    fun setUp() {
        // fixme: don't stub repositories and roll back transaction after the test (or make separate test for deletion logic in data layer)
        doNothing().whenever(testExecutionRepository).delete(any())
        doNothing().whenever(agentStatusRepository).delete(any())
        doNothing().whenever(agentRepository).delete(any())
        doNothing().whenever(executionRepository).delete(any())
        whenever(executionRepository.findById(any())).thenAnswer {
            Optional.of(Execution.stub(Project.stub(1)).apply { id = it.arguments[0] as Long })
        }
        whenever(projectRepository.findByNameAndOrganizationName(any(), any())).thenReturn(
            Project.stub(99).apply { id = 1 }
        )
        whenever(projectRepository.findByNameAndOrganizationNameAndStatusIn(any(), any(), any())).thenReturn(
            Project.stub(99).apply { id = 1 }
        )
        whenever(organizationRepository.findByName(any())).thenReturn(
            Organization.stub(null)
        )
        whenever(organizationRepository.findByNameAndStatusIn(any(), any())).thenReturn(
            Organization.stub(null).copy(status = OrganizationStatus.CREATED)
        )
        with(projectPermissionEvaluator) {
            whenever(any<Mono<Project?>>().filterByPermission(any(), any(), any())).thenCallRealMethod()
        }
        whenever(projectPermissionEvaluator.checkPermissions(any(), any(), any())).thenCallRealMethod()
        whenever(projectPermissionEvaluator.hasPermission(any(), any(), any())).thenAnswer {
            val authentication = it.arguments[0] as UsernamePasswordAuthenticationToken
            return@thenAnswer authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))
        }
    }

    @Test
    @WithMockUser
    fun `should forbid deletion by ID for ordinary user`() {
        val ids = listOf(1L, 2L, 3L)
        deleteExecutionsAndAssert(ids) {
            expectStatus().isForbidden
        }
    }

    @Test
    @WithMockUser
    fun `should forbid deletion by ID for ordinary user on a private project`() {
        Mockito.reset(projectRepository, executionRepository)
        val privateProject = Project.stub(99).apply {
            id = 1
            public = false
        }
        whenever(projectRepository.findByNameAndOrganization(any(), any())).thenReturn(
            privateProject
        )
        whenever(executionRepository.findById(any())).thenAnswer {
            Optional.of(Execution.stub(privateProject).apply { id = it.arguments[0] as Long })
        }
        val ids = listOf(1L, 2L, 3L)

        deleteExecutionsAndAssert(ids) {
            expectStatus().isNotFound
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should delete by ID for project admin`() {
        val ids = listOf(1L, 2L, 3L)
        deleteExecutionsAndAssert(ids) {
            expectStatus().isOk
        }
    }

    @Test
    @WithMockUser
    fun `should forbid deletion of all executions for ordinary user`() {
        deleteAllExecutionsAndAssert("huaweiName", "Huawei") {
            expectStatus().isForbidden
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should delete all executions for project admin`() {
        deleteAllExecutionsAndAssert("huaweiName", "Huawei") {
            expectStatus().isOk
        }
    }

    private fun deleteExecutionsAndAssert(executionIds: List<Long>, assert: ResponseSpec.() -> Unit) {
        webClient.postJsonAndAssert(
            uri = "/api/${v1}/execution/delete?executionIds=${executionIds.joinToString(DATABASE_DELIMITER)}",
            assert = assert
        )
    }

    private fun deleteAllExecutionsAndAssert(name: String, organizationName: String, assert: ResponseSpec.() -> Unit) {
        webClient.postJsonAndAssert(
            uri = "/api/${v1}/execution/delete-all-except-contest?name=$name&organizationName=$organizationName",
            assert = assert
        )
    }
}
