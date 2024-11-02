/**
 * Utility methods for HTTP requests
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.common.agent.AgentState
import com.saveourtool.common.utils.createAtomicLong
import com.saveourtool.common.utils.failureOrNotOk
import com.saveourtool.common.utils.fs
import com.saveourtool.common.utils.notOk
import com.saveourtool.save.agent.SaveAgent
import com.saveourtool.save.core.logging.logWarn
import com.saveourtool.save.core.utils.runIf

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.client.utils.DEFAULT_HTTP_BUFFER_SIZE
import io.ktor.http.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import okio.Path
import okio.buffer
import okio.use

import kotlinx.coroutines.CancellationException

/**
 * Attempt to send execution data to backend.
 *
 * @param requestToBackend
 * @return a [Result] wrapping response
 */
internal suspend fun SaveAgent.processRequestToBackend(
    requestToBackend: suspend () -> HttpResponse
): Result<HttpResponse> = processRequestToBackendWrapped {
    runCatching { requestToBackend() }
}

/**
 * Attempt to send execution data to backend.
 *
 * @param requestToBackend
 * @return a [Result] wrapping response
 */
internal suspend fun SaveAgent.processRequestToBackendWrapped(
    requestToBackend: suspend () -> Result<HttpResponse>
): Result<HttpResponse> = requestToBackend().runIf({ failureOrNotOk() }) {
    val reason = if (notOk()) {
        state.set(AgentState.BACKEND_FAILURE)
        "Backend returned status ${getOrNull()?.status}"
    } else {
        state.set(AgentState.BACKEND_UNREACHABLE)
        if (exceptionOrNull() is CancellationException) {
            "Request has been interrupted, switching to ${AgentState.BACKEND_UNREACHABLE} state"
        } else {
            "Backend is unreachable, ${exceptionOrNull()?.message}"
        }
    }
    logErrorCustom("Cannot process request to backed: $reason")
    this
}

/**
 * Perform a GET request to [url] (optionally with body [body] that will be serialized as JSON),
 * accepting application/octet-stream and return result wrapping [HttpResponse]
 *
 * @param url
 * @param file
 * @return result wrapping [HttpResponse]
 */
internal suspend fun HttpClient.download(url: String, file: Path): Result<HttpResponse> = runCatching {
    prepareGet {
        url(url)
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.OctetStream)
    }
        .execute { httpResponse ->
            if (httpResponse.status.isSuccess()) {
                val channel: ByteReadChannel = httpResponse.body()
                val totalBytes = createAtomicLong(0L)
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_HTTP_BUFFER_SIZE.toLong())
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        val hasAlreadyAnyData = totalBytes.get() > 0
                        fs.appendingSink(file, mustExist = hasAlreadyAnyData)
                            .buffer()
                            .use {
                                it.write(bytes)
                            }
                        totalBytes.addAndGet(bytes.size.toLong())
                        logDebugCustom("Received ${bytes.size} (${totalBytes.get()}) bytes out of ${httpResponse.contentLength()} bytes from ${httpResponse.request.url}")
                    }
                }
                if (totalBytes.get() == 0L) {
                    error("Downloaded a file from $url but content is empty")
                }
            } else {
                logWarn("Skipping downloading as request is not a success")
            }
            httpResponse
        }
}
