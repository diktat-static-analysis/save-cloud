package org.cqfn.save.backend

import org.cqfn.save.backend.controllers.ProjectController
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.OrganizationRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.backend.utils.mutateMockedUser
import org.cqfn.save.domain.Jdk
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.testutils.checkQueues
import org.cqfn.save.testutils.cleanup
import org.cqfn.save.testutils.createMockWebServer
import org.cqfn.save.testutils.enqueue

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.cqfn.save.v1
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(StandardSuitesUpdateScheduler::class),
    MockBean(ProjectController::class),
)
class CloneRepoTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var executionRepository: ExecutionRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Test
    @WithMockUser(username = "admin")
    fun checkSaveProject() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        val sdk = Jdk("8")
        mockServerPreprocessor.enqueue(
            "/upload",
            MockResponse()
                .setResponseCode(202)
                .setBody("Clone pending")
                .addHeader("Content-Type", "application/json")
        )
        val project = projectRepository.findAll().first()
        val gitRepo = GitDto("1")
        val executionRequest = ExecutionRequest(project, gitRepo, executionId = null, sdk = sdk, testRootPath = ".")
        val multipart = MultipartBodyBuilder().apply {
            part("executionRequest", executionRequest)
        }
            .build()
        webClient.post()
            .uri("/api/$v1/submitExecutionRequest")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multipart))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.ACCEPTED)
        Assertions.assertTrue(
            executionRepository.findAll().any {
                it.project.name == project.name &&
                        it.project.organization == project.organization &&
                        it.type == ExecutionType.GIT &&
                        it.sdk == sdk.toString()
            }
        )
    }

    @Test
    @WithMockUser(username = "admin")
    fun checkNonExistingProject() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        val sdk = Jdk("11")
        val organization = organizationRepository.getOrganizationById(1)
        val project = Project.stub(null, organization)
        val gitRepo = GitDto("1")
        val executionRequest = ExecutionRequest(project, gitRepo, executionId = null, sdk = sdk, testRootPath = ".")
        val executionsClones = listOf(executionRequest, executionRequest, executionRequest)
        // fixme: why is it repeated 3 times?
        val multiparts = executionsClones.map {
            MultipartBodyBuilder().apply {
                part("executionRequest", it)
            }
                .build()
        }
        multiparts.forEach {
            webClient.post()
                .uri("/api/$v1/submitExecutionRequest")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(it))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    companion object {
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
