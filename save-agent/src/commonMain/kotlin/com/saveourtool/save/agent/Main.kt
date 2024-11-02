/**
 * Main entrypoint for SAVE Agent
 */

package com.saveourtool.save.agent

import com.saveourtool.common.agent.*
import com.saveourtool.common.utils.KubernetesServiceAccountAuthHeaderPlugin
import com.saveourtool.common.utils.fs
import com.saveourtool.common.utils.parseConfig
import com.saveourtool.save.agent.utils.*
import com.saveourtool.save.agent.utils.ktorLogger
import com.saveourtool.save.core.config.LogType
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.logging.logType

import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import okio.Path.Companion.toPath

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

internal val json = Json {
    serializersModule = SerializersModule {
        contextual(HeartbeatResponse::class, PolymorphicSerializer(HeartbeatResponse::class))
        polymorphic(HeartbeatResponse::class) {
            subclass(InitResponse::class)
            subclass(NewJobResponse::class)
            subclass(ContinueResponse::class)
            subclass(WaitResponse::class)
            subclass(TerminateResponse::class)
        }
    }
}

fun main() {
    val propertiesFile = "agent.toml".toPath()
    val config: AgentConfiguration = if (fs.exists(propertiesFile)) {
        parseConfig(propertiesFile)
    } else {
        AgentConfiguration.initializeFromEnv()
    }
        .updateFromEnv()
    logType.set(if (config.debug) LogType.ALL else LogType.WARN)
    logDebugCustom("Instantiating save-agent version ${config.info.version} with config $config")

    handleSigterm()

    val httpClient = configureHttpClient(config)

    runBlocking {
        // Launch in a new scope, because we cancel the scope on graceful termination,
        // and `BlockingCoroutine` shouldn't be cancelled.
        launch {
            val saveAgent = SaveAgent(config, httpClient, coroutineScope = this)

            val mainJob = saveAgent.start()
            mainJob.join()
        }
    }
    logInfoCustom("Agent is shutting down")
}

@Suppress("FLOAT_IN_ACCURATE_CALCULATIONS", "MagicNumber")
private fun configureHttpClient(agentConfiguration: AgentConfiguration) = HttpClient {
    install(ContentNegotiation) {
        json(json = json)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = agentConfiguration.requestTimeoutMillis
    }
    install(HttpRequestRetry) {
        retryOnException(maxRetries = agentConfiguration.retry.attempts)
        retryOnServerErrors(maxRetries = agentConfiguration.retry.attempts)
        exponentialDelay(base = agentConfiguration.retry.initialRetryMillis / 1000.0)
        modifyRequest {
            if (retryCount > 1) {
                val reason = response?.status ?: cause?.describe() ?: "Unknown reason"
                logDebugCustom("Retrying request: attempt #$retryCount, reason: $reason")
            }
        }
    }
    install(Logging) {
        logger = ktorLogger
        level = if (agentConfiguration.debug) LogLevel.ALL else LogLevel.INFO
    }
    if (agentConfiguration.kubernetes) {
        install(KubernetesServiceAccountAuthHeaderPlugin)
    }
}
