package com.saveourtool.save.demo.service

import com.saveourtool.common.demo.DemoAgentConfig
import com.saveourtool.common.demo.DemoRunRequest
import com.saveourtool.common.demo.DemoStatus
import com.saveourtool.common.utils.*
import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.storage.DemoInternalFileStorage
import com.saveourtool.save.demo.utils.*

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.client.KubernetesClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

import java.net.ConnectException

import kotlinx.serialization.json.Json

/**
 * Service for interactions with kubernetes
 */
@Service
@Profile("kubernetes | minikube")
class KubernetesService(
    private val kc: KubernetesClient,
    configProperties: ConfigProperties,
    private val internalFileStorage: DemoInternalFileStorage,
) {
    private val kubernetesSettings = requireNotNull(configProperties.kubernetes) {
        "Kubernetes settings should be passed in order to use Kubernetes"
    }
    private val agentConfig = requireNotNull(configProperties.agentConfig) {
        "Agent settings should be passed in order to use Kubernetes"
    }

    /**
     * @param demo demo entity
     * @param version version of [demo] that should be run in kubernetes pod
     * @return [Mono] of [StringResponse] filled with readable message
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    fun start(demo: Demo, @Suppress("UnusedParameter") version: String = "manual"): Mono<StringResponse> = Mono.fromCallable {
        logger.info("Creating job ${jobNameForDemo(demo)}...")
        try {
            val downloadAgentUrl = internalFileStorage.generateRequiredUrlToDownloadFromContainer(
                DemoInternalFileStorage.saveDemoAgent
            ).toString()
            createConfiguredJob(demo, downloadAgentUrl)
            demo
        } catch (kre: KubernetesRunnerException) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not create job for ${jobNameForDemo(demo)}",
                kre,
            )
        }
    }
        .map { StringResponse.ok("Created container for demo.") }

    /**
     * @param demo demo entity
     * @return list of [StatusDetails]
     */
    fun stop(demo: Demo): List<StatusDetails> {
        logger.info("Stopping job...")
        return kc.getJobByNameInNamespace(demo, kubernetesSettings.agentNamespace).delete()
    }

    /**
     * @param demo demo entity
     * @return [DemoStatus] of [demo] pod
     */
    suspend fun getStatus(demo: Demo): DemoStatus {
        val status = retrySilently(RETRY_TIMES_QUICK, RETRY_DELAY_MILLIS) {
            demoAgentRequestWrapper("/alive", demo) { url ->
                logger.info("Sending GET request with url $url")
                httpClient.get(url).status
            }
        }
        return when {
            status == null -> DemoStatus.STOPPED
            status == HttpStatusCode.OK -> DemoStatus.RUNNING
            status.isSuccess() -> DemoStatus.STARTING
            else -> DemoStatus.ERROR
        }
    }

    /**
     * Get save-demo-agent configuration
     *
     * @param demo [Demo] entity
     * @param version required demo version
     * @return [DemoAgentConfig] corresponding to [Demo] with [version]
     */
    fun getConfiguration(demo: Demo, version: String) = DemoAgentConfig(
        agentConfig.demoUrl,
        demo.toDemoConfiguration(version),
        demo.toRunConfiguration(),
    )

    private fun createConfiguredJob(demo: Demo, downloadAgentUrl: String) {
        val job = getJobObjectForDemo(demo, downloadAgentUrl, kubernetesSettings, agentConfig)
        val createdJob = kc.createResourceOrThrow(job, kubernetesSettings.agentNamespace)
        val service = getServiceObjectForDemo(demo, createdJob, kubernetesSettings)
        kc.createResourceOrThrow(service, kubernetesSettings.agentNamespace)
    }

    /**
     * Performs run request with [demoRunRequest] params to [demo]
     *
     * @param demo [Demo] entity
     * @param demoRunRequest params of [DemoRunRequest]
     * @return [HttpResponse] if response is fetched, null if some error occurred
     */
    suspend fun sendRunRequest(
        demo: Demo,
        demoRunRequest: DemoRunRequest,
    ): HttpResponse? {
        val response = try {
            demoAgentRequestWrapper("/run", demo) { url ->
                logger.info("Sending POST request with url $url")
                httpClient.post {
                    url(url)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(demoRunRequest)
                }
            }
        } catch (connectException: ConnectException) {
            null
        }
        return response
    }

    private suspend fun <R> demoAgentRequestWrapper(
        urlPath: String,
        demo: Demo,
        request: suspend (Url) -> R,
    ): R = demoAgentRequestWrapper(urlPath, demo, kubernetesSettings, request)

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesService::class.java)
        private const val REQUEST_TIMEOUT_MILLIS = 20_000L
        private const val RETRY_DELAY_MILLIS = 500L
        private const val RETRY_TIMES_QUICK = 1
        private val httpClient = HttpClient(Apache) {
            install(ContentNegotiation) {
                val json = Json { ignoreUnknownKeys = true }
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            }
        }
    }
}
