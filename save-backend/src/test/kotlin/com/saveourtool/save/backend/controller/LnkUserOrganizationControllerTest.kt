package com.saveourtool.save.backend.controller

import com.saveourtool.save.authservice.config.WebSecurityConfig
import com.saveourtool.save.backend.controllers.LnkUserOrganizationController
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.*
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.permission.SetRoleRequest
import com.saveourtool.common.repository.OriginalLoginRepository
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.security.OrganizationPermissionEvaluator
import com.saveourtool.common.service.LnkUserOrganizationService
import com.saveourtool.common.service.OrganizationService
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

@WebFluxTest(controllers = [LnkUserOrganizationController::class])
@Import(
    WebSecurityConfig::class,
    OrganizationService::class,
)
@MockBeans(
    MockBean(OriginalLoginRepository::class),
    MockBean(NamedParameterJdbcTemplate::class),
    MockBean(BlockingBridge::class),
)
@AutoConfigureWebTestClient
class LnkUserOrganizationControllerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var lnkUserOrganizationService: LnkUserOrganizationService

    @MockBean
    private lateinit var organizationPermissionEvaluator: OrganizationPermissionEvaluator

    @MockBean
    private lateinit var userRepository: UserRepository

    @MockBean
    private lateinit var organizationService: OrganizationService

    @Test
    @WithMockUser
    fun `should allow changing roles for organization owners`() {
        mutateMockedUser(id = 99)
        given(userRepository.findByName(any())).willReturn(
            User("user", null, null, "").apply { id = 99 }
        )
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            organization = Organization.stub(id = 99),
            organizationRole = Role.OWNER,
        )
        given(organizationPermissionEvaluator.canChangeRoles(any(), any(), any(), any())).willReturn(true)
        webTestClient.post()
            .uri("/api/${v1}/organizations/Huawei/users/roles")
            .bodyValue(SetRoleRequest("admin", Role.ADMIN))
            .exchange()
            .expectStatus()
            .isOk
        verify(lnkUserOrganizationService, times(1)).setRole(any(), any(), any())
    }

    @Test
    @WithMockUser
    fun `should forbid changing roles unless user is an organization owner`() {
        mutateMockedUser(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            organization = Organization.stub(id = 99),
            organizationRole = Role.ADMIN,
        )

        webTestClient.post()
            .uri("/api/${v1}/organizations/Huawei/users/roles")
            .bodyValue(SetRoleRequest("admin", Role.ADMIN))
            .exchange()
            .expectStatus()
            .isForbidden
        verify(lnkUserOrganizationService, times(0)).setRole(any(), any(), any())
    }

    @Test
    @WithMockUser
    fun `should get 403 when deleting users from organization without permission`() {
        mutateMockedUser(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            organization = Organization.stub(id = 99),
            organizationRole = Role.VIEWER,
        )
        webTestClient.delete()
            .uri("/api/${v1}/organizations/Huawei/users/roles/user")
            .exchange()
            .expectStatus()
            .isForbidden
        verify(lnkUserOrganizationService, times(0)).removeRole(any(), any())
    }

    @Test
    @WithMockUser
    fun `should permit deleting users from organization if user is admin or higher`() {
        mutateMockedUser(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            organization = Organization.stub(99),
            organizationRole = Role.ADMIN,
        )

        given(organizationPermissionEvaluator.canChangeRoles(any(), any(), any(), any())).willReturn(true)
        webTestClient.delete()
            .uri("/api/${v1}/organizations/Huawei/users/roles/user")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @WithMockUser
    fun `should forbid removing people from organization if user has less permissions than admin`() {
        mutateMockedUser(id = 99)
        given(
            user = { User(name = it.arguments[0] as String, null, null, "") },
            organization = Organization.stub(id = 99),
            organizationRole = Role.VIEWER,
        )
        given(organizationPermissionEvaluator.canChangeRoles(any(), any(), any(), any())).willReturn(false)
        webTestClient.delete()
            .uri("/api/${com.saveourtool.common.v1}/organizations/Huawei/users/roles/user")
            .exchange()
            .expectStatus()
            .isForbidden
        verify(lnkUserOrganizationService, times(0)).removeRole(any(), any())
    }

    @Suppress("LAMBDA_IS_NOT_LAST_PARAMETER")
    private fun given(
        user: (InvocationOnMock) -> User,
        organization: Organization,
        organizationRole: Role,
    ) {
        given(organizationService.findByNameAndCreatedStatus(any())).willReturn(organization)
        given(organizationPermissionEvaluator.hasPermission(any(), any(), any())).willAnswer {
            when (it.arguments[2] as Permission?) {
                null -> false
                Permission.READ -> organizationRole.priority >= Role.VIEWER.priority
                Permission.WRITE -> organizationRole.priority >= Role.ADMIN.priority
                Permission.DELETE -> organizationRole.priority >= Role.OWNER.priority
                Permission.BAN -> organizationRole.priority== Role.SUPER_ADMIN.priority
            }
        }
        given(lnkUserOrganizationService.getUserByName(any())).willAnswer { invocationOnMock ->
            user(invocationOnMock)
        }
    }
}
