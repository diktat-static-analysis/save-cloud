package com.saveourtool.save.backend

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.authservice.config.NoopWebSecurityConfig
import com.saveourtool.save.backend.controllers.DownloadFilesController
import com.saveourtool.save.backend.controllers.FileController
import com.saveourtool.save.backend.controllers.internal.FileInternalController
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.*
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.common.configs.WebConfig
import com.saveourtool.save.core.result.DebugInfo
import com.saveourtool.save.core.result.Pass
import com.saveourtool.common.domain.*
import com.saveourtool.common.entities.*
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.security.ProjectPermissionEvaluator
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.service.ProjectService
import com.saveourtool.common.service.UserService
import com.saveourtool.common.utils.BlockingBridge
import com.saveourtool.common.utils.CONTENT_LENGTH_CUSTOM
import com.saveourtool.common.utils.collectToInputStream
import com.saveourtool.common.v1
import org.jetbrains.annotations.Blocking

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.nio.ByteBuffer
import java.nio.file.Path

import java.time.LocalDateTime
import java.util.concurrent.Future
import kotlin.io.path.*

@ActiveProfiles("test")
@WebFluxTest(controllers = [DownloadFilesController::class, FileController::class, FileInternalController::class])
@Import(
    WebConfig::class,
    NoopWebSecurityConfig::class,
    S11nTestConfig::class,
)
@AutoConfigureWebTestClient
@EnableConfigurationProperties(ConfigProperties::class)
@MockBeans(
    MockBean(OrganizationService::class),
    MockBean(UserService::class),
    MockBean(ExecutionService::class),
    MockBean(AgentService::class),
    MockBean(ProjectPermissionEvaluator::class),
    MockBean(DebugInfoStorage::class),
    MockBean(ExecutionInfoStorage::class),
    MockBean(BlockingBridge::class),
)
class DownloadFilesTest {
    private val organization = Organization.stub(2).apply {
        name = "Example.com"
    }
    private val organization2 = Organization.stub(1).apply {
        name = "Huawei"
    }
    private var testProject: Project = Project.stub(3, organization).apply {
        name = "TheProject"
        url = "example.com"
        description = "This is an example project"
    }
    private var testProject2: Project = Project.stub(1, organization2).apply {
        name = "huaweiName"
        url = "huawei.com"
        description = "test description"
    }
    private var file1: File = File(
        project = testProject,
        name = "test-1.txt",
        uploadedTime = LocalDateTime.now(),
        sizeBytes = 11L,
        isExecutable = false,
    ).apply {
        id = 1L
    }
    private var file2: File = File(
        project = testProject2,
        name = "test-2.txt",
        uploadedTime = LocalDateTime.now(),
        sizeBytes = 22L,
        isExecutable = false,
    ).apply {
        id = 2L
    }

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var fileStorage: FileStorage

    @MockBean
    private lateinit var projectService: ProjectService

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    @WithMockUser(roles = ["USER"])
    fun `should download a file`() {
        mutateMockedUser(id = 1)

        val fileContent = "Lorem ipsum"
        whenever(fileStorage.doesExist(file1.toDto()))
            .thenReturn(true.toMono())
        whenever(fileStorage.getFileById(eq(file1.requiredId())))
            .thenReturn(Mono.just(file1.toDto()))
        whenever(fileStorage.download(eq(file1.toDto())))
            .thenReturn(Flux.just(ByteBuffer.wrap(fileContent.toByteArray())))
        whenever(fileStorage.listByProject(eq(file1.project)))
            .thenReturn(Flux.just(file1.toDto()))

        whenever(projectService.findByNameAndOrganizationNameAndCreatedStatus(eq(testProject.name), eq(organization.name)))
            .thenReturn(testProject)
        whenever(projectService.findWithPermissionByNameAndOrganization(any(), eq(testProject.name), eq(organization.name), eq(Permission.READ), anyOrNull(), any()))
            .thenAnswer { Mono.just(testProject) }

        webTestClient.get()
            .uri("/api/${v1}/files/download?fileId={fileId}", file1.requiredId())
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
                Assertions.assertArrayEquals(fileContent.toByteArray(), it.responseBody)
            }

        webTestClient.get()
            .uri("/api/${v1}/files/{organizationName}/{projectName}/list", testProject.organization.name, testProject.name)
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<FileDto>()
            .hasSize(1)
            .consumeWith<WebTestClient.ListBodySpec<FileDto>> {
                Assertions.assertEquals(
                    listOf(file1.toDto()), it.responseBody
                )
            }
    }

    @Test
    fun `should return 404 for non-existent files`() {
        webTestClient.get()
            .uri("/api/${v1}/files/download/invalid-name")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Suppress("LongMethod")
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun checkUpload(@TempDir tmpDir: Path) {
        mutateMockedUser(id = 1)

        val fileContent = "Some content"
        val file = (tmpDir / file2.name).createFile()
            .also { it.writeText(fileContent) }
        whenever(fileStorage.doesExist(argThat { candidateTo(file2) }))
            .thenReturn(Mono.just(false))
        whenever(fileStorage.upload(argThat { candidateTo(file2) }, eq(file.fileSize()), argThat { collectToString() == fileContent }))
            .thenReturn(Mono.just(file2.toDto()))

        whenever(projectService.findByNameAndOrganizationNameAndCreatedStatus(eq(testProject2.name), eq(organization2.name)))
            .thenReturn(testProject2)
        whenever(projectService.findWithPermissionByNameAndOrganization(any(), eq(testProject2.name), eq(organization2.name), eq(Permission.WRITE), anyOrNull(), any()))
            .thenAnswer { Mono.just(testProject2) }

        val body = MultipartBodyBuilder().apply {
            val resource = FileSystemResource(file)
            part("file", resource)
        }
            .build()

        webTestClient.post()
            .uri("/api/${v1}/files/Huawei/huaweiName/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(body))
            .header(CONTENT_LENGTH_CUSTOM, file.fileSize().toString())
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<FileDto>()
            .consumeWith { result ->
                Assertions.assertEquals(
                    file2.toDto(),
                    result.responseBody
                )
            }
    }

    @Test
    fun `should save test data`() {
        val execution: Execution = mock()
        whenever(execution.id).thenReturn(1)

        webTestClient.post()
            .uri("/internal/files/debug-info?executionId=1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                TestResultDebugInfo(
                    TestResultLocation("suite1", "plugin1", "path/to/test/Test.test"),
                    DebugInfo("./a.out", "stdout", "stderr", 42L),
                    Pass(null),
                )
            )
            .exchange()
            .expectStatus()
            .isOk
    }

    companion object {
        private fun FileDto.candidateTo(file: File) = name == file.name && projectCoordinates == file.project.toProjectCoordinates()

        /**
         * Sometimes, the [block][Mono.block] operation of the resulting [Mono]
         * may get executed using a non-blocking scheduler, such as
         * [Schedulers.single] or [Schedulers.parallel], resulting in a failure
         * at [reactor.core.publisher.BlockingSingleSubscriber.blockingGet].
         *
         * As a workaround, we first convert the [Mono] to a [Future].
         *
         * See [#1787](https://github.com/saveourtool/save-cloud/pull/1787) for details.
         *
         * @see Mono.block
         * @see Schedulers.single
         * @see Schedulers.parallel
         * @see reactor.core.publisher.BlockingSingleSubscriber.blockingGet
         */
        @Blocking
        private fun Flux<ByteBuffer>.collectToString(): String? = collectToInputStream()
            .map { it.bufferedReader().readText() }
            .toFuture()
            .get()
    }
}
