package com.saveourtool.save.backend.service

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.Project
import com.saveourtool.common.entities.User
import com.saveourtool.common.permission.SetRoleRequest
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.service.LnkUserProjectService
import com.saveourtool.common.service.ProjectService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@ExtendWith(SpringExtension::class)
@Import(PermissionService::class)
class PermissionServiceTest {
    @Autowired private lateinit var permissionService: PermissionService
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var projectService: ProjectService
    @MockBean private lateinit var lnkUserProjectService: LnkUserProjectService

    @Test
    fun `should return a role`() {
        given(userRepository.findByName(any())).willAnswer { invocationOnMock ->
            User(invocationOnMock.arguments[0] as String, null, null)
                .apply { id = 99 }
        }
        given(projectService.findByNameAndOrganizationNameAndCreatedStatus(any(), any())).willAnswer {
            Project.stub(id = 99)
        }
        given(projectService.findByNameAndOrganizationNameAndStatusIn(any(), any(), any())).willAnswer {
            Project.stub(id = 99)
        }
        given(lnkUserProjectService.findRoleByUserIdAndProject(eq(99), any())).willReturn(Role.ADMIN)

        val role = permissionService.getRole(userName = "admin", projectName = "Example", organizationName = "Example Org")
            .blockOptional()

        Assertions.assertEquals(Role.ADMIN, role.get())
    }

    @Test
    fun `should return empty for non-existent projects or users`() {
        given(userRepository.findByName(any())).willReturn(null)
        given(projectService.findByNameAndOrganizationNameAndStatusIn(any(), any(), any())).willReturn(null)

        val role = permissionService.getRole(userName = "admin", projectName = "Example", organizationName = "Example Org")
            .blockOptional()

        Assertions.assertTrue(role.isEmpty)
        verify(lnkUserProjectService, times(0)).findRoleByUserIdAndProject(any(), any())
    }

    @Test
    fun `should add a role`() {
        given(userRepository.findByName(any())).willAnswer { invocationOnMock ->
            User(invocationOnMock.arguments[0] as String, null, null)
                .apply { id = 99 }
        }
        given(projectService.findByNameAndOrganizationNameAndCreatedStatus(any(), any())).willAnswer {
            Project.stub(id = 99)
        }
        given(projectService.findByNameAndOrganizationNameAndStatusIn(any(), any(), any())).willAnswer {
            Project.stub(id = 99)
        }

        val result = permissionService.setRole("Example Org", "Example", SetRoleRequest("user", Role.ADMIN))
            .blockOptional()

        Assertions.assertTrue(result.isPresent)
        verify(lnkUserProjectService, times(1)).setRole(any(), any(), any())
    }

    private fun PermissionService.getRole(userName: String, projectName: String, organizationName: String): Mono<Role> =
            findUserAndProject(userName, organizationName, projectName).map { (user, project) ->
                getRole(user, project)
            }
}
