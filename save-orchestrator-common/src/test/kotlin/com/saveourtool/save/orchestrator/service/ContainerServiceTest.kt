package com.saveourtool.save.orchestrator.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.*
import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.Project
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.docker.DockerContainerRunner
import com.saveourtool.save.orchestrator.runner.ContainerRunnerException
import com.saveourtool.save.orchestrator.utils.DockerClientTestConfiguration
import com.saveourtool.save.orchestrator.utils.MockserverExtension
import com.saveourtool.save.orchestrator.utils.MockserverExtension.Companion.MOCKSERVER_MOCK_URL

import com.saveourtool.save.orchestrator.utils.silentlyCleanupContainer
import com.saveourtool.common.utils.error
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import reactor.kotlin.core.publisher.toMono
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.math.absoluteValue
import kotlin.random.Random

@SpringBootTest(properties = ["orchestrator.docker.runtime=runc"])
@ActiveProfiles("docker-test")
@Import(
    DockerClientTestConfiguration::class,
    DockerContainerRunner::class,
    ContainerService::class,
    AgentService::class,
)
@ExtendWith(MockserverExtension::class)
class ContainerServiceTest {
    @Autowired private lateinit var dockerClient: DockerClient
    @Autowired private lateinit var containerService: ContainerService
    @MockBean private lateinit var orchestratorAgentService: OrchestratorAgentService
    private lateinit var testContainerId: String

    init {
        if (System.getProperty("os.name").lowercase().contains("win")) {
            System.setProperty("OVERRIDE_HOST_IP", "host-gateway")
        }
    }

    @BeforeEach
    fun setUp() {
        whenever(orchestratorAgentService.updateExecutionStatus(any(), any(), anyOrNull()))
            .thenReturn(ResponseEntity.ok().build<Void>().toMono())
    }

    @Test
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION", "HttpUrlsUsage")
    fun `should create a container with save agent and test resources and start it`() {
        val executionId = Random.nextLong().absoluteValue
        // build base image
        val project = Project.stub(null)
        val testExecution = Execution.stub(project).apply {
            id = executionId
            sdk = "Java:11"
            status = ExecutionStatus.PENDING
        }
        val configuration = containerService.prepareConfiguration(
            testExecution.toRunRequest(
                saveAgentUrl = URL("http://host.docker.internal:${MockserverExtension.getMockserverExposedPort()}$MOCKSERVER_MOCK_URL"),
            )
        )
        // start container and query backend
        try {
            containerService.createAndStartContainers(
                testExecution.requiredId(),
                configuration
            )
        } catch (ex: ContainerRunnerException) {
            logger.error(ex) {
                "Failed test with exception: ${ex.message}"
            }
            fail(ex)
        }
        testContainerId = dockerClient.listContainersCmd()
            .withNameFilter(listOf("-${testExecution.requiredId()}-"))
            .exec()
            .map { it.id }
            .single()
        logger.debug("Created container $testContainerId")

        // assertions
        Thread.sleep(2_500)  // waiting for container to start
        val inspectContainerResponse = dockerClient.inspectContainerCmd(testContainerId).exec()
        Assertions.assertTrue(inspectContainerResponse.state.running!!) {
            dockerClient.logContainerCmd(testContainerId)
                .withStdOut(true)
                .withStdErr(true)
                .exec(object : ResultCallback.Adapter<Frame>() {
                    override fun onNext(frame: Frame?) {
                        logger.info(frame.toString())
                    }
                })
                .awaitCompletion()
            "container $testContainerId is not running, actual state ${inspectContainerResponse.state}"
        }

        verifyNoMoreInteractions(orchestratorAgentService)
    }

    @AfterEach
    fun tearDown() {
        if (::testContainerId.isInitialized) {
            dockerClient.silentlyCleanupContainer(testContainerId)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContainerServiceTest::class.java)
    }
}
