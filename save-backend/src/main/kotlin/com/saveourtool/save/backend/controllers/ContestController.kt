package com.saveourtool.save.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.common.entities.*
import com.saveourtool.common.entities.Contest.Companion.toContest
import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.common.entities.contest.ContestStatus
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.request.TestFilesRequest
import com.saveourtool.common.security.OrganizationPermissionEvaluator
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.test.TestFilesContent
import com.saveourtool.common.utils.*
import com.saveourtool.common.v1
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.TestsSourceSnapshotStorage

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.time.LocalDateTime

/**
 * Controller for working with contests.
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "contests"),
)
@RestController
@RequestMapping(path = ["/api/$v1/contests"])
@Suppress("LongParameterList")
internal class ContestController(
    private val contestService: ContestService,
    private val testService: TestService,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
    private val organizationService: OrganizationService,
    private val testSuitesService: TestSuitesService,
    private val testsSourceSnapshotStorage: TestsSourceSnapshotStorage,
    private val lnkContestTestSuiteService: LnkContestTestSuiteService,
) {
    @GetMapping("/{contestName}")
    @Operation(
        method = "GET",
        summary = "Get contest by name.",
        description = "Get contest by name.",
    )
    @Parameters(
        Parameter(name = "contestName", `in` = ParameterIn.PATH, description = "name of a contest", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched contest by it's name.")
    @ApiResponse(responseCode = "404", description = "Contest with such name was not found.")
    fun getContestByName(@PathVariable contestName: String): Mono<ContestDto> = getContestOrNotFound(contestName)
        .map { contest ->
            contest.toDto()
        }

    @GetMapping("/{contestName}/is-featured")
    @Operation(
        method = "GET",
        summary = "Check if contest is featured.",
        description = "Check if a given contest is featured or not.",
    )
    @Parameters(
        Parameter(name = "contestName", `in` = ParameterIn.PATH, description = "name of a contest", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched contest data.")
    @ApiResponse(responseCode = "404", description = "Contest with such name was not found.")
    fun isContestFeatured(
        @PathVariable contestName: String,
    ): Mono<Boolean> = getContestOrNotFound(contestName)
        .map { contest ->
            contestService.isContestFeatured(contest.requiredId())
        }

    @PostMapping("/featured/add-or-delete")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Operation(
        method = "POST",
        summary = "Create or delete featured contest.",
        description = "Mark contest to be featured if it is not marked so yet or unmark otherwise.",
    )
    @Parameters(
        Parameter(name = "contestDto", `in` = ParameterIn.DEFAULT, description = "contest requested for creation", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Contest was successfully created.")
    @ApiResponse(responseCode = "404", description = "Contest with given name is not found.")
    fun addOrDeleteContestToFeatured(
        @RequestParam contestName: String,
    ): Mono<StringResponse> = getContestOrNotFound(contestName)
        .flatMap {
            blockingToMono {
                contestService.addOrDeleteFeaturedContest(it)
            }
        }
        .map {
            ResponseEntity.ok("Contest $contestName was successfully marked to be featured contest.")
        }
        .defaultIfEmpty(
            ResponseEntity.ok("Contest $contestName was successfully unmarked to be featured contest.")
        )

    @GetMapping("/featured/list-active")
    @Operation(
        method = "GET",
        summary = "Get featured contests.",
        description = "Get list of contests marked by admins as featured.",
    )
    @ApiResponse(responseCode = "200", description = "Contests were successfully fetched.")
    fun getFeaturedContests(): Flux<ContestDto> = Flux.fromIterable(contestService.getFeaturedContests())
        .filter {
            LocalDateTime.now() < it.endTime
        }
        .map {
            it.toDto()
        }

    @GetMapping("/active")
    @Operation(
        method = "GET",
        summary = "Get list of contests that are in progress now.",
        description = "Get list of contests that are in progress now.",
    )
    @Parameters(
        Parameter(name = "pageSize", `in` = ParameterIn.QUERY, description = "amount of contests that should be returned, default: 10", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of active contests.")
    fun getContestsInProgress(
        @RequestParam(defaultValue = "10") pageSize: Int,
    ): Flux<ContestDto> = Flux.fromIterable(
        contestService.findContestsInProgress(pageSize)
    ).map {
        it.toDto()
    }

    @GetMapping("/finished")
    @Operation(
        method = "GET",
        summary = "Get list of contests that has already finished.",
        description = "Get list of contests that has already finished.",
    )
    @Parameters(
        Parameter(name = "pageSize", `in` = ParameterIn.QUERY, description = "amount of contests that should be returned, default: 10", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of finished contests.")
    fun getFinishedContests(
        @RequestParam(defaultValue = "10") pageSize: Int,
    ): Flux<ContestDto> = Flux.fromIterable(
        contestService.findFinishedContests(pageSize)
    ).map {
        it.toDto()
    }

    @GetMapping("/{contestName}/public-test")
    @Operation(
        method = "GET",
        summary = "Get public test for contest with given name.",
        description = "Get public test for contest with given name.",
    )
    @Parameters(
        Parameter(name = "contestName", `in` = ParameterIn.PATH, description = "name of a contest", required = true),
        Parameter(name = "testSuiteId", `in` = ParameterIn.QUERY, description = "id of a testSuite", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched public tests.")
    @ApiResponse(responseCode = "404", description = "Either contest with such name was not found or tests are not provided/not attached to given contest.")
    @Suppress("TOO_LONG_FUNCTION")
    fun getPublicTestForContest(
        @PathVariable contestName: String,
        @RequestParam testSuiteId: Long,
    ): Mono<TestFilesContent> = getContestOrNotFound(contestName)
        .map { contest ->
            contest.testSuites().find { it.id == testSuiteId }
                .orNotFound {
                    "No test suite with id $testSuiteId was found in contest $contestName."
                }
        }
        .zipWith(
            Mono.justOrEmpty(testService.findFirstTestByTestSuiteId(testSuiteId))
        )
        .switchIfEmptyToNotFound {
            "No tests were found for test suite with id $testSuiteId."
        }
        .flatMap { (testSuite, test) ->
            testsSourceSnapshotStorage.getTestContent(TestFilesRequest(test.toDto(), testSuite.sourceSnapshot.toDto()))
                .map { testFilesContent ->
                    testFilesContent.copy(
                        language = testSuite.language
                    )
                }
        }

    @GetMapping("/by-organization")
    @Operation(
        method = "GET",
        summary = "Get contests connected with given organization.",
        description = "Get contests connected with given organization.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.QUERY, description = "name of an organization", required = true),
        Parameter(name = "pageSize", `in` = ParameterIn.QUERY, description = "amount of records that will be returned", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched public tests.")
    @ApiResponse(responseCode = "404", description = "Either contest with such name was not found or tests are not provided.")
    fun getOrganizationContests(
        @RequestParam organizationName: String,
        @RequestParam(required = false, defaultValue = "10") pageSize: Int,
    ): Flux<ContestDto> = Flux.fromIterable(
        contestService.findPageOfContestsByOrganizationName(organizationName, Pageable.ofSize(pageSize))
    )
        .map {
            it.toDto()
        }

    @GetMapping("/newest")
    @Operation(
        method = "GET",
        summary = "Get newest contests.",
        description = "Get list of [pageSize] newest contests.",
    )
    @Parameters(
        Parameter(name = "pageSize", `in` = ParameterIn.QUERY, description = "amount of records that will be returned", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched newest contests.")
    fun getSeveralNewestContests(
        @RequestParam(required = false, defaultValue = "5") pageSize: Int,
    ): Flux<ContestDto> = Flux.fromIterable(
        contestService.getNewestContests(pageSize)
    )
        .map {
            it.toDto()
        }

    @PostMapping("/create")
    @com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "POST",
        summary = "Create a new contest.",
        description = "Create a new contest.",
    )
    @Parameters(
        Parameter(name = "contestDto", `in` = ParameterIn.DEFAULT, description = "contest requested for creation", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Contest was successfully created.")
    @ApiResponse(responseCode = "404", description = "Organization with given name was not found.")
    @ApiResponse(responseCode = "403", description = "User cannot create contests with given organization.")
    @ApiResponse(responseCode = "409", description = "Contest with given name is already present.")
    @Suppress("TYPE_ALIAS", "TOO_LONG_FUNCTION")
    fun createContest(
        @RequestBody contestDto: ContestDto,
        authentication: Authentication,
    ): Mono<StringResponse> = Mono.just(
        contestDto.organizationName
    )
        .flatMap {
            organizationService.findByNameAndCreatedStatus(it).toMono()
        }
        .switchIfEmptyToNotFound()
        .filter {
            organizationPermissionEvaluator.canCreateContests(it, authentication)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN)
        .map { organization ->
            contestDto.toContest(organization, emptyList())
        }
        .filter {
            it.validate()
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "Contest data is not valid."
        }
        .filter {
            contestService.createContestIfNotPresent(it)
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "Contest with name [${contestDto.name}] already exists!"
        }
        .zipWith(
            Mono.fromCallable {
                testSuitesService.findTestSuitesByIds(contestDto.testSuites.map { it.id })
            }
        )
        .map { (contest, testSuites) ->
            testSuites.map { testSuite ->
                LnkContestTestSuite(contest, testSuite)
            }
        }
        .map {
            lnkContestTestSuiteService.saveAll(it)
            ResponseEntity.ok("Contest has been successfully created!")
        }

    @PostMapping("/update")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "POST",
        summary = "Update contest.",
        description = "Change existing contest settings.",
    )
    @Parameters(
        Parameter(name = "contestRequest", `in` = ParameterIn.DEFAULT, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched public tests.")
    @ApiResponse(responseCode = "403", description = "Not enough permission to edit current contest.")
    @ApiResponse(responseCode = "404", description = "Either organization or contest with such name was not found.")
    fun updateContest(
        @RequestBody contestRequest: ContestDto,
        authentication: Authentication,
    ): Mono<StringResponse> = Mono.zip(
        organizationService.findByNameAndCreatedStatus(contestRequest.organizationName).toMono(),
        contestService.findByName(contestRequest.name).toMono(),
    )
        .switchIfEmptyToNotFound {
            "Either organization [${contestRequest.organizationName}] or contest [${contestRequest.name}] was not found."
        }
        .filter { (organization, _) ->
            if (contestRequest.status == ContestStatus.DELETED) {
                organizationPermissionEvaluator.hasPermission(authentication, organization, Permission.DELETE)
            } else {
                organizationPermissionEvaluator.hasPermission(authentication, organization, Permission.WRITE)
            }
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "You do not have enough permissions to edit this contest."
        }
        .map { (organization, contest) ->
            contestService.updateContest(
                contestRequest.toContest(organization, contest.testSuiteLinks).apply {
                    id = contest.id
                }
            )
            ResponseEntity.ok("Contest successfully updated")
        }

    @PostMapping("/update-all")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "POST",
        summary = "Update contest.",
        description = "Change existing contest settings.",
    )
    @Parameters(
        Parameter(name = "contestRequest", `in` = ParameterIn.DEFAULT, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched public tests.")
    @ApiResponse(responseCode = "403", description = "Not enough permission to edit current contest.")
    @ApiResponse(responseCode = "404", description = "Either organization or contest with such name was not found.")
    fun updateAllContest(
        @RequestBody contestsRequest: List<ContestDto>,
        authentication: Authentication,
    ): Mono<StringResponse> = Mono.zip(
        organizationService.findByNameAndCreatedStatus(contestsRequest.first().organizationName).toMono(),
        contestsRequest.map { contestRequest -> contestService.findByName(contestRequest.name) }.toMono(),
    )
        .switchIfEmptyToNotFound {
            "Either organization [${contestsRequest.first().organizationName}] or one or more contests in [${contestsRequest.map { it.name }}] was not found."
        }
        .filter { (organization, _) ->
            if (contestsRequest.none { it.status == ContestStatus.DELETED }) {
                organizationPermissionEvaluator.hasPermission(authentication, organization, Permission.WRITE)
            } else {
                organizationPermissionEvaluator.hasPermission(authentication, organization, Permission.DELETE)
            }
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "You do not have enough permissions to edit this contest."
        }
        .map { (organization, contests) ->
            contestsRequest.map { contestRequest ->
                contests.singleOrNull {
                    contestRequest.name == it?.name && contestRequest.organizationName == it.organization.name
                }
                    .orNotFound {
                        "Could not find contest with name ${contestRequest.name} from organization ${contestRequest.organizationName}"
                    }
                    .let { contest ->
                        contestService.updateContest(
                            contestRequest.toContest(organization, contest.testSuiteLinks).apply {
                                id = contest.id
                            }
                        )
                    }
            }
            ResponseEntity.ok("Contest successfully updated")
        }

    private fun getContestOrNotFound(contestName: String): Mono<Contest> = Mono.fromCallable {
        contestService.findByName(contestName).orNotFound {
            "Could not find contest with name $contestName."
        }
    }
}
