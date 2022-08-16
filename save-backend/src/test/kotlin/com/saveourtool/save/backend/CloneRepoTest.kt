package com.saveourtool.save.backend

import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.ExecutionRepository
import com.saveourtool.save.backend.repository.GitRepository
import com.saveourtool.save.backend.repository.OrganizationRepository
import com.saveourtool.save.backend.repository.ProjectRepository
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.backend.utils.MySqlExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.save.domain.Jdk
import com.saveourtool.save.entities.ExecutionRequest
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.execution.ExecutionType
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.testutils.enqueue
import com.saveourtool.save.utils.toByteBufferFlux
import com.saveourtool.save.v1

import io.kotest.matchers.collections.shouldExist
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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

import java.time.Instant

import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient(timeout = "600000000")
@ExtendWith(MySqlExtension::class)
@MockBeans(
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

    @Autowired
    private lateinit var gitRepository: GitRepository

    @Autowired
    private lateinit var testSuitesSourceService: TestSuitesSourceService

    @Autowired
    private lateinit var testSuitesSourceSnapshotStorage: TestSuitesSourceSnapshotStorage

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    @Test
    @WithMockUser(username = "admin")
    fun checkSaveProject() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        val sdk = Jdk("8")
        mockServerOrchestrator.enqueue(
            "/initializeAgents",
            MockResponse()
                .setResponseCode(202)
                .setBody("Clone pending")
                .addHeader("Content-Type", "application/json")
        )
        val project = projectRepository.findAll().first { it.name == "huaweiName" }
        val gitRepo = gitRepository.findAllByOrganizationId(project.organization.requiredId())
            .first { it.url == "github" }
        val branch = "master"
        val testSuitesSource = testSuitesSourceService.getOrCreate(
            project.organization,
            gitRepo,
            "",
            branch,
        )
        val snapshotKey = TestSuitesSourceSnapshotKey(testSuitesSource.toDto(), "123", Instant.now().toEpochMilli())
        val tempFile = createTempFile()
        tempFile.writeText("TEST")
        testSuitesSourceSnapshotStorage.upload(snapshotKey, tempFile.toByteBufferFlux())
            .block()
        tempFile.deleteExisting()
        val executionRequest = ExecutionRequest(
            project,
            testSuitesSource.git.toDto(),
            "origin/${testSuitesSource.branch}",
            executionId = null,
            sdk = sdk,
            testRootPath = testSuitesSource.testRootPath
        )

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
        executionRepository.findAll().shouldExist {
            it.project.name == project.name &&
                    it.project.organization == project.organization &&
                    it.type == ExecutionType.GIT &&
                    it.sdk == sdk.toString()
        }
        testSuitesSourceSnapshotStorage.delete(snapshotKey).block()
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
        val executionRequest = ExecutionRequest(project, gitRepo, "origin/main", executionId = null, sdk = sdk, testRootPath = ".")
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
        @JvmStatic lateinit var mockServerOrchestrator: MockWebServer

        @AfterEach
        fun cleanup() {
            mockServerOrchestrator.checkQueues()
            mockServerOrchestrator.cleanup()
        }

        @AfterAll
        fun tearDown() {
            mockServerOrchestrator.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerOrchestrator = createMockWebServer()
            mockServerOrchestrator.start()
            registry.add("backend.orchestratorUrl") { "http://localhost:${mockServerOrchestrator.port}" }
        }
    }
}
