/**
 * Utils to check results of http requests
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.common.utils

import io.ktor.client.plugins.api.*
import io.ktor.client.statement.*
import io.ktor.http.*
import okio.Path.Companion.toPath
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Kubernetes token header name
 */
const val SA_HEADER_NAME = "X-Service-Account-Token"

/**
 * Kubernetes service account based authentication plugin for ktor client.
 *
 * Basically it:
 *  * keeps control over token ([KubernetesServiceAccountAuthHeaderPluginConfig.tokenPath]);
 *  * refreshes it every [KubernetesServiceAccountAuthHeaderPluginConfig.expirationTime];
 *  * appends required [KubernetesServiceAccountAuthHeaderPluginConfig.headerName] header to every request.
 */
@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
val KubernetesServiceAccountAuthHeaderPlugin = createClientPlugin(
    "KubernetesServiceAccountAuthHeaderPlugin",
    ::KubernetesServiceAccountAuthHeaderPluginConfig,
) {
    val token = ExpiringValueWrapper(pluginConfig.expirationTime) {
        fs.read(pluginConfig.tokenPath.toPath()) { readUtf8() }
    }
    onRequest { request, _ ->
        request.headers.append(SA_HEADER_NAME, token.getValue())
    }
}

/**
 * Configuration for [KubernetesServiceAccountAuthHeaderPlugin]
 */
@Suppress("USE_DATA_CLASS")
class KubernetesServiceAccountAuthHeaderPluginConfig {
    /**
     * Kubernetes service account token path configuration
     */
    var tokenPath: String = "/var/run/secrets/kubernetes.io/serviceaccount/token"

    /**
     * Token expiration [Duration] configuration
     */
    var expirationTime: Duration = DEFAULT_EXPIRATION_TIME_MINUTES.minutes

    /**
     * Header name
     */
    var headerName: String = SA_HEADER_NAME

    companion object {
        private const val DEFAULT_EXPIRATION_TIME_MINUTES = 5
    }
}

/**
 * @return true if [HttpResponse] is not ok or some failure has happened, false otherwise
 */
fun Result<HttpResponse>.failureOrNotOk() = isFailure || notOk()

/**
 * @return true if [HttpResponse] is not successful, but [Result] is completed, false otherwise
 */
fun Result<HttpResponse>.notOk() = isSuccess && !getOrThrow().status.isSuccess()
