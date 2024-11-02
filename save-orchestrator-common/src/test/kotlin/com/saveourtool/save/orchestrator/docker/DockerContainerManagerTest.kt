package com.saveourtool.save.orchestrator.docker

import com.saveourtool.save.orchestrator.service.ContainerService

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.Image
import com.saveourtool.save.orchestrator.runner.ContainerRunnerException
import com.saveourtool.save.orchestrator.service.OrchestratorAgentService
import com.saveourtool.save.orchestrator.utils.DockerClientTestConfiguration
import com.saveourtool.save.orchestrator.utils.silentlyCleanupContainer
import com.saveourtool.common.utils.error
import com.saveourtool.common.utils.getLogger
import org.junit.jupiter.api.*
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

import kotlin.io.path.createTempFile
import kotlin.math.absoluteValue
import kotlin.random.Random

@SpringBootTest(properties = ["orchestrator.docker.runtime=runc"])
@ActiveProfiles("docker-test")
@Import(
    DockerClientTestConfiguration::class,
    DockerContainerRunner::class,
)
@MockBeans(
    MockBean(OrchestratorAgentService::class),
)
class DockerContainerManagerTest {
    @Autowired private lateinit var dockerClient: DockerClient
    @Autowired private lateinit var dockerAgentRunner: DockerContainerRunner
    private lateinit var baseImage: Image
    private lateinit var testContainerId: String

    init {
        if (System.getProperty("os.name").lowercase().contains("win")) {
            System.setProperty("OVERRIDE_HOST_IP", "host-gateway")
        }
    }

    @BeforeEach
    fun setUp() {
        dockerClient.pullImageCmd("ghcr.io/saveourtool/save-base")
            .withRegistry("https://ghcr.io")
            .withTag("eclipse-temurin-11")
            .exec(PullImageResultCallback())
            .awaitCompletion()
        baseImage = dockerClient.listImagesCmd()
            .exec()
            .first {
                it.repoTags?.contains("ghcr.io/saveourtool/save-base:eclipse-temurin-11") == true
            }
    }

    @Test
    fun `should create a container with specified cmd and then copy resources into it`() {
        val executionId = Random.nextLong().absoluteValue
        val testFile = createTempFile().toFile()
        testFile.writeText("wow such testing")
        try {
            dockerAgentRunner.createAndStart(
                executionId = executionId,
                configuration = ContainerService.RunConfiguration(
                    baseImage.repoTags.first(),
                    listOf("bash", "-c", "./script.sh"),
                    workingDir = "/",
                    env = emptyMap(),
                ),
                replicas = 1,
            )
        } catch (ex: ContainerRunnerException) {
            log.error(ex) {
                "Failed test with exception: ${ex.message}"
            }
            fail(ex)
        }
        testContainerId = dockerClient.listContainersCmd()
            .withNameFilter(listOf("-$executionId-"))
            .exec()
            .map { it.id }
            .single()
        val inspectContainerResponse = dockerClient
            .inspectContainerCmd(testContainerId)
            .exec()
        Assertions.assertEquals("/__cacert_entrypoint.sh", inspectContainerResponse.path)
        inspectContainerResponse.args.forEach { println(it) }
        Assertions.assertArrayEquals(
            arrayOf("bash", "-c", "env \$(cat /home/save-agent/.env | xargs) sh -c \"./script.sh\""),
            inspectContainerResponse.args
        )
        // leading extra slash: https://github.com/moby/moby/issues/6705
        Assertions.assertTrue(inspectContainerResponse.name.startsWith("/save-execution-$executionId"))

        val resourceFile = createTempFile().toFile()
        resourceFile.writeText("Lorem ipsum dolor sit amet")
        dockerAgentRunner.copyResourcesIntoContainer(testContainerId, "/var", listOf(testFile, resourceFile))
    }

    @AfterEach
    fun tearDown() {
        if (::testContainerId.isInitialized) {
            dockerClient.silentlyCleanupContainer(testContainerId)
        }
    }

    companion object {
        private val log: Logger = getLogger<DockerContainerManagerTest>()
    }
}
