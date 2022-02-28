/**
 * Kotlin/JS utilities for Fetch API
 */

package org.cqfn.save.frontend.utils

import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project

import org.w3c.fetch.Headers
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

val apiUrl = "${window.location.origin}/api"

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
    error("$status $statusText")
}

/**
 * Read [this] Response body as text and deserialize it using [Json] as type [T]
 *
 * @return response body deserialized as [T]
 */
suspend inline fun <reified T> Response.decodeFromJsonString() = Json.decodeFromString<T>(text().await())

/**
 * Perform GET request.
 *
 * @param url request URL
 * @param headers request headers
 * @return [Response] instance
 */
suspend fun get(url: String, headers: Headers) = request(url, "GET", headers)

/**
 * Perform POST request.
 *
 * @param url request URL
 * @param headers request headers
 * @param body request body
 * @return [Response] instance
 */
suspend fun post(url: String, headers: Headers, body: dynamic) = request(url, "POST", headers, body)

/**
 * Perform an HTTP request using Fetch API. Suspending function that returns a [Response] - a JS promise with result.
 *
 * @param url request URL
 * @param method HTTP request method
 * @param headers HTTP headers
 * @param body request body
 * @param credentials [RequestCredentials] for fetch API
 * @return [Response] instance
 */
suspend fun request(url: String,
                    method: String,
                    headers: Headers,
                    body: dynamic = undefined,
                    credentials: RequestCredentials? = undefined,
): Response = window.fetch(
    input = url,
    RequestInit(
        method = method,
        headers = headers,
        body = body,
        credentials = credentials,
    )
)
    .await().also {
        if (it.status == 401.toShort()) {
            // if 401 - change current URL to the main page (with login screen)
            // note: we may have other uses for 401 in the future
            window.location.href = "${window.location.origin}/#"
        }
    }

/**
 * @param name
 * @param organizationName
 * @return project
 */
suspend fun getProject(name: String, organizationName: String) =
        get("$apiUrl/projects/get/organization-name?name=$name&organizationName=$organizationName", Headers().apply {
            set("Accept", "application/json")
        })
            .decodeFromJsonString<Project>()

/**
 * @param name organization name
 * @return organization
 */
suspend fun getOrganization(name: String) =
        get("$apiUrl/organization/get/organization-name?name=$name", Headers().apply {
            set("Accept", "application/json")
        })
            .decodeFromJsonString<Organization>()
