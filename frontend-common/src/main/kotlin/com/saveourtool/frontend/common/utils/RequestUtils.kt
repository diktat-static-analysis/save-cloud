/**
 * Kotlin/JS utilities for Fetch API
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.utils

import com.saveourtool.common.coroutines.flow.decodeToString
import com.saveourtool.common.v1
import com.saveourtool.frontend.common.components.RequestStatusContext
import com.saveourtool.frontend.common.components.requestStatusContext
import com.saveourtool.frontend.common.http.HttpStatusException

import js.buffer.ArrayBuffer
import js.core.jso
import js.promise.asDeferred
import js.typedarrays.Int8Array
import js.typedarrays.Uint8Array
import org.w3c.dom.url.URLSearchParams
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import web.streams.ReadableStream
import web.streams.ReadableStreamReadValueResult

import kotlin.js.Promise
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

val apiUrl = "${window.location.origin}/api/$v1"
val demoApiUrl = "${window.location.origin}/api/demo"
val cpgDemoApiUrl = "${window.location.origin}/api/cpg"

val jsonHeaders = Headers()
    .withAcceptJson()
    .withContentTypeJson()

/**
 * The chunk of data read from the body of an HTTP response.
 *
 * @param T the type of the data (usually a byte array).
 */
private typealias ResultAsync<T> = Promise<ReadableStreamReadValueResult<T>>

/**
 * Interface for objects that have access to [requestStatusContext]
 */
interface WithRequestStatusContext {
    /**
     * Coroutine used for processing [setLoadingCounter]
     */
    val coroutineScope: CoroutineScope

    /**
     * @param response
     */
    fun setResponse(response: Response)

    /**
     * @param isNeedRedirect
     * @param response
     */
    fun setRedirectToFallbackView(isNeedRedirect: Boolean, response: Response)

    /**
     * @param transform
     */
    fun setLoadingCounter(transform: (oldValue: Int) -> Int)
}

/**
 * @return [this] headers with `content-type` for JSON
 */
fun Headers.withContentTypeJson() = apply {
    set("Content-Type", "application/json")
}

/**
 * @return [this] headers with `accept` for JSON
 */
fun Headers.withAcceptJson() = apply {
    set("Accept", "application/json")
}

/**
 * @return [this] headers with `accept` for NDJSON
 */
fun Headers.withAcceptNdjson() = apply {
    set("Accept", "application/x-ndjson")
}

/**
 * @return [this] headers with `accept` for octet-stream (bytes)
 */
fun Headers.withAcceptOctetStream() = apply {
    set("Accept", "application/octet-stream")
}

/**
 * Gets errors from the back-end (_Spring Boot_ returns errors in the `message`
 * part of JSON).
 *
 * @return the `message` part of JSON response, or "null" if the `message` is
 *   `null`.
 * @see Response.unpackMessageOrNull
 * @see Response.unpackMessageOrHttpStatus
 */
suspend fun Response.unpackMessage(): String = unpackMessageOrNull().toString()

/**
 * Gets errors from the back-end (_Spring Boot_ returns errors in the `message`
 * part of JSON).
 *
 * @return the `message` part of JSON response (may well be `null`).
 * @see Response.unpackMessage
 * @see Response.unpackMessageOrHttpStatus
 */
suspend fun Response.unpackMessageOrNull(): String? = decodeFieldFromJsonStringOrNull("message")

/**
 * Gets errors from the back-end (_Spring Boot_ returns errors in the `message`
 * part of JSON).
 *
 * @return the `message` part of JSON response, or the HTTP status line (in the
 *   form of "HTTP 418 I'm a teapot").
 * @see Response.unpackMessage
 * @see Response.unpackMessageOrNull
 */
suspend fun Response.unpackMessageOrHttpStatus(): String = unpackMessageOrNull() ?: "HTTP $status $statusText"

/**
 * Perform a mapping operation on a [Response] if it's status is OK or throw an exception otherwise.
 *
 * @param map mapping function
 * @return mapped result
 * @throws IllegalStateException if response status is not OK
 */
