package com.saveourtool.save.orchestrator.kubernetes

import com.saveourtool.common.agent.AgentEnvName
import com.saveourtool.common.utils.debug
import com.saveourtool.common.utils.warn
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.runner.ContainerRunner
import com.saveourtool.save.orchestrator.runner.ContainerRunnerException
import com.saveourtool.save.orchestrator.service.ContainerService

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * A component that manages save-agents running in Kubernetes.
 */
@Component
@Profile("kubernetes")
class KubernetesManager(
    private val kc: KubernetesClient,
    private val configProperties: ConfigProperties,
) : ContainerRunner {
    private val kubernetesSettings = requireNotNull(configProperties.kubernetes) {
        "orchestrator.kubernetes.* properties are required in this profile"
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "LongMethod",
        "MagicNumber",
        "NestedBlockDepth",
        "ComplexMethod",
    )
    override fun createAndStart(executionId: Long,
                                configuration: ContainerService.RunConfiguration,
                                replicas: Int,
    ) {
        val baseImageTag = configuration.imageTag
        val agentRunCmd = configuration.runCmd
        val workingDir = configuration.workingDir
        // fixme: pass image name instead of ID from the outside

        // Creating Kubernetes objects that will be responsible for lifecycle of save-agents.
        // We use Job, because Deployment will always try to restart failing pods.
        val job = Job().apply {
            metadata = ObjectMeta().apply {
                name = jobNameForExecution(executionId)
                namespace = kubernetesSettings.agentNamespace
            }
            spec = JobSpec().apply {
                parallelism = replicas
                ttlSecondsAfterFinished = kubernetesSettings.ttlAfterFinished.toSeconds().toInt()
                // do not attempt to restart failed pods, because if we manually stop pods by deleting them,
                // job controller would think that they need to be restarted
                backoffLimit = 0
                template = PodTemplateSpec().apply {
                    spec = PodSpec().apply {
                        if (kubernetesSettings.useGvisor) {
                            nodeSelector = mapOf(
                                "gvisor" to "enabled"
                            )
                            runtimeClassName = "gvisor"
                        }
                        metadata = ObjectMeta().apply {
                            labels = mapOf(
                                EXECUTION_ID_LABEL to executionId.toString(),
                                // "baseImageName" to baseImageName
                                "io.kompose.service" to "save-agent",
                                // todo: should be set to version of agent that is stored in backend...
                                // "version" to SAVE_CLI_VERSION
                            )
                        }
                        // If agent fails, we should handle it manually (update statuses, attempt restart etc.)
                        restartPolicy = "Never"
                        containers = listOf(
                            agentContainerSpec(baseImageTag, agentRunCmd, workingDir, configuration.env)
                        )
                    }
                }
            }
        }
        logger.debug { "Attempt to create Job from the following spec: $job" }
        try {
            kc
                .resource(job)
                .inNamespace(kubernetesSettings.agentNamespace)
                .create()
            logger.info("Created Job for execution id=$executionId")
        } catch (kex: KubernetesClientException) {
            throw ContainerRunnerException("Unable to create a job for execution $executionId", kex)
        }
    }

    override fun cleanupAllByExecution(executionId: Long) {
        logger.debug { "Removing a job for execution id=$executionId" }
        val jobName = jobNameForExecution(executionId)
        val job = kc.batch()
            .v1()
            .jobs()
            .inNamespace(kubernetesSettings.agentNamespace)
            .withName(jobName)
        job.get()?.let {
            val deletedResources = job.delete()
            val isDeleted = deletedResources.size == 1
            if (!isDeleted) {
                throw ContainerRunnerException("Failed to delete job with name $jobName: response is $deletedResources")
            }
            logger.debug { "Deleted Job for execution id=$executionId" }
        } ?: run {
            logger.warn { "Failed to delete job with name $jobName: there is no such job" }
            return
        }
        logger.debug { "Cleanup job for execution id=$executionId" }
    }

    override fun isStopped(containerId: String): Boolean {
        val pod = kc.pods()
            .inNamespace(kubernetesSettings.agentNamespace)
            .withName(containerId)
            .get()
        return pod == null || run {
            // Retrieve reason based on https://github.com/kubernetes/kubernetes/issues/22839
            val reason = pod.status.phase ?: pod.status.reason
            val isRunning = pod.status.containerStatuses.any {
                it.ready && it.state.running != null
            }
            logger.debug { "Pod name=$containerId is still present; reason=$reason, isRunning=$isRunning, conditions=${pod.status.conditions}" }
            if (reason == "Completed" && isRunning) {
                "ContainerReady" in pod.status.conditions.map { it.type }
            } else {
                !isRunning
            }
        }
    }

    override fun getContainerIdentifier(containerId: String): String = containerId

    private fun jobNameForExecution(executionId: Long) = "${configProperties.containerNamePrefix}$executionId"

    @Suppress("TOO_LONG_FUNCTION")
    private fun agentContainerSpec(
        imageName: String,
        agentRunCmd: List<String>,
        workingDir: String,
        env: Map<AgentEnvName, String>,
    ) = Container().apply {
        name = "save-agent-pod"
        image = imageName
        imagePullPolicy = "IfNotPresent"  // so that local images could be used

        val staticEnvs = env.mapToEnvs()
        this.env = staticEnvs + containerIdEnv + kubernetesEnv

        this.command = agentRunCmd.dropLast(1)
        this.args = listOf(agentRunCmd.last())

        this.workingDir = workingDir

        resources = with(kubernetesSettings) {
            ResourceRequirements().apply {
                requests = mapOf(
                    "cpu" to Quantity(agentCpuRequests),
                    "memory" to Quantity(agentMemoryRequests),
                )
                limits = mapOf(
                    "cpu" to Quantity(agentCpuLimits),
                    "memory" to Quantity(agentMemoryLimits),
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesManager::class.java)
        private const val EXECUTION_ID_LABEL = "executionId"
        private val containerIdEnv = setOf(AgentEnvName.CONTAINER_ID, AgentEnvName.CONTAINER_NAME)
            .map { it.name }
            .map { envName ->
                EnvVar().apply {
                    name = envName
                    valueFrom = EnvVarSource().apply {
                        fieldRef = ObjectFieldSelector().apply {
                            fieldPath = "metadata.name"
                        }
                    }
                }
            }
        private val kubernetesEnv: EnvVar = AgentEnvName.KUBERNETES.toEnv(true)

        private fun Map<AgentEnvName, Any>.mapToEnvs(): List<EnvVar> = entries.map { (key, value) -> key.toEnv(value) }

        private fun AgentEnvName.toEnv(value: Any): EnvVar = EnvVar().apply {
            this.name = this@toEnv.name
            this.value = value.toString()
        }
    }
}
