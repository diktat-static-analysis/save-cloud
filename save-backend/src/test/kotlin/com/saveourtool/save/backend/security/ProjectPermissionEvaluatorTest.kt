package com.saveourtool.save.backend.security

import com.saveourtool.save.authservice.utils.SaveUserDetails
import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.LnkUserProject
import com.saveourtool.common.entities.Project
import com.saveourtool.common.entities.User
import com.saveourtool.common.info.UserStatus
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.repository.LnkUserProjectRepository
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.security.ProjectPermissionEvaluator
import com.saveourtool.common.service.LnkUserOrganizationService
import com.saveourtool.common.service.LnkUserProjectService
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
@Import(ProjectPermissionEvaluator::class, LnkUserProjectService::class, LnkUserOrganizationService::class)
@MockBeans(
    MockBean(UserRepository::class),
    MockBean(LnkUserOrganizationService::class),
)
class ProjectPermissionEvaluatorTest {
    @Autowired private lateinit var projectPermissionEvaluator: ProjectPermissionEvaluator
    @MockBean private lateinit var lnkUserProjectRepository: LnkUserProjectRepository
    @MockBean private lateinit var userDetailsService: UserService
    private lateinit var mockProject: Project

    private val ownerPermissions = Permission.values().filterNot { it == Permission.BAN }.toTypedArray()

    @BeforeEach
    fun setUp() {
        mockProject = Project.stub(99)
    }

    @Test
    fun `default permissions for users with only global roles`() {
        userShouldHavePermissions(
            "super_admin", Role.SUPER_ADMIN, Role.NONE, *Permission.values()
        )
        userShouldHavePermissions(
            "admin", Role.ADMIN, Role.NONE, Permission.READ
        )
        userShouldHavePermissions(
            "owner", Role.OWNER, Role.NONE, Permission.READ
        )
        userShouldHavePermissions(
            "viewer", Role.VIEWER, Role.NONE, Permission.READ
        )
    }

    @Test
    fun `default permissions for users with only global roles for private projects`() {
        mockProject.public = false
        userShouldHavePermissions(
            "super_admin", Role.SUPER_ADMIN, Role.NONE, *Permission.values()
        )
        userShouldHavePermissions(
            "admin", Role.ADMIN, Role.NONE
        )
        userShouldHavePermissions(
            "owner", Role.OWNER, Role.NONE
        )
        userShouldHavePermissions(
            "viewer", Role.VIEWER, Role.NONE
        )
    }

    @Test
    fun `permissions for project owners`() {
        userShouldHavePermissions(
            "super_admin", Role.SUPER_ADMIN, Role.OWNER, *Permission.values(), userId = 99
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
    fun `permissions for project admins`() {
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
    fun `permissions for project viewers`() {
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
        projectRole: Role,
        vararg permissions: Permission,
        userId: Long = 1
    ) {
        val authentication = mockAuth(username, role.asSpringSecurityRole(), id = userId)
        given(userDetailsService.getGlobalRole(any())).willReturn(Role.VIEWER)
        given(userDetailsService.getUserByName(any())).willReturn(mockUser(userId))
        whenever(lnkUserProjectRepository.findByUserIdAndProject(any(), any())).thenAnswer { invocation ->
            LnkUserProject(
                invocation.arguments[1] as Project,
                mockUser((invocation.arguments[0] as Number).toLong()),
                projectRole,
            )
        }
        permissions.forEach { permission ->
            Assertions.assertTrue(projectPermissionEvaluator.hasPermission(authentication, mockProject, permission)) {
                "User by authentication=$authentication is expected to have permission $permission on project $mockProject"
            }
        }
        Permission.values().filterNot { it in permissions }.forEach { permission ->
            Assertions.assertFalse(projectPermissionEvaluator.hasPermission(authentication, mockProject, permission)) {
                "User by authentication=$authentication isn't expected to have permission $permission on project $mockProject"
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
