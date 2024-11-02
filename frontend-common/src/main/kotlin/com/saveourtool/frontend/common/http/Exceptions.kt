/**
 * Exception types for frontend
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.http

/**
 * Exception class for HTTP responses until we use a dedicated HTTP client
 *
 * @property status response status
 * @property statusText response status text
 */
data class HttpStatusException(
    val status: Short,
    val statusText: String,
) : RuntimeException() {
    override val message: String = "$status $statusText"
}