suspend fun <T> Response.unsafeMap(map: suspend (Response) -> T) = if (this.ok) {
    map(this)
} else {
    throw HttpStatusException(status, statusText)
}

/**
 * Read [this] Response body as text and deserialize it using [Json] as type [T]
 *
 * @return response body deserialized as [T]
 */
suspend inline fun <reified T> Response.decodeFromJsonString() = Json.decodeFromString<T>(text().await())

/**
 * Read [this] Response body as text and deserialize it using [Json] to [JsonObject] and take [fieldName]
 *
 * @param fieldName
 * @return content of [fieldName] taken from response body
 * @throws IllegalArgumentException if [fieldName] is not present in response body
 */
suspend inline fun Response.decodeFieldFromJsonString(fieldName: String): String = decodeFieldFromJsonStringOrNull(fieldName)
    ?: throw IllegalArgumentException("Not found field \'$fieldName\' in response body")

/**
 * Read [this] Response body as text and deserialize it using [Json] to [JsonObject] and take [fieldName]
 *
 * @param fieldName
 * @return content of [fieldName] taken from response body or null if [fieldName] is not present
 */
suspend inline fun Response.decodeFieldFromJsonStringOrNull(fieldName: String): String? = text().await()
    .let { Json.parseToJsonElement(it) }
    .let { it as? JsonObject }
    ?.let { it[fieldName] }
    ?.let { it as? JsonPrimitive }
    ?.content

/**
 * @return content of [this] with type [T] encoded as JSON
 */
inline fun <reified T : Any> T.toJsonBody(): String = Json.encodeToString(this)

/**
 * Perform GET request from a class component. See [request] for parameter description.
 *
 * @return [Response]
 */
