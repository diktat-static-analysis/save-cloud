package com.saveourtool.save.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.common.domain.EntitySaveStatus
import com.saveourtool.common.entities.*
import com.saveourtool.common.entities.TestSuitesSource.Companion.toTestSuiteSource
import com.saveourtool.common.service.GitService
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.test.TestsSourceVersionInfoList
import com.saveourtool.common.testsuite.*
import com.saveourtool.common.utils.*
import com.saveourtool.common.v1
import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.utils.toResponseEntity

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

typealias EntitySaveStatusResponse = ResponseEntity<EntitySaveStatus>

/**
 * Controller for [TestSuitesSource]
 */
@ApiSwaggerSupport
@RestController
@Tags(
    Tag(name = "test-suites-source"),
)
@RequestMapping("/api/$v1/test-suites-sources")
class TestSuitesSourceController(
    private val testSuitesSourceService: TestSuitesSourceService,
    private val testsSourceVersionService: TestsSourceVersionService,
    private val organizationService: OrganizationService,
    private val gitService: GitService,
) {
    @GetMapping("/{organizationName}/list")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "List test suites source by organization name.",
        description = "List test suites source by organization name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true)
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of test suites sources by organization name.")
    @ApiResponse(responseCode = "404", description = "Organization was not found by provided name.")
    fun list(
        @PathVariable organizationName: String,
    ): Mono<TestSuitesSourceDtoList> = getOrganization(organizationName)
        .map { organization ->
            testSuitesSourceService.getAllByOrganization(organization)
                .map { it.toDto() }
        }

    @GetMapping("/{organizationName}/{sourceName}")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get test suites source by organization name and source name.",
        description = "Get test suites source by organization name and test suites source name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of test suites sources by organization name.")
    @ApiResponse(
        responseCode = "404",
        description = "Either organization was not found by provided name or test suites source with such name in organization name was not found.",
    )
    fun findAsDtoByName(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String
    ): Mono<TestSuitesSourceDto> = getTestSuitesSource(organizationName, sourceName)
        .map { it.toDto() }

    @GetMapping("/{organizationName}/{sourceName}/list-version")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "List of snapshot for test suites source.",
        description = "List of snapshot for test suites source.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully listed snapshots for requested test suites source.")
    @ApiResponse(
        responseCode = "404",
        description = "Either organization was not found by provided name or test suites source with such name in organization name was not found.",
    )
    @ApiResponse(responseCode = "404", description = ".")
    fun listVersions(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
    ): Mono<TestsSourceVersionInfoList> = findAsDtoByName(organizationName, sourceName)
        .flatMap {
            blockingToMono { testsSourceVersionService.getAllAsInfo(it.organizationName, it.name) }
        }

    @GetMapping("/{organizationName}/list-version")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "List of snapshot for all test suites sources in requested organization.",
        description = "List of snapshot for all test suites sources in requested organization.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully listed snapshots for all test suites sources in requested organization.")
    @ApiResponse(responseCode = "404", description = "Organization was not found by provided name.")
    fun listVersions(
        @PathVariable organizationName: String,
    ): Mono<TestsSourceVersionInfoList> = getOrganization(organizationName)
        .flatMap { organization ->
            blockingToMono { testsSourceVersionService.getAllAsInfo(organization.name) }
        }

    @PostMapping("/create")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get or create a new test suite source by provided values.",
        description = "Get or create a new test suite source by provided values.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully get or create test suites source with requested values.")
    @ApiResponse(responseCode = "404", description = "Either git credentials were not found by provided url or organization was not found by provided name.")
    @ApiResponse(responseCode = "409", description = "Test suite name is already taken.")
    fun createTestSuitesSource(
        @RequestBody testSuiteRequest: TestSuitesSourceDto,
    ): Mono<EntitySaveStatusResponse> = getOrganization(testSuiteRequest.organizationName)
        .zipWhen { getGit(it, testSuiteRequest.gitDto.url) }
        .map { (organization, git) ->
            testSuiteRequest.toTestSuiteSource(organization, git)
        }
        .flatMap { testSuitesSource ->
            when (val saveStatus = testSuitesSourceService.createSourceIfNotPresent(testSuitesSource)) {
                EntitySaveStatus.EXIST, EntitySaveStatus.CONFLICT, EntitySaveStatus.NEW -> Mono.fromCallable { saveStatus.toResponseEntity() }
                else -> Mono.error(IllegalStateException("Not expected status for creating a new entity"))
            }
        }

    @PostMapping("/update")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get or create a new test suite source by provided values.",
        description = "Get or create a new test suite source by provided values.",
    )
    @Parameters(
        Parameter(name = "id", `in` = ParameterIn.QUERY, description = "ID of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully get or create test suites source with requested values.")
    @ApiResponse(responseCode = "400", description = "Try to change organization or git by this request.")
    @ApiResponse(responseCode = "404", description = "Test suites source was not found by provided ID.")
    @ApiResponse(responseCode = "409", description = "Test suite name is already taken.")
    fun update(
        @RequestParam("id") id: Long,
        @RequestBody dtoToUpdate: TestSuitesSourceDto
    ): Mono<EntitySaveStatusResponse> = getTestSuitesSource(id)
        .requireOrSwitchToResponseException({ organization.name == dtoToUpdate.organizationName }, HttpStatus.BAD_REQUEST) {
            "Organization cannot be changed in TestSuitesSource"
        }
        .requireOrSwitchToResponseException({ git.url == dtoToUpdate.gitDto.url }, HttpStatus.BAD_REQUEST) {
            "Git cannot be changed in TestSuitesSource"
        }
        .map { originalEntity ->
            originalEntity.apply {
                name = dtoToUpdate.name
                description = dtoToUpdate.description
                testRootPath = dtoToUpdate.testRootPath
                latestFetchedVersion = dtoToUpdate.latestFetchedVersion
            }
        }
        .flatMap { updatedEntity ->
            when (val saveStatus = testSuitesSourceService.update(updatedEntity)) {
                EntitySaveStatus.EXIST, EntitySaveStatus.CONFLICT, EntitySaveStatus.UPDATED -> Mono.just(saveStatus.toResponseEntity())
                else -> Mono.error(IllegalStateException("Not expected status for creating a new entity"))
            }
        }

    @DeleteMapping("/{organizationName}/{sourceName}/delete-version")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "DELETE",
        summary = "Delete test suites and snapshot for requested version from provided test suites source.",
        description = "Delete test suites and snapshot for requested version from provided test suites source.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully deleted test suites and snapshot for requested version from provided source.")
    @ApiResponse(
        responseCode = "404",
        description = "Either organization was not found by provided name or test suites source with such name in organization name was not found.",
    )
    fun deleteVersion(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam version: String,
    ): Mono<Unit> = blockingToMono {
        testsSourceVersionService.delete(organizationName, sourceName, version)
    }

    private fun getOrganization(organizationName: String): Mono<Organization> = blockingToMono {
        organizationService.findByNameAndCreatedStatus(organizationName)
    }.switchIfEmptyToNotFound {
        "Organization not found by name $organizationName"
    }

    private fun getGit(organization: Organization, gitUrl: String): Mono<Git> = blockingToMono {
        gitService.findByOrganizationAndUrl(organization, gitUrl)
    }.switchIfEmptyToNotFound {
        "There is no git credential with url $gitUrl in ${organization.name}"
    }

    private fun getTestSuitesSource(organizationName: String, name: String): Mono<TestSuitesSource> =
            getOrganization(organizationName)
                .flatMap { organization ->
                    testSuitesSourceService.findByName(organization, name).toMono()
                }
                .switchIfEmptyToNotFound {
                    "TestSuitesSource not found by name $name for organization $organizationName"
                }

    private fun getTestSuitesSource(id: Long): Mono<TestSuitesSource> =
            blockingToMono {
                testSuitesSourceService.findById(id)
            }
                .switchIfEmptyToNotFound {
                    "TestSuiteSource not found by ID $id"
                }

    @PostMapping("/{organizationName}/{sourceName}/fetch")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Post fetching of new tests from test suites source.",
        description = "Post fetching of new tests from test suites source.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
        Parameter(name = "mode", `in` = ParameterIn.QUERY, description = "fetch mode", required = true),
        Parameter(name = "version", `in` = ParameterIn.QUERY, description = "version to be fetched: tag, branch or commit id", required = true),
    )
    @ApiResponse(responseCode = "202", description = "Successfully trigger fetching new tests from requested test suites source.")
    fun triggerFetch(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam mode: TestSuitesSourceFetchMode,
        @RequestParam version: String,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono { testSuitesSourceService.findByName(organizationName, sourceName) }
        .flatMap { testSuitesSource ->
            Mono.just(
                ResponseEntity.accepted()
                    .body("Trigger fetching new tests from $sourceName in $organizationName")
            ).doOnSuccess {
                testSuitesSourceService.fetch(testSuitesSource.toDto(), mode, version, authentication.userId())
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe()
            }
        }

    @GetMapping("/{organizationName}/{sourceName}/tag-list-to-fetch")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get list of tags which can be fetched from test suites source.",
        description = "Get list of tags which can be fetched from test suites source.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully listed tags which can be fetched from requested test suites source.")
    fun tagListToFetch(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
    ): Mono<StringListResponse> = blockingToMono { testSuitesSourceService.findByName(organizationName, sourceName) }
        .flatMap { testSuitesSourceService.tagList(it.toDto()) }
        .map { tags ->
            val versions = testsSourceVersionService.getAllVersions(organizationName, sourceName)
            ResponseEntity.ok()
                .body(tags.filterNot { it in versions })
        }

    @GetMapping("/{organizationName}/{sourceName}/branch-list-to-fetch")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get list of branches which can be fetched from test suites source.",
        description = "Get list of branches which can be fetched from test suites source.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully listed branches which can be fetched from requested test suites source.")
    fun branchListToFetch(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
    ): Mono<StringListResponse> = blockingToMono { testSuitesSourceService.findByName(organizationName, sourceName) }
        .flatMap { testSuitesSourceService.branchList(it.toDto()) }
        .map { ResponseEntity.ok().body(it) }

    @GetMapping("/available")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get organizations with public test suite sources.",
        description = "Get list of organizations with public test suite sources",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched organizations with public test suite sources.")
    fun getOrganizationNamesWithPublicTestSuiteSources(
    ): Mono<TestSuitesSourceDtoList> = testSuitesSourceService.getAvailableTestSuiteSources().toMono()
        .map { testSuitesSourceList ->
            testSuitesSourceList.map { it.toDto() }
        }
}
