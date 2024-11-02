package com.saveourtool.save.backend.controller

import com.saveourtool.save.authservice.config.WebSecurityConfig
import com.saveourtool.save.backend.controllers.PermissionController
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.*
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.permission.SetRoleRequest
import com.saveourtool.common.repository.OrganizationRepository
import com.saveourtool.common.repository.OriginalLoginRepository
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.security.OrganizationPermissionEvaluator
import com.saveourtool.common.security.ProjectPermissionEvaluator
import com.saveourtool.common.service.LnkUserOrganizationService
import com.saveourtool.common.service.LnkUserProjectService
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.service.ProjectService
import com.saveourtool.common.utils.BlockingBridge
import com.saveourtool.common.v1
import org.junit.jupiter.api.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import reactor.util.function.Tuples

@WebFluxTest(controllers = [PermissionController::class])
@Import(
    WebSecurityConfig::class,
    OrganizationService::class,
    LnkUserProjectService::class,
    LnkUserOrganizationService::class,
)
@MockBeans(
    MockBean(LnkUserProjectService::class),
    MockBean(LnkUserOrganizationService::class),
    MockBean(OriginalLoginRepository::class),
    MockBean(NamedParameterJdbcTemplate::class),
    MockBean(BlockingBridge::class),
)
@AutoConfigureWebTestClient
class PermissionControllerTest {
    @Autowired private lateinit var webTestClient: WebTestClient
    @MockBean private lateinit var permissionService: PermissionService
    @MockBean private lateinit var organizationRepository: OrganizationRepository
    @MockBean private lateinit var projectPermissionEvaluator: ProjectPermissionEvaluator
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var projectService: ProjectService
    @MockBean private lateinit var organizationPermissionEvaluator: OrganizationPermissionEvaluator
    @MockBean private lateinit var organizationService: OrganizationService

    @Test
    @WithMockUser
    fun `should allow reading of roles if user has permission`() {
        mutateMockedUser(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = Permission.READ,
        )
        given(permissionService.getRole(any(), any())).willReturn(Role.ADMIN)

        webTestClient.get()
            .uri("/api/${v1}/projects/Huawei/huaweiName/users/roles?userName=admin")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<Role>()
            .isEqualTo(Role.ADMIN)
        verify(permissionService, times(1)).getRole(any(), any())
    }

    @Test
    @WithMockUser
    fun `should forbid reading of roles if user doesn't have permission`() {
        mutateMockedUser(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = null,
        )

        webTestClient.get()
            .uri("/api/${v1}/projects/Huawei/huaweiName/users/roles?userName=admin")
            .exchange()
            .expectStatus()
            .isNotFound
        verify(permissionService, times(0)).getRole(any(), any())
    }

    @Test
    @WithMockUser
    fun `should allow changing roles for organization owners`() {
        mutateMockedUser(id = 99)
        given(userRepository.findByName(any())).willReturn(
            User("user", null, null, "").apply { id = 99 }
        )
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = Permission.WRITE,
        )
        given(projectPermissionEvaluator.canChangeRoles(any(), any(), any(), any())).willReturn(true)
        given(organizationRepository.findByName(any())).willReturn(Organization.stub(null).apply { name  = "Example Org" })
        given(permissionService.setRole(any(), any(), any())).willReturn(Mono.just(Unit))

