@file:JvmName("HttpUtils")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.common.utils

import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.system.measureNanoTime
import kotlin.time.Duration.Companion.nanoseconds

private const val SERVER_TIMING = "Server-Timing"

/**
 * [`no-transform`](https://www.rfc-editor.org/rfc/rfc7234#section-5.2.2.4)
 * is absolutely necessary, so that SSE stream passes through the
 * [proxy](https://github.com/chimurai/http-proxy-middleware) without
 * [compression](https://github.com/expressjs/compression).
 *
 * Otherwise, the front-end receives all the events at once, and only
 * after the response body is fully written.
 *
 * See
 * [this comment](https://github.com/facebook/create-react-app/issues/7847#issuecomment-544715338)
 * for details:
 *
 * The rest of the `Cache-Control` header is merely what _Spring_ sets by default.
 */
private val cacheControlValues: Array<out String> = arrayOf(
    "no-cache",
    "no-store",
    "no-transform",
    "max-age=0",
    "must-revalidate",
)

/**
 * Lazy HTTP response.
 */
typealias LazyResponse<T> = () -> T

/**
 * Lazy HTTP response with timings.
 */
typealias LazyResponseWithTiming<T> = () -> ResponseWithTiming<T>

/**
 * Adds required [HttpHeaders.CACHE_CONTROL] to support [org.springframework.http.MediaType.APPLICATION_NDJSON_VALUE]
 *
 * @return builder [this]
 */
fun ResponseEntity.BodyBuilder.cacheControlForNdjson() = cacheControl(
    CacheControl
        .noStore()  // no-cache and max-age cannot be set
        .noTransform()
        .mustRevalidate()
)

/**
 * Adds support for the
 * [`Server-Timing`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing)
 * header.
 */
private fun <T : Any> T.withTimings(vararg timings: ServerTiming): ResponseWithTiming<T> =
        ResponseWithTiming(this, *timings)

/**
 * Adds support for the
 * [`Server-Timing`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing)
 * header.
 */
private fun <T : Any> LazyResponse<T>.withTimings(vararg timings: ServerTiming): LazyResponseWithTiming<T> =
        {
            val response: T
            val nanos = measureNanoTime {
                response = this()
            }

            when {
                timings.isEmpty() -> response.withTimings(
                    ServerTiming(
                        "total",
                        "Total server time",
                        nanos.nanoseconds
                    )
                )

                else -> response.withTimings(*timings)
            }
        }

/**
 * @param timings the server-side timings.
 * @param lazyResponse the lazy HTTP response.
 * @return [lazyResponse] wrapped with HTTP headers.
 */
@OptIn(ExperimentalContracts::class)
fun <T : Any> withHttpHeaders(
    vararg timings: ServerTiming,
    lazyResponse: LazyResponse<T>,
): ResponseEntity<T> {
    contract {
        callsInPlace(lazyResponse, EXACTLY_ONCE)
    }

    return withHttpHeaders(lazyResponse.withTimings(*timings))
}

/**
 * Evaluates lazyResponse and returns an `HTTP 200 OK` with `Cache-Control` and
 * optional `Server-Timing` headers.
 *
 * @return [lazyResponse] wrapped with HTTP headers.
 */
@OptIn(ExperimentalContracts::class)
private fun <T : Any> withHttpHeaders(
    lazyResponse: LazyResponseWithTiming<T>,
): ResponseEntity<T> {
    contract {
        callsInPlace(lazyResponse, EXACTLY_ONCE)
    }

    val response = lazyResponse()

    return ResponseEntity(
        response.response,
        httpHeaders(*response.timings),
        OK,
    )
}

/**
 * @return HTTP headers with `Cache-Control` and optional `Server-Timing`.
 */
private fun httpHeaders(vararg timings: ServerTiming): HttpHeaders =
        httpHeaders { headers ->
            headers[CACHE_CONTROL] = cacheControlValues.joinToString()
            if (timings.isNotEmpty()) {
                headers[SERVER_TIMING] = timings.joinToString()
            }
        }

/**
 * @return HTTP headers initialized with [init].
 */
@OptIn(ExperimentalContracts::class)
private fun httpHeaders(init: (headers: HttpHeaders) -> Unit): HttpHeaders {
    contract {
        callsInPlace(init, EXACTLY_ONCE)
    }

    return HttpHeaders().also { headers ->
        init(headers)
    }
}
