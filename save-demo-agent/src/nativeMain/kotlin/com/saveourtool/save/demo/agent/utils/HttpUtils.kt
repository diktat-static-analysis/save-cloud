/**
 * Utils to send http requests
 */

package com.saveourtool.save.demo.agent.utils

import com.saveourtool.common.demo.DemoAgentConfig
import com.saveourtool.common.utils.*
import com.saveourtool.common.utils.fs
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logError
import com.saveourtool.save.core.logging.logInfo
import com.saveourtool.save.core.logging.logWarn
import com.saveourtool.save.core.utils.runIf

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import okio.Path
import okio.buffer
import okio.use

import kotlin.concurrent.AtomicLong

private const val DOWNLOAD_REQUEST_TIMEOUT_MILLIS = 5 * 60 * 1000L

private val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) { json() }
    install(HttpTimeout)
    install(KubernetesServiceAccountAuthHeaderPlugin)
}

private suspend fun HttpClient.download(url: String, file: Path): Result<HttpResponse> = runCatching {
    prepareGet {
        url(url)
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.OctetStream)
        timeout { requestTimeoutMillis = DOWNLOAD_REQUEST_TIMEOUT_MILLIS }
    }
        .execute { httpResponse ->
            if (httpResponse.status.isSuccess()) {
                val channel: ByteReadChannel = httpResponse.body()
                val totalBytes = AtomicLong(0L)
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_HTTP_BUFFER_SIZE.toLong())
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        val hasAlreadyAnyData = totalBytes.value > 0
                        fs.appendingSink(file, mustExist = hasAlreadyAnyData).buffer().use { it.write(bytes) }
                        totalBytes.addAndGet(bytes.size.toLong())
                        logDebug("Received ${bytes.size} ($totalBytes) bytes out of ${httpResponse.contentLength()} bytes from ${httpResponse.request.url}")
                    }
                }
                if (totalBytes.value == 0L) {
                    error("Downloaded a file from $url but content is empty")
                }
            } else {
                logWarn("Skipping downloading as request is not a success: ${httpResponse.status}")
            }
            httpResponse
        }
}

/**
 * Construct url from environment variable [DemoAgentConfig.DEMO_CONFIGURE_ME_URL_ENV] and get the rest of configuration
 *
 * @return [DemoAgentConfig] fetched from server
 */
suspend fun getConfiguration(): DemoAgentConfig = httpClient.get {
    url(requiredEnv(DemoAgentConfig.DEMO_CONFIGURE_ME_URL_ENV))
}.body()

/**
 * @param fileLabel
 * @param url
 * @param target
 */
suspend fun download(fileLabel: String, url: String, target: Path) {
    logDebug("Will now download $fileLabel from $url into $target")
    val result = demoRequestWrapper {
        httpClient.download(
            url = url,
            file = target,
        )
    }
    if (result.failureOrNotOk()) {
        logError("Couldn't download $fileLabel from $url")
    }

    logInfo("Downloaded $fileLabel (resulting size = ${fs.metadata(target).size} bytes) from $url into $target")
}

private suspend fun demoRequestWrapper(
    requestToBackend: suspend () -> Result<HttpResponse>
): Result<HttpResponse> = requestToBackend().runIf({ failureOrNotOk() }) {
    val reason = if (notOk()) {
        "save-demo returned status ${getOrNull()?.status}"
    } else {
        if (exceptionOrNull() is CancellationException) {
            "Request has been interrupted, terminating."
        } else {
            "save-demo is unreachable, ${exceptionOrNull()?.message}"
        }
    }
    // logError("Cannot process request to save-demo: $reason")
    throw IllegalStateException("Cannot process request to save-demo: $reason")
}