@Suppress("KDOC_WITHOUT_PARAM_TAG")
suspend fun ComponentWithScope<*, *>.get(
    url: String,
    headers: Headers,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
): Response = get<dynamic>(
    url = url,
    headers = headers,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Performs a `GET` request from a class component. See [request] for parameter
 * description.
 *
 * @param T the type of [request parameters][params] (by default, use `dynamic`).
 * @param url the request URL (may or may not end with `?`).
 * @param params the request parameters, the default is an empty object (`jso {}`).
 * @return the HTTP response _promise_, see
 *   [`Response`](https://developer.mozilla.org/en-US/docs/Web/API/Response).
 *   The response, even a successful one, can also be processed using
 *   [responseHandler].
 * @see request
 */
@Suppress(
    "KDOC_WITHOUT_PARAM_TAG",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
)
suspend fun <T : Any> ComponentWithScope<*, *>.get(
    url: String,
    params: T = jso { },
    headers: Headers,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
): Response = request(
    url = url.withParams(params),
    method = "GET",
    headers = headers,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Perform POST request from a class component. See [request] for parameter description.
 *
 * @return [Response]
 */
@Suppress("KDOC_WITHOUT_PARAM_TAG")
suspend fun ComponentWithScope<*, *>.post(
    url: String,
    headers: Headers,
    body: dynamic,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
): Response = post<dynamic>(
    url = url,
    headers = headers,
    body = body,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Performs a `POST` request from a class component.
 *
 * @param T the type of [request parameters][params] (by default, use `dynamic`).
 * @param url the request URL (may or may not end with `?`).
 * @param params the request parameters, the default is an empty object (`jso {}`).
 * @param headers the HTTP request headers.
 *   Use [jsonHeaders] for the standard `Accept` and `Content-Type` headers.
 * @param responseHandler the response handler to be invoked.
 *   The default implementation is to show the modal dialog if the HTTP response
 *   code is not in the range of 200..299 (i.e. [Response.ok] is `false`).
 *   Alternatively, a custom or a [noopResponseHandler] can be used, or the
 *   return value can be inspected directly.
 * @return the HTTP response _promise_, see
 *   [`Response`](https://developer.mozilla.org/en-US/docs/Web/API/Response).
 *   The response, even a successful one, can also be processed using
 *   [responseHandler].
 */
@Suppress(
    "LongParameterList",
    "TOO_MANY_PARAMETERS",
    "KDOC_WITHOUT_PARAM_TAG",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
)
suspend fun <T : Any> ComponentWithScope<*, *>.post(
    url: String,
    params: T = jso { },
    headers: Headers,
    body: dynamic,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
): Response = request(
    url = url.withParams(params),
    method = "POST",
    headers = headers,
    body = body,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Perform DELETE request from a class component. See [request] for parameter description.
 *
 * @return [Response]
 */
@Suppress("KDOC_WITHOUT_PARAM_TAG")
suspend fun ComponentWithScope<*, *>.delete(
    url: String,
    headers: Headers,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
): Response = delete<dynamic>(
    url = url,
    headers = headers,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Performs a `DELETE` request from a class component.
 *
 * @param T the type of [request parameters][params] (by default, use `dynamic`).
 * @param url the request URL (may or may not end with `?`).
 * @param params the request parameters, the default is an empty object (`jso {}`).
 * @param headers the HTTP request headers.
 *   Use [jsonHeaders] for the standard `Accept` and `Content-Type` headers.
 * @param responseHandler the response handler to be invoked.
 *   The default implementation is to show the modal dialog if the HTTP response
 *   code is not in the range of 200..299 (i.e. [Response.ok] is `false`).
 *   Alternatively, a custom or a [noopResponseHandler] can be used, or the
 *   return value can be inspected directly.
 * @return the HTTP response _promise_, see
 *   [`Response`](https://developer.mozilla.org/en-US/docs/Web/API/Response).
 *   The response, even a successful one, can also be processed using
 *   [responseHandler].
 */
@Suppress(
    "KDOC_WITHOUT_PARAM_TAG",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
)
suspend fun <T : Any> ComponentWithScope<*, *>.delete(
    url: String,
    params: T = jso { },
    headers: Headers,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
): Response = request(
    url = url.withParams(params),
    method = "DELETE",
    headers = headers,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Perform GET request from a functional component
 *
 * @return [Response]
 */
@Suppress(
    "KDOC_WITHOUT_PARAM_TAG",
    "EXTENSION_FUNCTION_WITH_CLASS",
)
suspend fun WithRequestStatusContext.get(
    url: String,
    headers: Headers,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
): Response = get<dynamic>(
    url = url,
    headers = headers,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Performs a `GET` request from a functional component. See [request] for
 * parameter description.
 *
 * @param T the type of [request parameters][params] (by default, use `dynamic`).
 * @param url the request URL (may or may not end with `?`).
 * @param params the request parameters, the default is an empty object (`jso {}`).
 * @return the HTTP response _promise_, see
 *   [`Response`](https://developer.mozilla.org/en-US/docs/Web/API/Response).
 *   The response, even a successful one, can also be processed using
 *   [responseHandler].
 * @see request
 */
@Suppress(
    "KDOC_WITHOUT_PARAM_TAG",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
    "EXTENSION_FUNCTION_WITH_CLASS",
)
suspend fun <T : Any> WithRequestStatusContext.get(
    url: String,
    params: T = jso { },
    headers: Headers,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
): Response = request(
    url = url.withParams(params),
    method = "GET",
    headers = headers,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Perform POST request from a functional component
 *
 * @return [Response]
 */
@Suppress(
    "KDOC_WITHOUT_PARAM_TAG",
    "EXTENSION_FUNCTION_WITH_CLASS",
)
suspend fun WithRequestStatusContext.post(
    url: String,
    headers: Headers,
    body: dynamic,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
): Response = post<dynamic>(
    url = url,
    headers = headers,
    body = body,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Performs a `POST` request from a functional component.
 *
 * @param T the type of [request parameters][params] (by default, use `dynamic`).
 * @param url the request URL (may or may not end with `?`).
 * @param params the request parameters, the default is an empty object (`jso {}`).
 * @param headers the HTTP request headers.
 *   Use [jsonHeaders] for the standard `Accept` and `Content-Type` headers.
 * @param loadingHandler use either [WithRequestStatusContext.loadingHandler],
 *   or [noopLoadingHandler].
 * @param responseHandler the response handler to be invoked.
 *   The default implementation is to show the modal dialog if the HTTP response
 *   code is not in the range of 200..299 (i.e. [Response.ok] is `false`).
 *   Alternatively, a custom or a [noopResponseHandler] can be used, or the
 *   return value can be inspected directly.
 * @return the HTTP response _promise_, see
 *   [`Response`](https://developer.mozilla.org/en-US/docs/Web/API/Response).
 *   The response, even a successful one, can also be processed using
 *   [responseHandler].
 */
@Suppress(
    "LongParameterList",
    "TOO_MANY_PARAMETERS",
    "KDOC_WITHOUT_PARAM_TAG",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
    "EXTENSION_FUNCTION_WITH_CLASS",
)
suspend fun <T : Any> WithRequestStatusContext.post(
    url: String,
    params: T = jso { },
    headers: Headers,
    body: dynamic,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
): Response = request(
    url = url.withParams(params),
    method = "POST",
    headers = headers,
    body = body,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Perform a `DELETE` request from a functional component.
 *
 * @param url the request URL.
 * @param headers the HTTP request headers.
 *   Use [jsonHeaders] for the standard `Accept` and `Content-Type` headers.
 * @param loadingHandler use either [WithRequestStatusContext.loadingHandler],
 *   or [noopLoadingHandler].
 * @param responseHandler the response handler to be invoked.
 *   The default implementation is to show the modal dialog if the HTTP response
 *   code is not in the range of 200..299 (i.e. [Response.ok] is `false`).
 *   Alternatively, a custom or a [noopResponseHandler] can be used, or the
 *   return value can be inspected directly.
 * @return the HTTP response _promise_, see
 *   [`Response`](https://developer.mozilla.org/en-US/docs/Web/API/Response).
 *   The response, even a successful one, can also be processed using
 *   [responseHandler].
 * @see jsonHeaders
 * @see undefined
 * @see WithRequestStatusContext.loadingHandler
 * @see noopLoadingHandler
 * @see noopResponseHandler
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
suspend fun WithRequestStatusContext.delete(
    url: String,
    headers: Headers,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
): Response = delete<dynamic>(
    url = url,
    headers = headers,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Performs a `DELETE` request from a functional component.
 *
 * @param T the type of [request parameters][params] (by default, use `dynamic`).
 * @param url the request URL (may or may not end with `?`).
 * @param params the request parameters, the default is an empty object (`jso {}`).
 * @param headers the HTTP request headers.
 *   Use [jsonHeaders] for the standard `Accept` and `Content-Type` headers.
 * @param loadingHandler use either [WithRequestStatusContext.loadingHandler],
 *   or [noopLoadingHandler].
 * @param responseHandler the response handler to be invoked.
 *   The default implementation is to show the modal dialog if the HTTP response
 *   code is not in the range of 200..299 (i.e. [Response.ok] is `false`).
 *   Alternatively, a custom or a [noopResponseHandler] can be used, or the
 *   return value can be inspected directly.
 * @return the HTTP response _promise_, see
 *   [`Response`](https://developer.mozilla.org/en-US/docs/Web/API/Response).
 *   The response, even a successful one, can also be processed using
 *   [responseHandler].
 */
@Suppress(
    "KDOC_WITHOUT_PARAM_TAG",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
    "EXTENSION_FUNCTION_WITH_CLASS",
)
suspend fun <T : Any> WithRequestStatusContext.delete(
    url: String,
    params: T = jso { },
    headers: Headers,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
): Response = request(
    url = url.withParams(params),
    method = "DELETE",
    headers = headers,
    loadingHandler = loadingHandler,
    responseHandler = responseHandler,
)

/**
 * Handler that allows to show loading modal
 *
 * @param request REST API method
 * @return [Response] received with [request]
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
suspend fun WithRequestStatusContext.loadingHandler(request: suspend () -> Response) = run {
    setLoadingCounter { it + 1 }
    val deferred = coroutineScope.async { request() }
    deferred.invokeOnCompletion {
        setLoadingCounter { it - 1 }
    }
    deferred.await()
}

/**
 * @return true if given [Response] has 409 code, false otherwise
 */
@Suppress("MAGIC_NUMBER")
fun Response.isConflict(): Boolean = this.status == 409.toShort()

/**
 * @return true if given [Response] has 409 code, false otherwise
 */
@Suppress("MAGIC_NUMBER")
fun Response.isBadRequest(): Boolean = this.status == 400.toShort()

/**
 * @return true if given [Response] has 401 code, false otherwise
 */
@Suppress("MAGIC_NUMBER")
fun Response.isUnauthorized(): Boolean = this.status == 401.toShort()

/**
 * Reads the HTTP response body as a flow of strings.
 *
 * @return the string  flow produced from the body of this HTTP response.
 * @see Response.inputStream
 */
suspend fun Response.readLines(): Flow<String> = inputStream().decodeToString()

/**
 * Appends the [parameters][params] to this URL.
 *
 * @param params
 * @return final URL with appended parameters after a `question` symbol
 */
fun <T : Any> String.withParams(params: T): String {
    val paramString = URLSearchParams(params).toString()

    return when {
        paramString.isEmpty() -> this
        endsWith('?') -> this + paramString
        contains('?') -> "$this&$paramString"
        else -> "$this?$paramString"
    }
}

/**
 * If this component has context, set [response] in this context. Otherwise, fallback to redirect.
 *
 * @param response
 */
@Suppress("MAGIC_NUMBER")
fun ComponentWithScope<*, *>.classComponentResponseHandler(
    response: Response,
) {
    val hasResponseContext = this.asDynamic().context is RequestStatusContext
    if (hasResponseContext) {
        this.withModalResponseHandler(response, false)
    }
}

/**
 * @param response
 */
fun ComponentWithScope<*, *>.classComponentRedirectOnFallbackResponseHandler(
    response: Response,
) {
    val hasResponseContext = this.asDynamic().context is RequestStatusContext
    if (hasResponseContext) {
        this.withModalResponseHandler(response, true)
    }
}

/**
 * Handler that allows to show loading modal
 *
 * @param request REST API method
 * @return [Response] received with [request]
 */
@Suppress("MAGIC_NUMBER")
suspend fun ComponentWithScope<*, *>.classLoadingHandler(request: suspend () -> Response): Response {
    val hasRequestStatusContext = this.asDynamic().context is RequestStatusContext
    if (hasRequestStatusContext) {
        return this.loadingHandler(request)
    }
    return request()
}

/**
 * If this component has context, set [response] in this context. Otherwise, fallback to redirect.
 *
 * @param response
 */
@Suppress("MAGIC_NUMBER")
fun ComponentWithScope<*, *>.classComponentResponseHandlerWithValidation(
    response: Response,
) {
    val hasResponseContext = this.asDynamic().context is RequestStatusContext
    if (hasResponseContext) {
        this.responseHandlerWithValidation(response)
    }
}

/**
 * @param response
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS", "MAGIC_NUMBER")
fun WithRequestStatusContext.responseHandlerWithValidation(
    response: Response,
) {
    if (!response.ok && !response.isConflict()) {
        setResponse(response)
    }
}

/**
 * Handler that allows to show loading modal
 *
 * @param request REST API method
 * @return [Response] received with [request]
 */
private suspend fun ComponentWithScope<*, *>.loadingHandler(request: suspend () -> Response) = run {
    val context: RequestStatusContext = this.asDynamic().context
    context.setLoadingCounter { it + 1 }
    val deferred = scope.async { request() }
    deferred.invokeOnCompletion {
        context.setLoadingCounter { it - 1 }
    }
    deferred.await()
}

@Suppress("MAGIC_NUMBER")
private fun ComponentWithScope<*, *>.withModalResponseHandler(
    response: Response,
    isNeedRedirect: Boolean
) {
    if (!response.ok) {
        val statusContext: RequestStatusContext = this.asDynamic().context
        statusContext.setRedirectToFallbackView(isNeedRedirect && response.status == 404.toShort())
        statusContext.setResponse.invoke(response)
    }
}

@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
private fun WithRequestStatusContext.withModalResponseHandler(
    response: Response,
) {
    if (!response.ok) {
        setResponse(response)
    }
}

private fun ComponentWithScope<*, *>.responseHandlerWithValidation(
    response: Response,
) {
    if (!response.ok && !response.isConflict()) {
        val statusContext: RequestStatusContext = this.asDynamic().context
        statusContext.setRedirectToFallbackView(response.isUnauthorized())
        statusContext.setResponse.invoke(response)
    }
}

/**
 * Reads the HTTP response body as a byte flow.
 *
 * @return the byte flow produced from the body of this HTTP response.
 * @see Response.readLines
 */
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun Response.inputStream(): Flow<Byte> {
    val reader = body.unsafeCast<ReadableStream<Uint8Array>>().getReader()

    return flow {
        /*
         * Read the response body in byte chunks, emitting each chunk as it's
         * available.
         */
        while (true) {
            val resultAsync: ResultAsync<Uint8Array> = reader
                .read()
                .unsafeCast<ResultAsync<Uint8Array>>()

            val result = resultAsync.await()

            val jsBytes: Uint8Array = result.value

            if (jsBytes == undefined || result.done) {
                break
            }

            emit(jsBytes.asByteArray())
        }
    }
        .flatMapConcat { bytes ->
            /*
             * Concatenate all chunks into a byte flow.
             */
            bytes.asSequence().asFlow()
        }
        .onCompletion {
            /*
             * Wait for the stream to get closed.
             */
            reader.closed.asDeferred().await()

            /*
             * Release the reader's lock on the stream.
             * See https://developer.mozilla.org/en-US/docs/Web/API/ReadableStreamDefaultReader/releaseLock
             */
            reader.releaseLock()
        }
}

/**
 * Converts this [Uint8Array] (most probably obtained by reading an HTTP
 * response body) to the standard [ByteArray].
 *
 * Conversion from an `Uint8Array` to an `Int8Array` is necessary &mdash;
 * otherwise, non-ASCII data will get corrupted.
 *
 * @return the converted instance.
 */
@Suppress("UnsafeCastFromDynamic")
private fun Uint8Array.asByteArray(): ByteArray = Int8Array(
    buffer = buffer.unsafeCast<ArrayBuffer>(),
    byteOffset = byteOffset,
    length = length,
)
    .asDynamic()

/**
 * Perform an HTTP request using Fetch API. Suspending function that returns a [Response] - a JS promise with result.
 *
 * @param url request URL
 * @param method HTTP request method
 * @param headers HTTP headers
 * @param body request body
 * @param credentials [RequestCredentials] for fetch API
 * @param loadingHandler
 * @param responseHandler
 * @return [Response] instance
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
suspend fun request(
    url: String,
    method: String,
    headers: Headers,
    body: dynamic = undefined,
    credentials: RequestCredentials? = undefined,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = ::noopResponseHandler,
): Response = loadingHandler {
    window.fetch(
        input = url,
        RequestInit(
            method = method,
            headers = headers,
            body = body,
            credentials = credentials,
        )
    )
        .await()
}
    .also { response ->
        if (responseHandler != undefined) {
            responseHandler(response)
        }
    }

/**
 * Handler that allows to skip loading modal
 *
 * @param request REST API method
 * @return [Response] received with [request]
 */
suspend fun noopLoadingHandler(request: suspend () -> Response) = request()

/**
 * Can be used to explicitly specify, that response will be handled is a custom way
 *
 * @param response
 * @return Unit
 */
fun noopResponseHandler(@Suppress("UNUSED_PARAMETER") response: Response) = Unit
