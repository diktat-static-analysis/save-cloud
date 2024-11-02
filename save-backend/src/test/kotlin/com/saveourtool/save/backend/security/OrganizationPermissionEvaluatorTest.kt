package com.saveourtool.save.backend.security

import com.saveourtool.save.authservice.utils.SaveUserDetails
import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.*
import com.saveourtool.common.info.UserStatus
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.repository.LnkUserOrganizationRepository
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.security.OrganizationPermissionEvaluator
import com.saveourtool.common.service.LnkUserOrganizationService
import com.saveourtool.common.service.UserService

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class, MockitoExtension::class)
@Import(OrganizationPermissionEvaluator::class, LnkUserOrganizationService::class)
@MockBeans(
    MockBean(UserRepository::class),
)
class OrganizationPermissionEvaluatorTest {
    @Autowired private lateinit var organizationPermissionEvaluator: OrganizationPermissionEvaluator
    @MockBean private lateinit var lnkUserOrganizationRepository: LnkUserOrganizationRepository
    @MockBean private lateinit var userDetailsService: UserService
    private lateinit var mockOrganization: Organization

    private val ownerPermissions = Permission.values().filterNot { it == Permission.BAN }.toTypedArray()

    @BeforeEach
    fun setUp() {
        mockOrganization = Organization.stub(99)
    }

    @Test
    fun `permissions for organization owners`() {
        userShouldHavePermissions(
            "super_admin", Role.SUPER_ADMIN, Role.OWNER,  *Permission.values(), userId = 99
        )
        userShouldHavePermissions(
            "admin", Role.ADMIN, Role.OWNER, *ownerPermissions, userId = 99
        )
        userShouldHavePermissions(
            "owner", Role.OWNER, Role.OWNER, *ownerPermissions, userId = 99
        )
        userShouldHavePermissions(
            "viewer", Role.VIEWER, Role.OWNER, *ownerPermissions, userId = 99
        )
    }

    @Test
    fun `permissions for organization admins`() {
        userShouldHavePermissions(
            "super_admin", Role.SUPER_ADMIN, Role.ADMIN, *Permission.values(), userId = 99
        )
        userShouldHavePermissions(
            "admin", Role.ADMIN, Role.ADMIN, Permission.READ, Permission.WRITE, userId = 99
        )
        userShouldHavePermissions(
            "owner", Role.OWNER, Role.ADMIN, Permission.READ, Permission.WRITE, userId = 99
        )
        userShouldHavePermissions(
            "viewer", Role.VIEWER, Role.ADMIN, Permission.READ, Permission.WRITE, userId = 99
        )
    }

    @Test
    fun `permissions for organization viewers`() {
        userShouldHavePermissions(
            "super_admin", Role.SUPER_ADMIN, Role.VIEWER, *Permission.values(), userId = 99
        )
        userShouldHavePermissions(
            "admin", Role.ADMIN, Role.VIEWER, Permission.READ, userId = 99
        )
        userShouldHavePermissions(
            "owner", Role.OWNER, Role.VIEWER, Permission.READ, userId = 99
        )
        userShouldHavePermissions(
            "viewer", Role.VIEWER, Role.VIEWER, Permission.READ, userId = 99
        )
    }

    private fun userShouldHavePermissions(
        username: String,
        role: Role,
        organizationRole: Role,
        vararg permissions: Permission,
        userId: Long = 1
    ) {
        val authentication = mockAuth(username, role.asSpringSecurityRole(), id = userId)
        given(userDetailsService.getGlobalRole(any())).willReturn(Role.VIEWER)
        given(userDetailsService.getUserByName(any())).willReturn(mockUser(userId))
        whenever(lnkUserOrganizationRepository.findByUserIdAndOrganization(any(), any())).thenAnswer { invocation ->
            LnkUserOrganization(
                invocation.arguments[1] as Organization,
                mockUser((invocation.arguments[0] as Number).toLong()),
                organizationRole,
            )
        }
        permissions.forEach { permission ->
            Assertions.assertTrue(organizationPermissionEvaluator.hasPermission(authentication, mockOrganization, permission)) {
                "User by authentication=$authentication is expected to have permission $permission on organization $mockOrganization"
            }
        }
        Permission.values().filterNot { it in permissions }.forEach { permission ->
            Assertions.assertFalse(organizationPermissionEvaluator.hasPermission(authentication, mockOrganization, permission)) {
                "User by authentication=$authentication isn't expected to have permission $permission on organization $mockOrganization"
            }
        }
    }

    private fun mockAuth(
        principal: String,
        vararg roles: String,
        id: Long = 99,
        status: UserStatus = UserStatus.ACTIVE,
    ) = SaveUserDetails(
        id = id,
        name = principal,
        role = roles.joinToString(","),
        status = status.toString(),
        token = null,
    ).toPreAuthenticatedAuthenticationToken()

    private fun mockUser(id: Long) = User("mocked", null, null, "").apply { this.id = id }
}
