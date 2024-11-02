package com.saveourtool.save.orchestrator.docker

import com.saveourtool.common.agent.AgentEnvName
import com.saveourtool.common.utils.debug
import com.saveourtool.common.utils.getLogger
import com.saveourtool.save.orchestrator.DOCKER_METRIC_PREFIX
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.config.ConfigProperties.DockerSettings
import com.saveourtool.save.orchestrator.createTgzStream
import com.saveourtool.save.orchestrator.execTimed
import com.saveourtool.save.orchestrator.getHostIp
import com.saveourtool.save.orchestrator.runner.ContainerRunner
import com.saveourtool.save.orchestrator.runner.ContainerRunnerException
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.runner.SAVE_AGENT_USER_HOME
import com.saveourtool.save.orchestrator.service.ContainerService

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.exception.DockerException
import com.github.dockerjava.api.model.*
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import java.io.File

import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

/**
 * [ContainerRunner] that uses Docker Daemon API to run save-agents
 */
@Component
@Profile("!kubernetes")
class DockerContainerRunner(
    private val configProperties: ConfigProperties,
    private val dockerClient: DockerClient,
    private val meterRegistry: MeterRegistry,
) : ContainerRunner, ContainerRunner.Prunable {
    private val settings: DockerSettings = requireNotNull(configProperties.docker) {
        "Properties under configProperties.docker are not set, but are required with active profiles."
    }

    override fun createAndStart(
        executionId: Long,
        configuration: ContainerService.RunConfiguration,
        replicas: Int,
    ) {
        log.debug { "Pulling image ${configuration.imageTag}" }
        try {
            dockerClient.pullImageCmd(configuration.imageTag)
                .withRegistry("https://ghcr.io")
                .exec(PullImageResultCallback())
                .awaitCompletion()
        } catch (dex: DockerException) {
            throw ContainerRunnerException("Failed to fetch image ${configuration.imageTag}", dex)
        }

        for (number in 1..replicas) {
            log.info("Creating a container #$number for execution.id=$executionId")
            val containerId = try {
                createContainerFromImage(configuration, containerName(executionId, number))
            } catch (dex: DockerException) {
                throw ContainerRunnerException("Unable to create containers", dex)
            }
            log.info("Created a container id=$containerId for execution.id=$executionId, starting it...")
            try {
                dockerClient.startContainerCmd(containerId).exec()
            } catch (dex: DockerException) {
                throw ContainerRunnerException("Unable to start container $containerId", dex)
            }
        }
    }

    override fun isStopped(containerId: String): Boolean = dockerClient.inspectContainerCmd(containerId)
        .exec()
        .state
        .also { state -> log.debug { "Container $containerId has state $state" } }
        .status != RUNNING_STATUS

    override fun cleanupAllByExecution(executionId: Long) {
        log.info("Stopping all agents for execution id=$executionId")
        val containersForExecution = dockerClient.listContainersCmd()
            .withNameFilter(listOf("-$executionId-"))
            .withShowAll(true)
            .exec()

        containersForExecution.map { it.id }.forEach { containerId ->
            log.info("Removing container $containerId")
            val existingContainerIds = dockerClient.listContainersCmd().withShowAll(true).exec()
                .map {
                    it.id
                }
            if (containerId in existingContainerIds) {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec()
            } else {
                log.info("Container $containerId is not present, so won't attempt to remove")
            }
        }
    }

    @Scheduled(cron = "0 0 4 * * MON")
    override fun prune() {
        var reclaimedBytes = 0L
        // Release all old resources, except volumes,
        // since there is no option --filter for `docker volume prune`, and also it could be quite dangerous to remove volumes,
        // as it possible to lose some prepared data
        for (type in PruneType.values().filterNot { it == PruneType.VOLUMES }) {
            val pruneCmd = dockerClient.pruneCmd(type).withUntilFilter(configProperties.dockerResourcesLifetime).exec()
            val currentReclaimedBytes = pruneCmd.spaceReclaimed ?: 0
            log.debug { "Reclaimed $currentReclaimedBytes bytes after prune of docker $type" }
            reclaimedBytes += currentReclaimedBytes
        }
        log.info("Reclaimed $reclaimedBytes bytes after prune command")
    }

    override fun getContainerIdentifier(containerId: String): String = dockerClient.inspectContainerCmd(containerId).exec().name

    /**
     * Creates a docker container
     *
     * @param containerName a name for the created container
     * @return id of created container or null if it wasn't created
     * @throws DockerException if docker daemon has returned an error
     * @throws RuntimeException if an exception not specific to docker has occurred
     */
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    private fun createContainerFromImage(configuration: ContainerService.RunConfiguration,
                                         containerName: String,
    ): String {
        val baseImageTag = configuration.imageTag
        val runCmd = configuration.runCmd
        val envFileTargetPath = "$SAVE_AGENT_USER_HOME/.env"
        val envVariables = configuration.env.mapToEnvs() +
                AgentEnvName.CONTAINER_NAME.toEnv(containerName) +
                kubernetesEnv

        // createContainerCmd accepts image name, not id, so we retrieve it from tags
        val createContainerCmdResponse: CreateContainerResponse = dockerClient.createContainerCmd(baseImageTag)
            .withWorkingDir(EXECUTION_DIR)
            // Load environment variables required by save-agent and then run it.
            // Rely on `runCmd` format: last argument is parameter of the subshell.
            .withCmd(
                // this part is like `sh -c` with probably some other flags
                runCmd.dropLast(1) + (
                        // last element is an actual command that will be executed in a new shell
                        "env $(cat $envFileTargetPath | xargs) sh -c \"${runCmd.last()}\""
                )
            )
            .withName(containerName)
            .withUser("save-agent")
            .withEnv(
                envVariables
            )
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withRuntime(settings.runtime)
                    // processes from inside the container will be able to access host's network using this hostname
                    .withExtraHosts("host.docker.internal:${getHostIp()}")
                    .withLogConfig(
                        when {
                            settings.useLoki -> LogConfig(
                                LogConfig.LoggingType.LOKI,
                                mapOf(
                                    // similar to config in docker-compose.yaml
                                    "mode" to "non-blocking",
                                    "loki-url" to "http://127.0.0.1:9110/loki/api/v1/push",
                                    "loki-external-labels" to "container_name={{.Name}},source=save-agent"
                                )
                            )
                            else -> LogConfig(LogConfig.LoggingType.DEFAULT)
                        }
                    )
            )
            .execTimed(meterRegistry, "$DOCKER_METRIC_PREFIX.container.create")

        val containerId = createContainerCmdResponse.id
        val envFile = createTempDirectory("orchestrator").resolve(envFileTargetPath.substringAfterLast("/")).apply {
            writeText("""
                ${AgentEnvName.CONTAINER_ID.name}=$containerId
                """.trimIndent()
            )
        }
        copyResourcesIntoContainer(
            containerId,
            envFileTargetPath.substringBeforeLast("/"),
            listOf(envFile.toFile())
        )

        return containerId
    }

    /**
     * Copies specified [resources] into the container with id [containerId]
     *
     * @param resources additional resources
     * @param containerId id of the target container
     * @param remotePath path in the target container
     */
    internal fun copyResourcesIntoContainer(containerId: String,
                                            remotePath: String,
                                            resources: Collection<File>) {
        createTgzStream(*resources.toTypedArray()).use { out ->
            dockerClient.copyArchiveToContainerCmd(containerId)
                .withTarInputStream(out.toByteArray().inputStream())
                .withRemotePath(remotePath)
                .execTimed<CopyArchiveToContainerCmd, Void?>(meterRegistry, "$DOCKER_METRIC_PREFIX.container.copy.archive")
        }
    }

    private fun containerName(executionId: Long, number: Int) = "${configProperties.containerNamePrefix}$executionId-$number"

    companion object {
        private val log: Logger = getLogger<DockerContainerRunner>()
        private const val RUNNING_STATUS = "running"
        private val kubernetesEnv: String = AgentEnvName.KUBERNETES.toEnv(false)

        private fun Map<AgentEnvName, Any>.mapToEnvs(): List<String> = entries.map { (key, value) -> key.toEnv(value) }

        private fun AgentEnvName.toEnv(value: Any): String = "${this.name}=$value"
    }
}
