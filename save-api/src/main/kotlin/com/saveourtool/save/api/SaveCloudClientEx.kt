@file:Suppress("TYPE_ALIAS")

package com.saveourtool.save.api

import com.saveourtool.common.agent.TestExecutionExtDto
import com.saveourtool.common.entities.FileDto
import com.saveourtool.common.entities.OrganizationDto
import com.saveourtool.common.entities.ProjectDto
import com.saveourtool.common.entities.ProjectStatus.CREATED
import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.permission.Permission.READ
import com.saveourtool.common.request.CreateExecutionRequest
import com.saveourtool.common.testsuite.TestSuiteVersioned
import com.saveourtool.save.api.errors.SaveCloudError
import com.saveourtool.save.api.impl.DefaultSaveCloudClient

import arrow.core.Either
import io.ktor.client.plugins.auth.Auth
import io.ktor.http.ContentType

import java.net.URL
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

/**
 * _SAVE_ REST API client.
 */
interface SaveCloudClientEx {
    /**
     * Lists the top-level organizations registered in this _SAVE_ instance.
     *
     * @return either the list of organizations, or the error if an error has
     *  occurred.
     */
    suspend fun listOrganizations(): Either<SaveCloudError, List<OrganizationDto>>

    /**
     * Lists the existing (i.e. [non-deleted][CREATED]) projects within the organization.
     *
     * @param organizationName the organization name.
     * @return either the list of projects, or the error if an error has
     *  occurred.
     * @see OrganizationDto.listProjects
     */
    suspend fun listProjects(organizationName: String): Either<SaveCloudError, List<ProjectDto>>

    /**
     * Lists test suites within the organization, [readable][READ] for the current user.
     *
     * @param organizationName the organization name.
     * @return either the list of test suites, or the error if an error has
     *  occurred.
     * @see OrganizationDto.listTestSuites
     */
    suspend fun listTestSuites(organizationName: String): Either<SaveCloudError, List<TestSuiteVersioned>>

    /**
     * Lists uploaded files within the project.
     *
     * @param organizationName the organization name.
     * @param projectName the name of the project.
     * @return either the list of files, or the error if an error has occurred.
     * @see OrganizationDto.listFiles
     */
    suspend fun listFiles(organizationName: String, projectName: String): Either<SaveCloudError, List<FileDto>>

    /**
     * Uploads a local file.
     *
     * @param organizationName the organization name.
     * @param projectName the name of the project.
     * @param file the local file.
     * @param contentType the MIME `Content-Type`, or `null` if unknown.
     * @param stripVersionFromName whether to strip the version number from the
     *   name of the file. For example, `diktat-1.2.3.jar` can be uploaded as
     *   `diktat.jar`.
     * @return the descriptor of the uploaded file.
     * @throws IllegalArgumentException if [file] is not a regular file.
     * @see OrganizationDto.uploadFile
     */
    suspend fun uploadFile(
        organizationName: String,
        projectName: String,
        file: Path,
        contentType: ContentType? = null,
        stripVersionFromName: Boolean = false
    ): Either<SaveCloudError, FileDto>

    /**
     * @param organizationName the organization name.
     * @param projectName the name of the project.
     * @param contestName the optional name of the contest.
     * @return either the list of executions, or the error if an error has
     *  occurred.
     * @see OrganizationDto.listExecutions
     */
    suspend fun listExecutions(
        organizationName: String,
        projectName: String,
        contestName: String? = null
    ): Either<SaveCloudError, List<ExecutionDto>>

    /**
     * @param request the test execution request.
     * @param timeoutValue the timeout value.
     * @param timeoutUnit the timeout unit.
     * @return the test execution descriptor.
     */
    suspend fun submitExecution(
        request: CreateExecutionRequest,
        timeoutValue: Long = 5L,
        timeoutUnit: TimeUnit = MINUTES
    ): Either<SaveCloudError, ExecutionDto>

    /**
     * @param id the identifier which uniquely identifies the execution.
     * @return the execution identified by [id].
     */
    suspend fun getExecutionById(id: Long): Either<SaveCloudError, ExecutionDto>

    /**
     * @param fileId [FileDto.id]
     * @return [Unit], or the error if an error has occurred.
     */
    suspend fun deleteFile(
        fileId: Long,
    ): Either<SaveCloudError, Unit>

    /**
     * Lists the active contests which the project specified by [projectName]
     * has enrolled in.
     *
     * @param organizationName the organization name.
     * @param projectName the name of the project.
     * @param limit the maxim number of contests returned.
     * @return either the list of active contests, or the error if an error has
     *  occurred.
     * @see OrganizationDto.listActiveContests
     */
    suspend fun listActiveContests(
        organizationName: String,
        projectName: String,
        limit: Int = Int.MAX_VALUE
    ): Either<SaveCloudError, List<ContestDto>>

