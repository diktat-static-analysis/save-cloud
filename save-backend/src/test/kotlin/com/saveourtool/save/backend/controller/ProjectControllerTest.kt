package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.common.entities.*
import com.saveourtool.common.filters.ProjectFilter
import com.saveourtool.common.repository.OrganizationRepository
import com.saveourtool.common.repository.ProjectRepository
import com.saveourtool.common.service.LnkUserProjectService
import com.saveourtool.common.service.UserService
import com.saveourtool.common.v1

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(InfraExtension::class)
@MockBeans(
    MockBean(LnkUserProjectService::class),
)
@Suppress("UnsafeCallOnNullableType")
class ProjectControllerTest {
    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var webClient: WebTestClient

    @MockBean private lateinit var userDetailsService: UserService

    @Test
    @WithMockUser
    fun `should return all public projects`() {
        given(userDetailsService.getUserByName(any())).willReturn(mockUser(99))

        webClient
            .post()
            .uri("/api/${v1}/projects/by-filters")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(ProjectFilter.created)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<List<ProjectDto>>()
            .consumeWith { exchangeResult ->
                val projects = exchangeResult.responseBody!!
                Assertions.assertTrue(projects.isNotEmpty())
                projects.forEach { Assertions.assertTrue(it.isPublic) }
            }
    }

    @Test
    @WithMockUser(value = "admin", roles = ["SUPER_ADMIN"])
    fun `should return project based on name and owner`() {
        mutateMockedUser(id = 1)

        getProjectAndAssert("huaweiName", "Huawei") {
            expectStatus()
                .isOk
                .expectBody<ProjectDto>()
                .consumeWith {
                    requireNotNull(it.responseBody)
                    Assertions.assertEquals(it.responseBody!!.url, "https://huawei.com")
                }
        }
    }

    @Test
    @WithMockUser(username = "MrBruh", roles = ["VIEWER"])
    fun `should return 200 if project is public`() {
        given(userDetailsService.getUserByName(any())).willReturn(mockUser(99))
        getProjectAndAssert("huaweiName", "Huawei") {
            expectStatus().isOk
        }
    }

    @Test
    @WithMockUser(username = "MrBruh", roles = ["VIEWER"])
    fun `should return 404 if user doesn't have access to a private project`() {
        given(userDetailsService.getUserByName(any())).willReturn(mockUser(99))

        getProjectAndAssert("TheProject", "Example.com") {
            expectStatus().isNotFound
        }
    }

    @Test
    @WithMockUser(value = "admin", roles = ["SUPER_ADMIN"])
    fun `delete project with owner permission`() {
        mutateMockedUser(id = 2)
        val organization: Organization = organizationRepository.getOrganizationById(1)
        val project = Project(
            "ToDelete",
            "http://test.com",
            "",
            ProjectStatus.CREATED,
            organization = organization,
        )

        projectRepository.save(project)

        webClient.post()
            .uri("/api/${v1}/projects/${organization.name}/${project.name}/change-status?status=${ProjectStatus.DELETED}")
            .exchange()
            .expectStatus()
            .isOk

        val projectFromDb = projectRepository.findByNameAndOrganization(project.name, organization)
        Assertions.assertTrue(
            projectFromDb?.status == ProjectStatus.DELETED
        )
    }

    @Test
    @WithMockUser(value = "admin", roles = ["SUPER_ADMIN"])
    fun `ban project with super admin permission`() {
        mutateMockedUser(id = 2)
        val organization: Organization = organizationRepository.getOrganizationById(1)
        val project = Project(
            "ToDelete1",
            "http://test.com",
            "",
            ProjectStatus.CREATED,
            organization = organization,
        )

        projectRepository.save(project)

        webClient.post()
            .uri("/api/${v1}/projects/${organization.name}/${project.name}/change-status?status=${ProjectStatus.BANNED}")
            .exchange()
            .expectStatus()
            .isOk

        val projectFromDb = projectRepository.findByNameAndOrganization(project.name, organization)
        Assertions.assertTrue(
            projectFromDb?.status == ProjectStatus.BANNED
        )
    }

    @Test
    @WithMockUser(value = "JohnDoe", roles = ["VIEWER"])
    fun `delete project without owner permission`() {
        given(userDetailsService.getUserByName(any())).willReturn(mockUser(3))
        val organization: Organization = organizationRepository.getOrganizationById(2)
        val project = Project(
            "ToDelete1",
            "http://test.com",
            "",
            ProjectStatus.CREATED,
            organization = organization,
        )

        projectRepository.save(project)

        webClient.post()
            .uri("/api/${v1}/projects/${organization.name}/${project.name}/change-status?status=${ProjectStatus.DELETED}")
            .exchange()
            .expectStatus()
            .isForbidden

        val projectFromDb = projectRepository.findByNameAndOrganization(project.name, organization)
        Assertions.assertTrue(
            projectFromDb?.status == ProjectStatus.CREATED
        )
    }

    @Test
    @WithMockUser(username = "JohnDoe", roles = ["VIEWER"])
    fun `check save new project`() {
        given(userDetailsService.getUserByName(any())).willReturn(mockUser(2))
        mutateMockedUser(id = 2)

        // `project` references an existing user from test data
        val organization: Organization = organizationRepository.getOrganizationById(1)
        val project = Project(
            "I",
            "http://test.com",
            "uurl",
            ProjectStatus.CREATED,
            organization = organization,
        )
        saveProjectAndAssert(
            project,
            { expectStatus().isOk }
        ) {
            expectStatus()
                .isOk
                .expectBody<ProjectDto>()
                .consumeWith {
                    requireNotNull(it.responseBody)
                    Assertions.assertEquals(it.responseBody!!.url, project.url)
                }
        }
    }

    @Test
    @WithMockUser
    fun `should forbid updating a project for a viewer`() {
        val project = Project.stub(99).apply {
            organization = organizationRepository.findById(1).get()
        }
        projectRepository.save(project)
        given(userDetailsService.getUserByName(any())).willReturn(mockUser(3))

        webClient.post()
            .uri("/api/${v1}/projects/update")
            .bodyValue(project.toDto())
            .exchange()
            .expectStatus()
            .isForbidden
    }

    private fun getProjectAndAssert(
        name: String,
        organizationName: String,
        assertion: WebTestClient.ResponseSpec.() -> Unit
    ) = webClient
        .get()
        .uri("/api/${v1}/projects/get/organization-name?name=$name&organizationName=$organizationName")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .let { assertion(it) }

    private fun saveProjectAndAssert(
        newProject: Project,
        saveAssertion: WebTestClient.ResponseSpec.() -> Unit,
        getAssertion: WebTestClient.ResponseSpec.() -> Unit,
    ) {
        webClient
            .post()
            .uri("/api/${v1}/projects/save")
            .body(BodyInserters.fromValue(newProject.toDto()))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .let { saveAssertion(it) }

        webClient
            .get()
            .uri("/api/${v1}/projects/get/organization-name?name=${newProject.name}&organizationName=${newProject.organization.name}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .let { getAssertion(it) }
    }

    private fun mockUser(id: Long) = User("mocked", null, null, "").apply { this.id = id }
}
