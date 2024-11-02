@file:Suppress("TYPE_ALIAS")

package com.saveourtool.save.api.impl

import com.saveourtool.common.agent.TestExecutionExtDto
import com.saveourtool.common.entities.*
import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.common.entities.contest.ContestResult
import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.execution.TestingType.CONTEST_MODE
import com.saveourtool.common.filters.ProjectFilter
import com.saveourtool.common.permission.Permission.READ
import com.saveourtool.common.request.CreateExecutionRequest
import com.saveourtool.common.testsuite.TestSuiteVersioned
import com.saveourtool.common.utils.CONTENT_LENGTH_CUSTOM
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.utils.supportJLocalDateTime
import com.saveourtool.common.v1
import com.saveourtool.save.api.SaveCloudClientEx
import com.saveourtool.save.api.errors.SaveCloudError
import com.saveourtool.save.api.errors.TimeoutError
import com.saveourtool.save.api.http.deleteAndCheck
import com.saveourtool.save.api.http.getAndCheck
import com.saveourtool.save.api.http.postAndCheck
import com.saveourtool.save.api.io.readChannel

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Application
import io.ktor.http.ContentType.Text
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders.ContentDisposition
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.contentType
import io.ktor.http.escapeIfNeeded
import io.ktor.serialization.kotlinx.json.json

import java.lang.System.nanoTime
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

import kotlin.coroutines.CoroutineContext
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.system.measureNanoTime
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/**
 * _SAVE_ REST API client, the default implementation.
 *
 * @param backendUrl the URL of a _SAVE_ backend (e.g.:
 *   `http://localhost:5800`) or a _SAVE_ gateway (e.g.:
 *   `http://localhost:5300`).
 * @param ioContext the context to be used for I/O.
 * @param requestTimeoutMillis HTTP request timeout, in milliseconds.
 *   Should be large enough, otherwise the process of uploading large
 *   files may fail.
 * @param socketTimeoutMillis TCP socket timeout, in milliseconds.
 * @param authConfiguration authentication configuration.
 */