    /**
     * Lists test runs (along with their results) for the batch execution
     * specified by [executionId].
     *
     * @param executionId the identifier which uniquely identifies the batch
     *   execution.
     * @return either the list of test runs, or the error if an error has
     *   occurred.
     * @see ExecutionDto.listTestRuns
     */
    suspend fun listTestRuns(executionId: Long): Either<SaveCloudError, List<TestExecutionExtDto>>

    /**
     * Lists projects within this organization.
     *
     * @return either the list of projects, or the error if an error has
     *  occurred.
     * @see SaveCloudClientEx.listProjects
     */
    suspend fun OrganizationDto.listProjects(): Either<SaveCloudError, List<ProjectDto>> =
            listProjects(organizationName = name)

    /**
     * Lists test suites within this organization.
     *
     * @return either the list of test suites, or the error if an error has
     *  occurred.
     * @see SaveCloudClientEx.listTestSuites
     */
    suspend fun OrganizationDto.listTestSuites(): Either<SaveCloudError, List<TestSuiteVersioned>> =
            listTestSuites(organizationName = name)

    /**
     * Lists uploaded files within the project.
     *
     * @param projectName the name of the project.
     * @return either the list of files, or the error if an error has occurred.
     * @see SaveCloudClientEx.listFiles
     */
    suspend fun OrganizationDto.listFiles(projectName: String): Either<SaveCloudError, List<FileDto>> =
            listFiles(organizationName = name, projectName)

    /**
     * Uploads a local file.
     *
     * @param projectName the name of the project.
     * @param file the local file.
     * @param contentType the MIME `Content-Type`, or `null` if unknown.
     * @param stripVersionFromName whether to strip the version number from the
     *   name of the file. For example, `diktat-1.2.3.jar` can be uploaded as
     *   `diktat.jar`.
     * @return the descriptor of the uploaded file.
     * @throws IllegalArgumentException if [file] is not a regular file.
     * @see SaveCloudClientEx.uploadFile
     */
    suspend fun OrganizationDto.uploadFile(
        projectName: String,
        file: Path,
        contentType: ContentType? = null,
        stripVersionFromName: Boolean = false
    ): Either<SaveCloudError, FileDto> =
            uploadFile(
                organizationName = name,
                projectName,
                file,
                contentType,
                stripVersionFromName
            )

    /**
     * @param projectName the name of the project.
     * @param contestName the optional name of the contest.
     * @return either the list of executions, or the error if an error has
     *  occurred.
     * @see SaveCloudClientEx.listExecutions
     */
    suspend fun OrganizationDto.listExecutions(
        projectName: String,
        contestName: String? = null,
    ): Either<SaveCloudError, List<ExecutionDto>> =
            listExecutions(
                organizationName = name,
                projectName,
                contestName,
            )

    /**
     * Lists the active contests which the project specified by [projectName]
     * has enrolled in.
     *
     * @param projectName the name of the project.
     * @param limit the maxim number of contests returned.
     * @return either the list of active contests, or the error if an error has
     *  occurred.
     * @see SaveCloudClientEx.listActiveContests
     */
    suspend fun OrganizationDto.listActiveContests(
        projectName: String,
        limit: Int = Int.MAX_VALUE
    ): Either<SaveCloudError, List<ContestDto>> =
            listActiveContests(
                organizationName = name,
                projectName,
                limit
            )

    /**
     * Lists test runs (along with their results) for this batch execution.
     *
     * @return either the list of test runs, or the error if an error has
     *   occurred.
     * @see SaveCloudClientEx.listTestRuns
     */
    suspend fun ExecutionDto.listTestRuns(): Either<SaveCloudError, List<TestExecutionExtDto>> =
            listTestRuns(id)

    /**
     * The factory object.
     */
    companion object Factory {
        private const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 100_000L
        private const val DEFAULT_SOCKET_TIMEOUT_MILLIS = 100_000L

        /**
         * Creates a new client instance.
         *
         * @param backendUrl the URL of a _SAVE_ backend (e.g.:
         *   `http://localhost:5800`) or a _SAVE_ gateway (e.g.:
         *   `http://localhost:5300`).
         * @param ioContext the context to be used for I/O, defaults to
         *   [Dispatchers.IO].
         * @param requestTimeoutMillis HTTP request timeout, in milliseconds.
         *   Should be large enough, otherwise the process of uploading large
         *   files may fail.
         * @param socketTimeoutMillis TCP socket timeout, in milliseconds.
         * @param authConfiguration authentication configuration.
         * @return the newly created client instance.
         */
        operator fun invoke(
            backendUrl: URL,
            ioContext: CoroutineContext = Dispatchers.IO,
            requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MILLIS,
            socketTimeoutMillis: Long = DEFAULT_SOCKET_TIMEOUT_MILLIS,
            authConfiguration: Auth.() -> Unit
        ): SaveCloudClientEx =
                DefaultSaveCloudClient(
                    backendUrl,
                    ioContext,
                    requestTimeoutMillis,
                    socketTimeoutMillis,
                    authConfiguration
                )
    }
}
