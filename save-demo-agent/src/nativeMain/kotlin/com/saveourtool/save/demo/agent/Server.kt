/**
 * save-demo-agent server configuration
 */

package com.saveourtool.save.demo.agent

import com.saveourtool.common.demo.DemoAgentConfig
import com.saveourtool.common.demo.DemoResult
import com.saveourtool.common.demo.DemoRunRequest
import com.saveourtool.common.demo.ServerConfiguration
import com.saveourtool.common.utils.retry
import com.saveourtool.save.core.config.LogType
import com.saveourtool.save.core.logging.*
import com.saveourtool.save.demo.agent.utils.getConfiguration
import com.saveourtool.save.demo.agent.utils.setupEnvironment

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*

import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

private const val RETRY_TIMES = 5

private fun Application.getConfigurationOnStartup(
    retryTimes: Int = RETRY_TIMES,
    updateConfig: (DemoAgentConfig) -> Unit,
) = environment.monitor.subscribe(ApplicationStarted) { application ->
    application.launch {
        logDebug("Fetching tool configuration for save-demo-agent...")
        retry(retryTimes) { iteration ->
            logTrace("$iteration attempts left for demo configuration.")
            getConfiguration()
        }
            .also { (_, errors) ->
                val prettyErrors = errors.joinToString("\n", prefix = "\t- ") { error ->
                    error.describe()
                }
                logError("${errors.size} errors occurred during configuration fetch:\n$prettyErrors")
            }
            .let { (config, _) ->
                logDebug("Configuration successfully fetched.")
                config
            }
            ?.also(updateConfig)
            ?.let { config ->
                logTrace("Configuration successfully updated.")
                setupEnvironment(config.demoUrl, config.setupShTimeoutMillis, config.demoConfiguration)
            }
            ?: run { logWarn("Could not prepare save-demo-agent, expecting /setup call.") }
    }
}

private fun Routing.alive(configuration: CompletableDeferred<DemoAgentConfig>) = get("/alive") {
    call.respond(if (configuration.isCompleted) {
        HttpStatusCode.OK
    } else {
        HttpStatusCode.Created
    })
}

private fun Routing.configure(updateConfig: (DemoAgentConfig) -> Unit) = post("/setup") {
    val config = call.receive<DemoAgentConfig>().also(updateConfig)
    logInfo("Agent has received configuration.")
    try {
        setupEnvironment(config.demoUrl, config.setupShTimeoutMillis, config.demoConfiguration)
        call.respondText(
            "Agent is set up.",
            status = HttpStatusCode.OK,
        )
    } catch (exception: IllegalStateException) {
        call.respondText(
            exception.message ?: "Internal agent error.",
            status = HttpStatusCode.InternalServerError,
        )
    }
}

@OptIn(ExperimentalTime::class)
private fun Routing.run(config: CompletableDeferred<DemoAgentConfig>) = post("/run") {
    if (!config.isCompleted) {
        logError("Cannot run demo as it was not configured yet.")
        call.respond(HttpStatusCode.FailedDependency)
    }
    val runRequest: DemoRunRequest = call.receive()
    logDebug("Running demo on code [${runRequest.codeLines}]")
    val result: DemoResult = measureTimedValue {
        runDemo(runRequest, config)
    }
        .let {
            logDebug("Time taken: ${it.duration}")
            it.value
        }
    call.respond(result)
}

/**
 * Configure ktor server (runs on [CIO] engine) with [serverConfiguration]
 *
 * @param serverConfiguration information required to configure ktor server
 * @param skipStartupConfiguration if true, startup configuration will be skipped
 * @return [CIOApplicationEngine]
 */
@Suppress("ExtractKtorModule")
fun server(serverConfiguration: ServerConfiguration, skipStartupConfiguration: Boolean = false) = embeddedServer(CIO, port = serverConfiguration.port.toInt()) {
    val deferredConfig: CompletableDeferred<DemoAgentConfig> = CompletableDeferred()
    logType.set(LogType.ALL)
    if (!skipStartupConfiguration) {
        getConfigurationOnStartup { deferredConfig.complete(it) }
    }
    install(ContentNegotiation) { json() }
    routing {
        alive(deferredConfig)
        configure { deferredConfig.complete(it) }
        run(deferredConfig)
    }
}
