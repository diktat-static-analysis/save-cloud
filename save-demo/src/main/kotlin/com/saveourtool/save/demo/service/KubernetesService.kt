package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.DemoAgentConfig
import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.utils.*
import com.saveourtool.save.utils.*

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.client.KubernetesClient
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json

/**
 * Service for interactions with kubernetes
 */
@Service
class KubernetesService(
    private val kc: KubernetesClient,
    private val configProperties: ConfigProperties,
) {
    private val kubernetesSettings = configProperties.kubernetes

    /**
     * @param demo demo entity
     * @param version version of [demo] that should be run in kubernetes pod
     * @return [Mono] of [StringResponse] filled with readable message
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    fun start(demo: Demo, version: String = "manual"): Mono<StringResponse> = Mono.just(demo)
        .logValue(logger::info) {
            "Creating job ${jobNameForDemo(it)}..."
        }
        .flatMap { requestedDemo ->
            try {
                requestedDemo.also { kc.startJob(requestedDemo, kubernetesSettings) }.toMono()
            } catch (kre: KubernetesRunnerException) {
                Mono.error(
                    ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Could not create job for ${jobNameForDemo(requestedDemo)}",
                        kre,
                    )
                )
            }
        }
        .flatMap { requestedDemo ->
            deferredToMono {
                configureDemoAgent(requestedDemo, version)
            }
        }

    /**
     * @param demo demo entity
     * @return list of [StatusDetails]
     */
    fun stop(demo: Demo): List<StatusDetails> {
        logger.info("Stopping job...")
        return kc.getJob(demo, configProperties.kubernetes.namespace).delete()
    }

    /**
     * @param demo demo entity
     * @return deferred filled with [DemoStatus]
     */
    fun getStatus(demo: Demo) = scope.async {
        val url = "${getPodUrl(demo)}/alive"
        when {
            httpClient.get(url).status.isSuccess() -> DemoStatus.RUNNING
            else -> DemoStatus.STOPPED
        }
    }

    private fun configureDemoAgent(demo: Demo, version: String) = scope.async {
        val url = "${getPodUrl(demo)}/configure"
        logger.info("Configuring save-demo-agent by url: $url")
        val requestStatus = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                DemoAgentConfig(
                    configProperties.agentConfig.demoUrl,
                    demo.toDemoConfiguration(version),
                    demo.toRunConfiguration(),
                )
            )
        }.status

        if (requestStatus.isSuccess()) {
            logAndRespond(HttpStatusCode.OK, logger::info) {
                "Job successfully started."
            }
        } else {
            logAndRespond(HttpStatusCode.InternalServerError, logger::error) {
                "Could not configure demo."
            }
        }
    }

    private suspend fun getPodUrl(demo: Demo) = retry(RETRY_TIMES) {
        kc.getJobPodsIps(demo).firstOrNull()
    } ?: throw KubernetesRunnerException("Could not run a job in 60 seconds.")

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesService::class.java)
        private const val RETRY_TIMES = 6

        @Suppress("InjectDispatcher")
        private val scope = CoroutineScope(Dispatchers.Default)
        private val httpClient = HttpClient(Apache) {
            install(ContentNegotiation) {
                val json = Json { ignoreUnknownKeys = true }
                json(json)
            }
        }
    }
}
