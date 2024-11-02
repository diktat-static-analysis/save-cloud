/**
 * Utilities, which extends http client functionality and provide api for execution submission process
 */

package com.saveourtool.save.api.utils

import com.saveourtool.common.entities.FileDto
import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.request.CreateExecutionRequest
import com.saveourtool.common.utils.supportJLocalDateTime
import com.saveourtool.common.v1
import com.saveourtool.save.api.authorization.Authorization
import com.saveourtool.save.api.config.WebClientProperties

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.InternalAPI
import okio.Path.Companion.toPath

import java.io.File

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

private val json = Json {
    serializersModule = SerializersModule {
        supportJLocalDateTime()
    }
}

private object Backend {
    lateinit var url: String
}

/**
 * @property username
 */
private object UserInformation {
    lateinit var username: String
}

/**
 * @return list of available files from storage
 */
suspend fun HttpClient.getAvailableFilesList(
): List<FileDto> = getRequestWithAuthAndJsonContentType(
    "${Backend.url}/api/$v1/files/list"
).body()

/**
 * @param file
 * @return [FileDto] of uploaded file
 */
@OptIn(InternalAPI::class)
suspend fun HttpClient.uploadAdditionalFile(
    file: String,
): FileDto = this.post {
    url("${Backend.url}/api/$v1/files/upload")
    body = MultiPartFormDataContent(formData {
        append(
            key = "file",
            value = File(file).readBytes(),
            headers = Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=${file.toPath().name}")
            }
        )
    })
}.body()

/**
 * Submit execution
 *
 * @param createExecutionRequest execution request
 * @return HttpResponse
 */
@Suppress("TOO_LONG_FUNCTION")
suspend fun HttpClient.submitExecution(createExecutionRequest: CreateExecutionRequest): HttpResponse = this.post {
    url("${Backend.url}/api/$v1/run/trigger")
    header(HttpHeaders.ContentType, ContentType.Application.Json)
    setBody(createExecutionRequest)
}

/**
 * @param projectName
 * @param organizationName
 * @return ExecutionDto
 */
suspend fun HttpClient.getLatestExecution(
    projectName: String,
    organizationName: String
): ExecutionDto = getRequestWithAuthAndJsonContentType(
    "${Backend.url}/api/$v1/latestExecution?name=$projectName&organizationName=$organizationName"
).body()

/**
 * @param executionId
 * @return ExecutionDto
 */
suspend fun HttpClient.getExecutionById(
    executionId: Long
): ExecutionDto = getRequestWithAuthAndJsonContentType(
    "${Backend.url}/api/$v1/executionDto?executionId=$executionId"
).body()

private suspend fun HttpClient.getRequestWithAuthAndJsonContentType(url: String): HttpResponse = this.get {
    url(url)
    contentType(ContentType.Application.Json)
}

/**
 * @param authorization authorization settings
 * @param webClientProperties http client configuration
 * @return HttpClient instance
 */
fun initializeHttpClient(
    authorization: Authorization,
    webClientProperties: WebClientProperties,
): HttpClient {
    Backend.url = webClientProperties.backendUrl
    UserInformation.username = authorization.userInformation

    return HttpClient {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.NONE
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Auth) {
            basic {
                // by default, ktor will wait for the server to respond with 401,
                // and only then send the authentication header
                // therefore, adding sendWithoutRequest is required
                sendWithoutRequest { true }
                credentials {
                    BasicAuthCredentials(username = authorization.userInformation, password = authorization.token)
                }
            }
        }
    }
}
