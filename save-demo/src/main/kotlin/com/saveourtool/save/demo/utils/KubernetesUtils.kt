/**
 * Utils for kubernetes client
 */

package com.saveourtool.save.demo.utils

import com.saveourtool.common.demo.DemoAgentConfig
import com.saveourtool.common.domain.toSdk
import com.saveourtool.common.utils.debug
import com.saveourtool.common.utils.downloadAndRunAgentCommand
import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.config.KubernetesConfig
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.storage.DemoInternalFileStorage

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.dsl.ScalableResource
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory

private const val DEMO_ORG_NAME = "organizationName"
private const val DEMO_PROJ_NAME = "projectName"
private const val DEMO_VERSION = "version"
private const val REPLICAS_PER_DEMO = 1
private const val TTL_AFTER_COMPLETED = 3600

private const val SHA1_PREFIX_SIZE = 6

private val logger = LoggerFactory.getLogger("KubernetesUtils")

/**
 * Create kubernetes resource and process possible exceptions
 *
 * @param resourceToCreate resource (e.g. [Job], [Service] etc) that should be created by [KubernetesClient]
 * @param namespace resource's namespace
 * @return created [HasMetadata] resource
 * @throws KubernetesRunnerException on failed job creation
 */
fun <T : HasMetadata> KubernetesClient.createResourceOrThrow(resourceToCreate: T, namespace: String): T = try {
    logger.debug { "Attempt to create ${resourceToCreate.kind} with the following name: ${resourceToCreate.metadata.name}" }
    resource(resourceToCreate).inNamespace(namespace).create().also {
        logger.info("Created ${resourceToCreate.kind} with the following name: ${resourceToCreate.fullResourceName}")
    }
} catch (kex: KubernetesClientException) {
    throw KubernetesRunnerException("Unable to create ${resourceToCreate.kind} with the following name: ${resourceToCreate.fullResourceName}", kex)
}

/**
 * @param demo demo entity
 * @param namespace namespace to look or the [demo] pod
 * @return [ScalableResource] of [Job]
 */
fun KubernetesClient.getJobByNameInNamespace(demo: Demo, namespace: String): ScalableResource<Job> = batch()
    .v1()
    .jobs()
    .inNamespace(namespace)
    .withName(jobNameForDemo(demo))

private fun ContainerPort.default(port: Int) = apply {
    protocol = "TCP"
    containerPort = port
    name = "agent-server"
}

/**
 * @param demo demo entity
 * @param agentDownloadUrl url to download save-demo-agent.kexe, will be used to get pod start command
 * @param kubernetesSettings kubernetes configuration
 * @param agentConfig configuration that is required to be present on save-demo-agent on startup
 * @return [Job], filled with all the required info, but not created yet
 * @see createResourceOrThrow
 */
@Suppress("NestedBlockDepth")
fun getJobObjectForDemo(
    demo: Demo,
    agentDownloadUrl: String,
    kubernetesSettings: KubernetesConfig,
    agentConfig: ConfigProperties.AgentConfig,
) = Job().apply {
    metadata = ObjectMeta().apply {
        name = jobNameForDemo(demo)
        namespace = kubernetesSettings.agentNamespace
    }
    spec = JobSpec().apply {
        parallelism = REPLICAS_PER_DEMO
        ttlSecondsAfterFinished = TTL_AFTER_COMPLETED
        backoffLimit = kubernetesSettings.podBackoffLimit
        template = PodTemplateSpec().apply {
            spec = PodSpec().apply {
                subdomain = kubernetesSettings.agentSubdomainName
                if (kubernetesSettings.useGvisor) {
                    nodeSelector = mapOf(
                        "gvisor" to "enabled"
                    )
                    runtimeClassName = "gvisor"
                }
                metadata = ObjectMeta().apply {
                    labels = mapOf(
                        DEMO_ORG_NAME to demo.organizationName,
                        DEMO_PROJ_NAME to demo.projectName,
                        DEMO_VERSION to "manual",
                        // "baseImageName" to baseImageName
                        "io.kompose.service" to "save-demo-agent",
                    )
                }
                restartPolicy = "Never"
                containers = listOf(
                    demoAgentContainerSpec(
                        demo.sdk.toSdk().baseImageName(),
                        agentDownloadUrl,
                        demo,
                        kubernetesSettings,
                        agentConfig,
                    )
                )
            }
        }
    }
}

