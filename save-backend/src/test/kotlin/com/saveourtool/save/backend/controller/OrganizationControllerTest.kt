package com.saveourtool.save.backend.controller

import com.saveourtool.save.authservice.config.NoopWebSecurityConfig
import com.saveourtool.save.backend.controllers.OrganizationController
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.S11nTestConfig
import com.saveourtool.save.backend.storage.TestsSourceSnapshotStorage
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.common.configs.WebConfig
import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.*
import com.saveourtool.common.repository.*
import com.saveourtool.common.security.OrganizationPermissionEvaluator
import com.saveourtool.common.security.ProjectPermissionEvaluator
import com.saveourtool.common.service.*
import com.saveourtool.common.storage.AvatarStorage
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.testutils.enqueue
import com.saveourtool.common.utils.BlockingBridge
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.utils.info
import com.saveourtool.common.v1
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@WebFluxTest(controllers = [OrganizationController::class])
@Import(
    OrganizationService::class,
    OrganizationPermissionEvaluator::class,
    LnkUserOrganizationService::class,
    NoopWebSecurityConfig::class,
    GitService::class,
    TestSuitesSourceService::class,
    TestSuitesService::class,
    WebConfig::class,
    ProjectPermissionEvaluator::class,
    LnkUserProjectService::class,
    UserService::class,
    S11nTestConfig::class,
)
@MockBeans(
    MockBean(ExecutionService::class),
    MockBean(ProjectService::class),
    MockBean(AgentService::class),
    MockBean(AgentStatusService::class),
    MockBean(TestSuitesSourceRepository::class),
    MockBean(TestSuiteRepository::class),
    MockBean(TestRepository::class),
    MockBean(TestExecutionRepository::class),
    MockBean(TestsSourceVersionService::class),
    MockBean(TestsSourceSnapshotStorage::class),
    MockBean(ExecutionRepository::class),
    MockBean(AgentStatusRepository::class),
    MockBean(AgentRepository::class),
    MockBean(ProjectRepository::class),
    MockBean(LnkUserProjectRepository::class),
    MockBean(LnkUserOrganizationRepository::class),
    MockBean(OriginalLoginRepository::class),
    MockBean(LnkContestProjectService::class),
    MockBean(LnkOrganizationTestSuiteService::class),
    MockBean(LnkExecutionTestSuiteService::class),
    MockBean(AvatarStorage::class),
    MockBean(BlockingBridge::class),
)
@AutoConfigureWebTestClient
@Suppress("UnsafeCallOnNullableType")
class OrganizationControllerTest {
    private val organization = Organization(
        "OrgForTests",
        OrganizationStatus.CREATED,
        dateCreated = LocalDateTime.now(),
    ).also { it.id = 1 }
    private val adminUser = User(
        "admin",
        "",
        Role.VIEWER.toString(),
        ""
    ).also { it.id = 1 }
    private val johnDoeUser = User(
        "JohnDoe",
        "",
        Role.VIEWER.toString(),
        ""
    ).also { it.id = 2 }

    @MockBean
    private lateinit var organizationRepository: OrganizationRepository

    @MockBean
    private lateinit var lnkUserOrganizationRepository: LnkUserOrganizationRepository

    @Autowired
    private lateinit var webClient: WebTestClient

    @MockBean
    private lateinit var userRepository: UserRepository

    @MockBean
    private lateinit var gitRepository: GitRepository