internal class DefaultSaveCloudClient(
    private val backendUrl: URL,
    private val ioContext: CoroutineContext,
    requestTimeoutMillis: Long,
    socketTimeoutMillis: Long,
    authConfiguration: Auth.() -> Unit
) : SaveCloudClientEx {
    private val httpClient = httpClient(
        requestTimeoutMillis,
        socketTimeoutMillis,
        authConfiguration
    )

    override suspend fun listOrganizations(): Either<SaveCloudError, List<OrganizationDto>> =
            getAndCheck("/organizations/get/list")

    override suspend fun listProjects(organizationName: String): Either<SaveCloudError, List<ProjectDto>> =
            postAndCheck(
                "/projects/by-filters",
                ProjectFilter(
                    name = "",
                    organizationName = organizationName,
                ),
                Application.Json
            )

    override suspend fun listTestSuites(organizationName: String): Either<SaveCloudError, List<TestSuiteVersioned>> =
            getAndCheck(
                "/test-suites/$organizationName/available",
                requestBody = EmptyContent,
                PERMISSION to READ
            )

    override suspend fun listFiles(
        organizationName: String,
        projectName: String
    ): Either<SaveCloudError, List<FileDto>> =
            getAndCheck("/files/$organizationName/$projectName/list")

    override suspend fun uploadFile(
        organizationName: String,
        projectName: String,
        file: Path,
        contentType: ContentType?,
        stripVersionFromName: Boolean
    ): Either<SaveCloudError, FileDto> {
        val absoluteFile = file.toAbsolutePath().normalize()

        require(absoluteFile.isRegularFile()) {
            when {
                absoluteFile.isDirectory() -> "$absoluteFile is a directory"
                absoluteFile.exists() -> "$absoluteFile is not a regular file"
                else -> "$absoluteFile doesn't exist"
            }
        }

        val fileDto: FileDto
        val nanos = measureNanoTime {
            fileDto = postAndCheck<FileDto>(
                "/files/$organizationName/$projectName/upload",
                MultiPartFormDataContent(formData {
                    val fileName = absoluteFile.fileName.toString().let { fileName ->
                        when {
                            stripVersionFromName -> fileName.stripVersion()
                            else -> fileName
                        }
                    }

                    val headers = Headers.build {
                        this[ContentDisposition] = "filename=${fileName.escapeIfNeeded()}"
                        contentType?.let {
                            this[ContentType] = contentType.toString()
                        }
                    }

                    append(
                        key = "file",
                        value = ChannelProvider {
                            absoluteFile.readChannel(ioContext = ioContext)
                        },
                        headers
                    )
                })
            ) {
                this[CONTENT_LENGTH_CUSTOM] = file.fileSize().toString()
            }.getOrElse { error ->
                return error.left()
            }
        }
        @Suppress("MagicNumber", "FLOAT_IN_ACCURATE_CALCULATIONS")
        logger.debug("Uploaded ${absoluteFile.fileSize()} byte(s) in ${nanos / 1000L / 1e3} ms.")

        return fileDto.right()
    }

    override suspend fun listExecutions(
        organizationName: String,
        projectName: String,
        contestName: String?
    ): Either<SaveCloudError, List<ExecutionDto>> =
            when (contestName) {
                null -> postAndCheck(
                    "/executionDtoList",
                    requestBody = EmptyContent,
                    contentType = Application.Json,
                    ORGANIZATION_NAME to organizationName,
                    PROJECT_NAME to projectName,
                )

                else -> getAndCheck(
                    "/contests/$contestName/executions/$organizationName/$projectName",
                    requestBody = EmptyContent,
                )
            }

    override suspend fun submitExecution(
        request: CreateExecutionRequest,
        timeoutValue: Long,
        timeoutUnit: TimeUnit
    ): Either<SaveCloudError, ExecutionDto> {
        val contestName = request.contestName
        require((request.testingType == CONTEST_MODE) == (contestName != null)) {
            "testingType: ${request.testingType}, contestName: $contestName"
        }

        val (organizationName, projectName) = request.projectCoordinates

        val ignoredExecutionIds = listExecutions(organizationName, projectName, contestName).getOrElse { error ->
            return error.left()
        }
            .asSequence()
            .map(ExecutionDto::id)
            .toSet()

        return postAndCheck<Unit>("/run/trigger", request, Application.Json).flatMap {
            repeatUntilResultAvailable(
                timeoutValue,
                timeoutUnit,
                "while waiting for the submitted execution to appear"
            ) {
                listExecutions(organizationName, projectName, contestName).map { executions ->
                    executions.firstOrNull { (id) ->
                        id !in ignoredExecutionIds
                    }
                }
            }
        }
    }

    override suspend fun getExecutionById(id: Long): Either<SaveCloudError, ExecutionDto> =
            getAndCheck(
                "/executionDto",
                requestBody = EmptyContent,
                EXECUTION_ID to id
            )

    override suspend fun deleteFile(
        fileId: Long,
    ): Either<SaveCloudError, Unit> =
            deleteAndCheck(
                "/files/delete",
                requestBody = EmptyContent,
                accept = Text.Plain,
                FILE_ID to fileId,
            )

    override suspend fun listActiveContests(
        organizationName: String,
        projectName: String,
        limit: Int
    ): Either<SaveCloudError, List<ContestDto>> =
            getAndCheck<List<ContestResult>>(
                "/contests/$organizationName/$projectName/best",
                requestBody = EmptyContent,
                AMOUNT to limit,
            ).map { contestResults ->
                contestResults
                    .asSequence()
                    .map(ContestResult::contestName)
                    .toSet()
            }.flatMap { contestNames ->
                getAndCheck<List<ContestDto>>(
                    "/contests/active",
                ).map { contests ->
                    /*
                     * Leave only those contests which the given project has
                     * enrolled in.
                     */
                    contests
                        .asSequence()
                        .filter { (name) ->
                            name in contestNames
                        }.toList()
                }
            }

    override suspend fun listTestRuns(
        executionId: Long
    ): Either<SaveCloudError, List<TestExecutionExtDto>> =
            postAndCheck(
                "/test-executions",
                requestBody = EmptyContent,
                contentType = Application.Json,
                EXECUTION_ID to executionId,
                CHECK_DEBUG_INFO to true,
                PAGE to 0,
                SIZE to Integer.MAX_VALUE,
            )

    private suspend inline fun <reified T> getAndCheck(
        absolutePath: String,
        requestBody: Any = EmptyContent,
        vararg parameters: Pair<String, Any?>
    ): Either<SaveCloudError, T> =
            httpClient.getAndCheck {
                url(backendUrl.apiUrl(absolutePath))
                parameters.forEach { (key, value) ->
                    parameter(key, value)
                }
                accept(Application.Json)
                setBody(requestBody)
            }

    private suspend inline fun <reified T> postAndCheck(
        absolutePath: String,
        requestBody: Any = EmptyContent,
        contentType: ContentType? = null,
        vararg parameters: Pair<String, Any?>
    ): Either<SaveCloudError, T> =
            httpClient.postAndCheck {
                url(backendUrl.apiUrl(absolutePath))
                parameters.forEach { (key, value) ->
                    parameter(key, value)
                }
                accept(Application.Json)
                contentType?.let {
                    contentType(contentType)
                }
                setBody(requestBody)
            }

    private suspend inline fun <reified T> postAndCheck(
        absolutePath: String,
        requestBody: Any = EmptyContent,
        noinline requestHeaders: HeadersBuilder.() -> Unit = {},
    ): Either<SaveCloudError, T> =
            httpClient.postAndCheck {
                url(backendUrl.apiUrl(absolutePath))
                accept(Application.Json)
                headers(requestHeaders)
                setBody(requestBody)
            }

    private suspend inline fun <reified T> deleteAndCheck(
        absolutePath: String,
        requestBody: Any = EmptyContent,
        accept: ContentType = Application.Json,
        vararg parameters: Pair<String, Any?>
    ): Either<SaveCloudError, T> =
            httpClient.deleteAndCheck {
                url(backendUrl.apiUrl(absolutePath))
                parameters.forEach { (key, value) ->
                    parameter(key, value)
                }
                accept(accept)
                setBody(requestBody)
            }

    private companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<DefaultSaveCloudClient>()
        private const val AMOUNT = "amount"
        private const val CHECK_DEBUG_INFO = "checkDebugInfo"
        private const val DEFAULT_API_VERSION = v1
        private const val EXECUTION_ID = "executionId"
        private const val FILE_ID = "fileId"
        private const val ORGANIZATION_NAME = "organizationName"
        private const val PAGE = "page"
        private const val PERMISSION = "permission"
        private const val POLL_DELAY_MILLIS = 100L
        private const val PROJECT_NAME = "projectName"
        private const val SIZE = "size"
        private val fileWithVersion =
                Regex("""^(?<basename>.+?)-(?<version>\d+(?:\.\d+)*)(?<extension>(?:\.[^.\s-]+)+?)?$""")

        private fun httpClient(
            requestTimeoutMillis: Long,
            socketTimeoutMillis: Long,
            authConfiguration: Auth.() -> Unit
        ): HttpClient =
                HttpClient {
                    install(HttpTimeout) {
                        this.requestTimeoutMillis = requestTimeoutMillis
                        this.socketTimeoutMillis = socketTimeoutMillis
                    }
                    install(Auth, authConfiguration)
                    install(ContentNegotiation) {
                        val json = Json {
                            serializersModule = SerializersModule {
                                supportJLocalDateTime()
                            }
                        }

                        json(json)
                    }
                }

        private fun URL.apiUrl(absolutePath: String, version: String = DEFAULT_API_VERSION): URL {
            require(absolutePath.startsWith('/')) {
                absolutePath
            }

            return "$this/api/$version$absolutePath".let(::URL)
        }

        private fun String.stripVersion(): String {
            val matchResult = fileWithVersion.matchEntire(this) ?: return this

            val basename = matchResult.groups["basename"]?.value ?: return this

            val version = matchResult.groups["version"]?.value
            logger.debug("Stripping version \"$version\" from $this")

            /*
             * The extension with a leading dot, e.g.: `.tar.gz` or `.Z`.
             */
            val extension = matchResult.groups["extension"]?.value.orEmpty()

            return basename + extension
        }

        @Suppress("NESTED_BLOCK")
        private suspend fun <T : Any> repeatUntilResultAvailable(
            timeoutValue: Long,
            timeoutUnit: TimeUnit = MILLISECONDS,
            errorMessageSuffix: String = "",
            block: suspend () -> Either<SaveCloudError, T?>
        ): Either<SaveCloudError, T> {
            val startNanos = nanoTime()

            while (true) {
                val result = block().getOrElse { error ->
                    /*
                     * Return immediately if an error has been encountered.
                     */
                    return error.left()
                }

                return when (result) {
                    null -> {
                        val elapsedNanos = nanoTime() - startNanos

                        when {
                            /*
                             * Time-out.
                             */
                            elapsedNanos > timeoutUnit.toNanos(timeoutValue) -> TimeoutError(
                                timeoutValue,
                                timeoutUnit,
                                errorMessageSuffix
                            ).left()

                            else -> {
                                delay(POLL_DELAY_MILLIS)
                                continue
                            }
                        }
                    }

                    else -> result.right()
                }
            }
        }
    }
}