/**
 * @param demo [Demo] entity
 * @param job already created job for ownerResource setting
 * @param kubernetesSettings kubernetes configuration
 * @return [ServicePort], filled with all the required info, but not created yet
 * @see createResourceOrThrow
 */
@Suppress("NestedBlockDepth")
fun getServiceObjectForDemo(
    demo: Demo,
    job: HasMetadata,
    kubernetesSettings: KubernetesConfig,
) = Service().apply {
    metadata = ObjectMeta().apply {
        name = serviceNameForDemo(demo)
        namespace = kubernetesSettings.agentNamespace
    }

    addOwnerReference(job)

    spec = ServiceSpec().apply {
        type = "ClusterIP"
        ports = listOf(
            ServicePort().apply {
                protocol = "TCP"
                port = kubernetesSettings.agentPort
                targetPort = IntOrString(kubernetesSettings.agentPort)
                name = "web-server"
            }
        )
        selector = mapOf(
            DEMO_ORG_NAME to demo.organizationName,
            DEMO_PROJ_NAME to demo.projectName,
            "io.kompose.service" to "save-demo-agent",
        )
    }
}

/**
 * @param demo demo entity
 * @return name of job that is/should be assigned to [demo]
 */
fun jobNameForDemo(demo: Demo) = with(demo) {
    // sha1 is required due to kubernetes naming restrictions - no capital letters are allowed while in save-cloud it is ok
    val sha1 = DigestUtils.sha1Hex("$organizationName$projectName")
    "demo-${organizationName.lowercase()}-${projectName.lowercase()}-${ sha1.take(SHA1_PREFIX_SIZE) }"
}

/**
 * @param demo demo entity
 * @return name of service that is/should be connected with [demo]
 */
fun serviceNameForDemo(demo: Demo) = with(demo) {
    // sha1 is required due to kubernetes naming restrictions - no capital letters are allowed while in save-cloud it is ok
    val sha1 = DigestUtils.sha1Hex("$organizationName$projectName")
    "demo-${organizationName.lowercase().first()}${projectName.lowercase().first()}-${ sha1.take(SHA1_PREFIX_SIZE) }"
}

@Suppress("SameParameterValue")
private fun getConfigureMeUrl(baseUrl: String, demo: Demo, version: String) = with(demo) {
    "$baseUrl/internal/demo/manager/$organizationName/$projectName/configure-me?version=$version"
}

@Suppress("TOO_LONG_FUNCTION")
private fun demoAgentContainerSpec(
    imageName: String,
    agentDownloadUrl: String,
    demo: Demo,
    kubernetesSettings: KubernetesConfig,
    agentConfig: ConfigProperties.AgentConfig,
) = Container().apply {
    name = "save-demo-agent-pod"
    image = imageName
    imagePullPolicy = "IfNotPresent"

    // todo: in later phases should be removed
    val currentlyHardcodedVersion = "manual"
    listOf(
        "KTOR_LOG_LEVEL" to "DEBUG",
        DemoAgentConfig.DEMO_CONFIGURE_ME_URL_ENV to getConfigureMeUrl(agentConfig.demoUrl, demo, currentlyHardcodedVersion),
        DemoAgentConfig.DEMO_ORGANIZATION_ENV to demo.organizationName,
        DemoAgentConfig.DEMO_PROJECT_ENV to demo.projectName,
        DemoAgentConfig.DEMO_VERSION_ENV to currentlyHardcodedVersion,
    )
        .map { (key, envValue) ->
            EnvVar().apply {
                name = key
                value = envValue
            }
        }
        .let { env = it }

    val startupCommand = downloadAndRunAgentCommand(
        agentDownloadUrl, DemoInternalFileStorage.saveDemoAgent,
    )

    command = listOf("sh", "-c", startupCommand)

    ports = listOf(ContainerPort().default(kubernetesSettings.agentPort))

    resources = with(kubernetesSettings) {
        ResourceRequirements().apply {
            requests = buildMap {
                agentCpuLimitations?.let { set("cpu", it.requestsQuantity()) }
                agentMemoryLimitations?.let { set("memory", it.requestsQuantity()) }
                agentEphemeralStorageLimitations?.let { set("ephemeral-storage", it.requestsQuantity()) }
            }

            limits = buildMap {
                agentCpuLimitations?.let { set("cpu", it.limitsQuantity()) }
                agentMemoryLimitations?.let { set("memory", it.limitsQuantity()) }
                agentEphemeralStorageLimitations?.let { set("ephemeral-storage", it.limitsQuantity()) }
            }
        }
    }
}
