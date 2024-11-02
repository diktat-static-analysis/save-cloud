/**
 * Methods to make specific requests to backend
 */

package com.saveourtool.frontend.common.http

import com.saveourtool.common.agent.TestExecutionDto
import com.saveourtool.common.entities.*
import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.utils.AvatarType
import com.saveourtool.common.utils.CONTENT_LENGTH_CUSTOM
import com.saveourtool.common.utils.FILE_PART_NAME
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import web.file.File
import web.http.FormData

import kotlinx.browser.window

/**
 * @param name
 * @param organizationName
 * @return project
 */
suspend fun ComponentWithScope<*, *>.getProject(name: String, organizationName: String) = get(
    "$apiUrl/projects/get/organization-name?name=$name&organizationName=$organizationName",
    jsonHeaders,
    loadingHandler = ::classLoadingHandler,
    responseHandler = ::classComponentRedirectOnFallbackResponseHandler,
)
    .runCatching {
        decodeFromJsonString<ProjectDto>()
    }

/**
 * @param name organization name
 * @return organization
 */
suspend fun ComponentWithScope<*, *>.getOrganization(name: String) = get(
    "$apiUrl/organizations/$name",
    jsonHeaders,
    loadingHandler = ::classLoadingHandler,
    responseHandler = ::classComponentRedirectOnFallbackResponseHandler,
)
    .decodeFromJsonString<OrganizationDto>()

/**
 * @param name contest name
 * @return contestDTO
 */
suspend fun ComponentWithScope<*, *>.getContest(name: String) = get(
    "$apiUrl/contests/$name",
    jsonHeaders,
    loadingHandler = ::classLoadingHandler,
    responseHandler = ::classComponentRedirectOnFallbackResponseHandler,
)
    .decodeFromJsonString<ContestDto>()

/**
 * @param name username
 * @return info about user
 */
suspend fun ComponentWithScope<*, *>.getUser(name: String) = get(
    "$apiUrl/users/$name",
    jsonHeaders,
    loadingHandler = ::classLoadingHandler,
)
    .decodeFromJsonString<UserInfo>()

/**
 * @param file image file
 * @param name avatar owner name
 * @param type avatar type
 * @param loadingHandler
 */
suspend fun ComponentWithScope<*, *>.postImageUpload(
    file: File,
    name: String,
    type: AvatarType,
    loadingHandler: suspend (suspend () -> Response) -> Response,
) {
    val response = post(
        url = "$apiUrl/avatar/upload",
        params = jso<dynamic> {
            owner = name
            this.type = type
        },
        Headers().apply { append(CONTENT_LENGTH_CUSTOM, file.size.toString()) },
        FormData().apply { set(FILE_PART_NAME, file) },
        loadingHandler,
    )
    if (response.ok) {
        window.location.reload()
    }
}

/**
 * @param file image file
 * @param name avatar owner name
 * @param type avatar type
 * @param loadingHandler
 */
suspend fun WithRequestStatusContext.postImageUpload(
    file: File,
    name: String?,
    type: AvatarType,
    loadingHandler: suspend (suspend () -> Response) -> Response,
) {
    val response = post(
        url = "$apiUrl/avatar/upload",
        params = jso<dynamic> {
            owner = name
            this.type = type
        },
        Headers().apply { append(CONTENT_LENGTH_CUSTOM, file.size.toString()) },
        FormData().apply { set(FILE_PART_NAME, file) },
        loadingHandler,
    )
    if (response.ok) {
        window.location.reload()
    }
}

/**
 * @param url url to upload a file
 * @param file a file which needs to be uploaded
 * @param loadingHandler
 * @return response of operation
 */
suspend fun WithRequestStatusContext.postUploadFile(
    url: String,
    file: File,
    loadingHandler: suspend (suspend () -> Response) -> Response,
): Response = post(
    url,
    Headers().apply { append(CONTENT_LENGTH_CUSTOM, file.size.toString()) },
    FormData().apply { set(FILE_PART_NAME, file) },
    loadingHandler = loadingHandler,
)

/**
 * Fetch debug info for test execution
 *
 * @param testExecutionId id of a particular test execution
 * @return Response
 */
@Suppress("TYPE_ALIAS")
suspend fun ComponentWithScope<*, *>.getDebugInfoFor(
    testExecutionId: Long,
) = getDebugInfoFor(testExecutionId, this::get)

/**
 * Fetch debug info for test execution
 *
 * @param testExecutionId id of a particular test execution
 * @return Response
 */
suspend fun WithRequestStatusContext.getDebugInfoFor(
    testExecutionId: Long,
) = getDebugInfoFor(testExecutionId, this::get)

/**
 * Fetch execution info for test execution
 *
 * @param testExecutionDto
 * @return Response
 */
@Suppress("TYPE_ALIAS")
suspend fun ComponentWithScope<*, *>.getExecutionInfoFor(
    testExecutionDto: TestExecutionDto,
) = get(
    "$apiUrl/files/get-execution-info",
    params = jso<dynamic> { executionId = testExecutionDto.executionId },
    jsonHeaders,
    ::noopLoadingHandler,
    ::noopResponseHandler
)

/**
 * Makes a call to change project status
 *
 * @param organizationName name of the organization whose status will be changed
 * @param status is new status
 * @return lazy response
 */
fun responseChangeOrganizationStatus(organizationName: String, status: OrganizationStatus): suspend WithRequestStatusContext.() -> Response = {
    post(
        url = "$apiUrl/organizations/$organizationName/change-status?status=$status",
        headers = jsonHeaders,
        body = undefined,
        loadingHandler = ::noopLoadingHandler,
        responseHandler = ::noopResponseHandler,
    )
}

@Suppress("TYPE_ALIAS")
private suspend fun getDebugInfoFor(
    testExecutionId: Long,
    get: suspend (String, dynamic, Headers, suspend (suspend () -> Response) -> Response, (Response) -> Unit) -> Response,
) = get(
    "$apiUrl/files/get-debug-info",
    jso { this.testExecutionId = testExecutionId },
    jsonHeaders,
    ::noopLoadingHandler,
    ::noopResponseHandler,
)