    @BeforeEach
    internal fun setUp() {
        whenever(gitRepository.save(any())).then {
            it.arguments[0] as Git
        }
    }

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `delete organization with owner permission`() {
        mutateMockedUserAndLink(organization, adminUser, Role.OWNER)
        webClient.post()
            .uri("/api/${v1}/organizations/${organization.name}/change-status?status=${OrganizationStatus.DELETED}")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @WithMockUser(value = "JohDoe", roles = ["SUPER_ADMIN"])
    fun `ban organization with super-admin permission`() {
        mutateMockedUserAndLink(organization, johnDoeUser, Role.SUPER_ADMIN)
        webClient.post()
            .uri("/api/${v1}/organizations/${organization.name}/change-status?status=${OrganizationStatus.BANNED}")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @WithMockUser(value = "JohDoe", roles = ["VIEWER"])
    fun `delete organization without owner permission`() {
        mutateMockedUserAndLink(organization, johnDoeUser, Role.VIEWER)
        webClient.post()
            .uri("/api/${v1}/organizations/${organization.name}/change-status?status=${OrganizationStatus.DELETED}")
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `list git credential in organization`() {
        mutateMockedUserAndLink(organization, adminUser, Role.OWNER)

        val git1 = Git("url1", null, null, organization)
        val git2 = Git("url2", null, null, organization)
        given(gitRepository.findAllByOrganizationId(organization.requiredId())).willReturn(listOf(git1, git2))
        webClient.get()
            .uri("/api/${v1}/organizations/${organization.name}/list-git")
            .exchange()
            .also {
                it.expectBodyList(GitDto::class.java)
                    .hasSize(2)
                    .contains(GitDto(git1.url))
                    .contains(GitDto(git2.url))
            }
            .expectStatus()
            .isOk
    }

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `create git credential in organization`() {
        mutateMockedUserAndLink(organization, adminUser, Role.OWNER)

        val assertionsToCreate = mockGitCheckConnectivity(true)
        val gitDtoToCreate = GitDto("url")
        webClient.post()
            .uri("/api/${v1}/organizations/${organization.name}/create-git")
            .bodyValue(gitDtoToCreate)
            .exchange()
            .expectStatus()
            .isOk
        verify(gitRepository).findByOrganizationAndUrl(organization, gitDtoToCreate.url)
        verify(gitRepository).save(argThat { isEqualToDto(gitDtoToCreate) })
        assertionsToCreate.forEach { Assertions.assertNotNull(it) }
        verifyNoMoreInteractions(gitRepository)
    }

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `create invalid git credential in organization`() {
        mutateMockedUserAndLink(organization, adminUser, Role.OWNER)

        val assertionsToCreate = mockGitCheckConnectivity(false)
        val gitDtoToCreate = GitDto("invalid-url")
        webClient.post()
            .uri("/api/${v1}/organizations/${organization.name}/create-git")
            .bodyValue(gitDtoToCreate)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
        assertionsToCreate.forEach { Assertions.assertNotNull(it) }
        verifyNoMoreInteractions(gitRepository)
    }

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `create duplicate git credential in organization`() {
        mutateMockedUserAndLink(organization, adminUser, Role.OWNER)

        val assertionsToCreate = mockGitCheckConnectivity(true)
        val gitExisted = Git(
            url = "duplicate-url",
            organization = organization,
        )
        doReturn(gitExisted).whenever(gitRepository).findByOrganizationAndUrl(organization, gitExisted.url)
        webClient.post()
            .uri("/api/${v1}/organizations/${organization.name}/create-git")
            .bodyValue(gitExisted.toDto())
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
        verify(gitRepository).findByOrganizationAndUrl(organization, gitExisted.url)
        assertionsToCreate.forEach { Assertions.assertNotNull(it) }
        verifyNoMoreInteractions(gitRepository)
    }

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `update git credential in organization`() {
        mutateMockedUserAndLink(organization, adminUser, Role.OWNER)

        val assertionsToUpdate = mockGitCheckConnectivity(true)
        val gitExisted = Git(
            url = "url",
            username = null,
            password = null,
            organization = organization
        ).apply { id = 1L }
        val gitDtoToUpdate = gitExisted.toDto()
            .copy(
                username = "updated",
                password = "updated",
            )
        doReturn(gitExisted).whenever(gitRepository).findByOrganizationAndUrl(organization, gitExisted.url)
        webClient.post()
            .uri("/api/${v1}/organizations/${organization.name}/update-git")
            .bodyValue(gitDtoToUpdate)
            .exchange()
            .expectStatus()
            .isOk
        verify(gitRepository).findByOrganizationAndUrl(organization, gitDtoToUpdate.url)
        verify(gitRepository).save(argThat { isEqualToDto(gitDtoToUpdate) && id == gitExisted.id })
        assertionsToUpdate.forEach { Assertions.assertNotNull(it) }
        verifyNoMoreInteractions(gitRepository)
    }

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `delete git credential in organization`() {
        mutateMockedUserAndLink(organization, adminUser, Role.OWNER)

        val gitExisted = Git(
            url = "url",
            organization = organization
        )
        given(gitRepository.findByOrganizationAndUrl(organization, gitExisted.url)).willReturn(gitExisted)
        webClient.delete()
            .uri("/api/${v1}/organizations/${organization.name}/delete-git?url=${gitExisted.url}")
            .exchange()
            .expectStatus()
            .isOk
        verify(gitRepository, times(1)).delete(gitExisted)
    }

    private fun mockGitCheckConnectivity(result: Boolean): Sequence<RecordedRequest?> {
        mockServerPreprocessor.enqueue(
            "/git/check-connectivity",
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(result.toString())
        )
        return sequence {
            yield(mockServerPreprocessor.takeRequest(60, TimeUnit.SECONDS))
        }.onEach {
            log.info { "Request $it" }
        }
    }

    private fun Git.isEqualToDto(gitDto: GitDto) =
            gitDto.url == url && gitDto.username == password && gitDto.password == password

    private fun mutateMockedUserAndLink(organization: Organization, user: User, userRole: Role) {
        mutateMockedUser(id = user.requiredId())
        prepareLink(organization, user, userRole)
    }

    private fun prepareLink(organization: Organization, user: User, userRole: Role) {
        given(lnkUserOrganizationRepository.findByUserIdAndOrganizationName(any(), any())).willReturn(
            LnkUserOrganization(organization, user, userRole)
        )
        given(lnkUserOrganizationRepository.findByUserIdAndOrganization(any(), any())).willReturn(
            LnkUserOrganization(organization, user, userRole)
        )
        given(organizationRepository.findByName(any())).willReturn(organization)
        given(organizationRepository.findByNameAndStatusIn(any(), any())).willReturn(organization)
        whenever(organizationRepository.save(any())).thenReturn(organization)
        given(userRepository.findByName(any())).willReturn(user)
    }

    companion object {
        private val log: Logger = getLogger<OrganizationControllerTest>()
        @JvmStatic lateinit var mockServerPreprocessor: MockWebServer

        @AfterEach
        fun cleanup() {
            mockServerPreprocessor.checkQueues()
            mockServerPreprocessor.cleanup()
        }

        @AfterAll
        fun tearDown() {
            mockServerPreprocessor.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerPreprocessor = createMockWebServer()
            mockServerPreprocessor.start()
            registry.add("backend.preprocessorUrl") { "http://localhost:${mockServerPreprocessor.port}" }
        }
    }
}