        webTestClient.post()
            .uri("/api/${com.saveourtool.common.v1}/projects/Huawei/huaweiName/users/roles")
            .bodyValue(SetRoleRequest("admin", Role.ADMIN))
            .exchange()
            .expectStatus()
            .isOk
        verify(permissionService, times(1)).setRole(any(), any(), any())
    }

    @Test
    @WithMockUser
    fun `should forbid changing roles unless user is an organization owner`() {
        mutateMockedUser(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = Permission.WRITE,
        )
        given(organizationRepository.findByName(any())).willReturn(Organization.stub(null).apply { name = "Example Org" })

        webTestClient.post()
            .uri("/api/${v1}/projects/Huawei/huaweiName/users/roles")
            .bodyValue(SetRoleRequest("admin", Role.ADMIN))
            .exchange()
            .expectStatus()
            .isForbidden
        verify(permissionService, times(0)).setRole(any(), any(), any())
    }

    @Test
    @WithMockUser
    fun `should return 404 when changing roles on hidden project`() {
        mutateMockedUser(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99).apply { public = false },
            permission = null,
        )
        given(organizationRepository.findByName(any())).willReturn(Organization.stub(null).apply { name = "Example Org" })

        webTestClient.post()
            .uri("/api/${v1}/projects/Huawei/huaweiName/users/roles")
            .bodyValue(SetRoleRequest("admin", Role.ADMIN))
            .exchange()
            .expectStatus()
            .isNotFound
        verify(permissionService, times(0)).setRole(any(), any(), any())
    }

    @Test
    @WithMockUser
    fun `should get 404 when deleting users from project without permission`() {
        mutateMockedUser(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99).apply { public = true },
            permission = null,
        )
        webTestClient.delete()
            .uri("/api/${v1}/projects/Huawei/huaweiName/users/roles/user")
            .exchange()
            .expectStatus()
            .isNotFound
        verify(permissionService, times(0)).removeRole(any(), any(), any())
    }

    @Test
    @WithMockUser
    fun `should permit deleting users from project if user can change roles in project`() {
        mutateMockedUser(id = 99)
        val project = Project.stub(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = project,
            permission = Permission.WRITE,
        )

        given(projectPermissionEvaluator.canChangeRoles(any(), any(), any(), any())).willReturn(true)
        given(permissionService.removeRole(any(), any(), any())).willReturn(Mono.just(Unit))
        webTestClient.delete()
            .uri("/api/${v1}/projects/Huawei/huaweiName/users/roles/user")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @WithMockUser
    fun `should forbid removing people from project if user cannot change roles in project`() {
        mutateMockedUser(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            project = Project.stub(id = 99),
            permission = Permission.WRITE,
        )
        given(projectPermissionEvaluator.canChangeRoles(any(), any(), any(), any())).willReturn(false)
        given(organizationPermissionEvaluator.canChangeRoles(any(), any(), any(), any())).willReturn(false)
        given(permissionService.removeRole(any(), any(), any())).willReturn(Mono.just(Unit))
        webTestClient.delete()
            .uri("/api/${v1}/projects/Huawei/huaweiName/users/roles/user")
            .exchange()
            .expectStatus()
            .isForbidden
        verify(permissionService, times(0)).removeRole(any(), any(), any())
    }

    @Suppress("LAMBDA_IS_NOT_LAST_PARAMETER")
    private fun given(
        user: (InvocationOnMock) -> User,
        project: Project,
        permission: Permission?,
    ) {
        given(permissionService.findUserAndProject(any(), any(), any())).willAnswer { invocationOnMock ->
            Tuples.of(user(invocationOnMock), project).let { Mono.just(it) }
        }
        given(organizationService.findByNameAndCreatedStatus(any())).willReturn(project.organization)
        given(organizationService.findByNameAndStatuses(any(), any())).willReturn(project.organization)
        given(projectService.findByNameAndOrganizationNameAndCreatedStatus(any(), any())).willReturn(project)
        given(projectService.findByNameAndOrganizationNameAndStatusIn(any(), any(), any())).willReturn(project)
        given(projectPermissionEvaluator.hasPermission(any(), any(), any())).willAnswer {
            when (it.arguments[2] as Permission?) {
                null -> false
                Permission.READ -> permission != null
                Permission.WRITE -> permission == Permission.WRITE || permission == Permission.DELETE || permission == Permission.BAN
                Permission.DELETE -> permission == Permission.DELETE || permission == Permission.BAN
                Permission.BAN -> permission == Permission.BAN
            }
        }
        given(projectService.findUserByName(any())).willAnswer { invocationOnMock ->
            user(invocationOnMock)
        }
    }
}
