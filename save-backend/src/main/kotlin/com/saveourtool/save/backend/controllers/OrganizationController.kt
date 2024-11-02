package com.saveourtool.save.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.common.domain.OrganizationSaveStatus.*
import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.*
import com.saveourtool.common.filters.OrganizationFilter
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.security.OrganizationPermissionEvaluator
import com.saveourtool.common.service.GitService
import com.saveourtool.common.service.LnkUserOrganizationService
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.utils.*
import com.saveourtool.common.v1
import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.TestsSourceSnapshotStorage

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.util.EnumSet

typealias OrganizationDtoList = List<OrganizationDto>

/**
 * Controller for working with organizations.
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "organizations"),
)
@RestController
@RequestMapping(path = ["/api/$v1/organizations"])
@Suppress("LongParameterList")
internal class OrganizationController(
    private val organizationService: OrganizationService,
    private val lnkUserOrganizationService: LnkUserOrganizationService,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
    private val gitService: GitService,
    private val testSuitesSourceService: TestSuitesSourceService,
    private val testsSourceSnapshotStorage: TestsSourceSnapshotStorage,
    config: ConfigProperties,
) {
    private val webClientToPreprocessor = WebClient.create(config.preprocessorUrl)

    @PostMapping("/all-by-filters")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get all organizations",
        description = "Get organizations",
    )
    @Parameters(
        Parameter(name = "organizationFilter", `in` = ParameterIn.DEFAULT, description = "organization filters", required = true),
        Parameter(name = "pageSize", `in` = ParameterIn.QUERY, description = "amount of organizations that should be returned, default: 5", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched all registered organizations")
    fun getAllOrganizationsByFilters(
        @RequestBody organizationFilter: OrganizationFilter,
        @RequestParam(required = false, defaultValue = "5") pageSize: Int,
    ): Mono<OrganizationDtoList> = blockingToMono {
        organizationService.getFiltered(organizationFilter, Pageable.ofSize(pageSize)).map(Organization::toDto)
    }

    @PostMapping("/by-filters-with-rating")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get organizations with rating matching filters.",
        description = "Get filtered organizations with rating available for the current user.",
    )
    @Parameters(
        Parameter(name = "organizationFilter", `in` = ParameterIn.DEFAULT, description = "organization filters", required = true),
        Parameter(name = "pageSize", `in` = ParameterIn.QUERY, description = "amount of organizations that should be returned, default: 5", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched non-deleted organizations.")
    fun getFilteredOrganizationsWithRating(
        @RequestBody organizationFilter: OrganizationFilter,
        @RequestParam(required = false, defaultValue = "5") pageSize: Int,
        authentication: Authentication?,
    ): Flux<OrganizationWithRating> = getFilteredOrganizationDtoList(organizationFilter, pageSize)
        .flatMap { organizationDto ->
            organizationService.getGlobalRating(organizationDto.name, authentication)
                .map { rating ->
                    OrganizationWithRating(
                        organization = organizationDto,
                        globalRating = rating,
                    )
                }
        }

    @GetMapping("/{organizationName}")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get organization by name.",
        description = "Get an organization by its name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched organization by it's name.")
    @ApiResponse(responseCode = "404", description = "Organization with such name was not found.")
    fun getOrganizationByName(
        @PathVariable organizationName: String,
    ) = blockingToMono {
        organizationService.findByNameAndCreatedStatus(organizationName)
    }
        .map { it.toDto() }
        .switchIfEmptyToNotFound {
            "Organization not found by name $organizationName"
        }

    @GetMapping("/get/list")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get your organizations.",
        description = "Get list of all organizations where current user is a participant.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of organizations.")
    fun getOrganizationsByUser(
        authentication: Authentication?,
    ): Flux<OrganizationDto> = authentication.toMono()
        .map { auth ->
            auth.userId()
        }
        .flatMapMany {
            lnkUserOrganizationService.findAllByAuthenticationAndStatuses(it)
        }
        .map {
            it.organization.toDto()
        }

    @GetMapping("/with-allow-bulk-upload")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get your organizations.",
        description = "Get list of all organizations where current user is a participant.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of organizations.")
    fun getOrganizationsByUserAndBulkUpload(
        authentication: Authentication?,
    ): Flux<OrganizationDto> = authentication.toMono()
        .map { auth ->
            auth.userId()
        }
        .flatMapMany {
            lnkUserOrganizationService.findAllByAuthenticationAndStatusesAndBulkUpload(it)
        }
        .map {
            it.organization.toDto()
        }

    @GetMapping("/get/list-by-user-name")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get your organizations.",
        description = "Get list of all organizations where current user is a participant.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of organizations.")
    fun getOrganizationsByUserNameAndCreatedStatus(
        @RequestParam userName: String,
    ): Flux<OrganizationDto> = blockingToFlux {
        lnkUserOrganizationService.getOrganizationsByUserNameAndCreatedStatus(userName).map { it.organization.toDto() }
    }

    @GetMapping("/get/by-prefix")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get organization by prefix.",
        description = "Get list of organizations matching prefix.",
    )
    @Parameters(
        Parameter(name = "prefix", `in` = ParameterIn.QUERY, description = "organization name prefix", required = false),
        Parameter(name = "pageSize", `in` = ParameterIn.QUERY, description = "amount of organizations that should be returned, default: 5", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of organizations.")
    fun getOrganizationNamesByPrefix(
        @RequestParam prefix: String,
        @RequestParam(required = false, defaultValue = "5") pageSize: Int,
    ): Mono<OrganizationDtoList> = getFilteredOrganizationDtoList(OrganizationFilter(prefix), pageSize)
        .collectList()

    @PostMapping("/{organizationName}/manage-contest-permission")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Operation(
        method = "POST",
        summary = "Make an organization to be able to create contests.",
        description = "Make an organization to be able to create contests.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "isAbleToCreateContests", `in` = ParameterIn.QUERY, description = "new flag for contest creation ability", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully changed ability to create contests.")
    @ApiResponse(responseCode = "403", description = "Could not change ability to create contests due to lack of permission.")
    @ApiResponse(responseCode = "404", description = "Organization with such name was not found.")
    @Suppress("UnsafeCallOnNullableType")
    fun setAbilityToCreateContest(
        @PathVariable organizationName: String,
        @RequestParam isAbleToCreateContests: Boolean,
        authentication: Authentication,
    ): Mono<StringResponse> = Mono.just(
        organizationName
    )
        .flatMap {
            organizationService.findByNameAndCreatedStatus(organizationName).toMono()
        }
        .switchIfEmptyToNotFound {
            "No organization with name $organizationName was found."
        }
        .filter {
            organizationPermissionEvaluator.hasGlobalRoleOrOrganizationRole(authentication, it.name, Role.SUPER_ADMIN)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "Not enough permission for managing canCreateContests flag."
        }
        .map { organization ->
            organizationService.updateOrganization(
                organization.copy(canCreateContests = isAbleToCreateContests).apply { id = organization.id }
            )
            ResponseEntity.ok("Organization updated")
        }

    @PostMapping("/{organizationName}/manage-bulk-upload-permission")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Operation(
        method = "POST",
        summary = "Make an organization to be able to bulk upload files.",
        description = "Make an organization to be able to bulk upload files.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "isAbleToToBulkUpload", `in` = ParameterIn.QUERY, description = "new flag for bulk upload files ability", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully changed ability to bulk upload files.")
    @ApiResponse(responseCode = "403", description = "Could not change ability to bulk upload files due to lack of permission.")
    @ApiResponse(responseCode = "404", description = "Organization with such name was not found.")
    @Suppress("UnsafeCallOnNullableType")
    fun setAbilityToBulkUpload(
        @PathVariable organizationName: String,
        @RequestParam isAbleToToBulkUpload: Boolean,
    ): Mono<StringResponse> = Mono.just(
        organizationName
    )
        .flatMap {
            organizationService.findByNameAndCreatedStatus(organizationName).toMono()
        }
        .switchIfEmptyToNotFound {
            "No organization with name $organizationName was found."
        }
        .map { organization ->
            organizationService.updateOrganization(
                organization.copy(canBulkUpload = isAbleToToBulkUpload).apply { id = organization.id }
            )
            ResponseEntity.ok("Organization updated")
        }

    @PostMapping("/save")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "POST",
        summary = "Create a new organization.",
        description = "Create a new organization.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved a new organization.")
    @ApiResponse(responseCode = "409", description = "Requested name is not available.")
    fun saveOrganization(
        @RequestBody newOrganization: OrganizationDto,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono {
        organizationService.saveOrganization(newOrganization.toOrganization())
    }.map {
        when (it.second) {
            CONFLICT -> throw ResponseStatusException(HttpStatus.CONFLICT, CONFLICT.message)
            // Small hack here just not to affect frontend rendering: we return CONFLICT status
            INVALID_NAME -> throw ResponseStatusException(HttpStatus.CONFLICT, INVALID_NAME.message)
            else -> it
        }
    }
        .map { (organizationId, organizationStatus) ->
            lnkUserOrganizationService.setRoleByIds(
                authentication.userId(),
                organizationId,
                Role.OWNER,
            )
            logger.info("Save new organization id = $organizationId")
            ResponseEntity.ok(organizationStatus.message)
        }

    @PostMapping("/{organizationName}/update")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "POST",
        summary = "Update existing organization.",
        description = "Change settings of an existing organization by it's name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully updated an organization.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for managing this organization.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name.")
    @ApiResponse(responseCode = "409", description = "Organization with such name already exists.")
    @Suppress("UnsafeCallOnNullableType")
    fun updateOrganization(
        @PathVariable organizationName: String,
        @RequestBody organization: OrganizationDto,
        authentication: Authentication,
    ): Mono<StringResponse> = Mono.just(
        organizationName
    )
        .flatMap {
            organizationService.findByNameAndCreatedStatus(it).toMono()
        }
        .switchIfEmptyToNotFound {
            "Could not find an organization with name $organizationName."
        }
        .filter {
            organizationPermissionEvaluator.hasGlobalRoleOrOrganizationRole(authentication, it.name, Role.OWNER)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "Not enough permission for managing organization $organizationName."
        }
        .filter {
            organizationService.findByNameAndCreatedStatus(organization.name) != null
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "There already is an organization with name ${organization.name}"
        }
        .map { organizationFromDb ->
            organizationService.updateOrganization(
                organizationFromDb.apply {
                    description = organization.description
                    rating = organization.rating
                }
            )
            ResponseEntity.ok("Organization updated")
        }

    @PostMapping("/{organizationName}/change-status")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "POST",
        summary = "Change status existing organization.",
        description = "Change status existing organization by its name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "status", `in` = ParameterIn.QUERY, description = "type of status being set", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully change status an organization.")
    @ApiResponse(responseCode = "400", description = "Invalid new status of the organization.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for this action on organization.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name.")
    @ApiResponse(responseCode = "409", description = "There are projects connected to organization. Please delete all of them and try again.")
    fun changeOrganizationStatus(
        @PathVariable organizationName: String,
        @RequestParam status: OrganizationStatus,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono {
        organizationService.findByNameAndStatuses(organizationName, EnumSet.allOf(OrganizationStatus::class.java))
    }
        .switchIfEmptyToNotFound {
            "Could not find an organization with name $organizationName."
        }
        .filter {
            it.status != status
        }
        .switchIfEmptyToResponseException(HttpStatus.BAD_REQUEST) {
            "Invalid new status of the organization $organizationName"
        }
        .filter {
            organizationPermissionEvaluator.hasPermissionToChangeStatus(authentication, it, status)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "Not enough permission for this action with organization $organizationName."
        }
        .filter {
            status != OrganizationStatus.DELETED || !organizationService.hasProjects(organizationName)
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "There are projects connected to $organizationName. Please delete all of them and try again."
        }
        .publishOn(Schedulers.boundedElastic())
        .map { organization ->
            when (status) {
                OrganizationStatus.BANNED -> {
                    organizationService.banOrganization(organization)
                    ResponseEntity.ok("Successfully banned the organization")
                }
                OrganizationStatus.DELETED -> {
                    organizationService.deleteOrganization(organization)
                    ResponseEntity.ok("Successfully deleted the organization")
                }
                OrganizationStatus.CREATED -> {
                    organizationService.recoverOrganization(organization)
                    ResponseEntity.ok("Successfully recovered the organization")
                }
            }
        }

    @GetMapping("/{organizationName}/list-git")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "GET",
        summary = "Get organization Gits.",
        description = "Get a list of organization's Gits.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched a list of GitDtos.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name.")
    fun listGit(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Flux<GitDto> = Mono.just(organizationName)
        .flatMap {
            organizationService.findByNameAndCreatedStatus(it).toMono()
        }
        .switchIfEmptyToNotFound {
            "Could not find an organization with name $organizationName."
        }
        .filter {
            organizationPermissionEvaluator.hasPermission(authentication, it, Permission.WRITE)
        }
        .flatMapIterable {
            gitService.getAllByOrganization(it)
        }
        .map { it.toDto() }

    @PostMapping("/{organizationName}/create-git")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "POST",
        summary = "Create git in organization.",
        description = "Create git in organization.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved an organization git.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for saving organization git.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name.")
    @ApiResponse(responseCode = "409", description = "Provided invalid git credential.")
    fun createGit(
        @PathVariable organizationName: String,
        @RequestBody gitDto: GitDto,
        authentication: Authentication,
    ): Mono<StringResponse> = upsertGitCredential(organizationName, gitDto, authentication, isUpdate = false)

    @PostMapping("/{organizationName}/update-git")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "POST",
        summary = "Update existed git in organization.",
        description = "Update existed git in organization.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved an organization git.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for saving organization git.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name or git credential with provided url.")
    @ApiResponse(responseCode = "409", description = "Provided invalid git credential.")
    fun updateGit(
        @PathVariable organizationName: String,
        @RequestBody gitDto: GitDto,
        authentication: Authentication,
    ): Mono<StringResponse> = upsertGitCredential(organizationName, gitDto, authentication, isUpdate = true)

    @DeleteMapping("/{organizationName}/delete-git")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "DELETE",
        summary = "Upsert organization git.",
        description = "Upsert organization git.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "url", `in` = ParameterIn.QUERY, description = "url of a git", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully deleted an organization git credentials and all corresponding data.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for deleting organization git credentials.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name.")
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "TOO_LONG_FUNCTION")
    fun deleteGit(
        @PathVariable organizationName: String,
        @RequestParam url: String,
        authentication: Authentication,
    ): Mono<StringResponse> = Mono.just(organizationName)
        .flatMap {
            organizationService.findByNameAndCreatedStatus(it).toMono()
        }
        .switchIfEmptyToNotFound {
            "Could not find an organization with name $organizationName."
        }
        .filter {
            organizationPermissionEvaluator.hasPermission(authentication, it, Permission.DELETE)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "Not enough permission for managing organization git credentials."
        }
        .asyncEffect { organization ->
            // Find all tests sources and remove all corresponding snapshots from storage
            blockingToFlux {
                val git = gitService.getByOrganizationAndUrl(organization, url)
                testSuitesSourceService.findByGit(git)
            }
                .flatMap { testsSourceSnapshotStorage.deleteAll(it) }
                .map { deleted ->
                    if (!deleted) {
                        logger.warn {
                            "Failed to clean-up tests snapshots for git url $url in $organizationName"
                        }
                    }
                }
                .then(Mono.just(Unit))
        }
        .map { organization ->
            // it removes TestSuitesSource and TestSuite by cascade constrain
            gitService.delete(organization, url)
        }
        .map {
            ResponseEntity.ok("Git credentials and corresponding data successfully deleted")
        }

    /**
     * @param organizationName
     * @param authentication
     * @return contest rating for organization
     */
    @GetMapping("/{organizationName}/get-organization-contest-rating")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "Get",
        summary = "Get organization contest rating.",
        description = "Get organization contest rating.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully get an organization contest rating.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name.")
    fun getOrganizationContestRating(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Mono<Double> = Mono.just(organizationName)
        .flatMap {
            organizationService.findByNameAndCreatedStatus(it).toMono()
        }
        .switchIfEmptyToNotFound {
            "Could not find an organization with name $organizationName."
        }
        .flatMap {
            organizationService.getGlobalRating(organizationName, authentication)
        }

    private fun getFilteredOrganizationDtoList(filters: OrganizationFilter, pageSize: Int): Flux<OrganizationDto> = blockingToFlux {
        organizationService.getFiltered(filters, Pageable.ofSize(pageSize))
    }.map { it.toDto() }

    private fun upsertGitCredential(
        organizationName: String,
        gitDto: GitDto,
        authentication: Authentication,
        isUpdate: Boolean
    ): Mono<StringResponse> = blockingToMono { organizationService.findByNameAndCreatedStatus(organizationName) }
        .switchIfEmptyToNotFound {
            "Could not find organization with name $organizationName"
        }
        .filter {
            organizationPermissionEvaluator.hasPermission(authentication, it, Permission.DELETE)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "Not enough permission to manage git in $organizationName."
        }
        .zipWith(validateGitCredential(gitDto))
        .filter { (_, isValid) -> isValid }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "Invalid git credential or connection problems (check you firewall) for url [${gitDto.url}]"
        }
        .flatMap { (organization, _) ->
            val existedGit = blockingToMono {
                gitService.findByOrganizationAndUrl(organization, gitDto.url)
            }
            if (isUpdate) {
                // expected that git already existed in case of update
                existedGit
                    .switchIfEmptyToNotFound {
                        "Not found git credential with url [${gitDto.url}] in $organizationName"
                    }
                    .map { git ->
                        git.apply {
                            url = gitDto.url
                            username = gitDto.username
                            password = gitDto.password
                        }
                    }
            } else {
                // if git already existed -> maps to error
                existedGit
                    .flatMap<Git?> {
                        Mono.error(
                            ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Already exist git credential with url [${gitDto.url}] in $organizationName"
                            )
                        )
                    }
                    .defaultIfEmpty(
                        Git(
                            url = gitDto.url,
                            username = gitDto.username,
                            password = gitDto.password,
                            organization = organization,
                        )
                    )
            }
        }
        .map {
            gitService.save(it)
            ResponseEntity.ok("Git credential saved")
        }

    private fun validateGitCredential(gitDto: GitDto) = webClientToPreprocessor
        .post()
        .uri("/git/check-connectivity")
        .bodyValue(gitDto)
        .retrieve()
        .bodyToMono<Boolean>()

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(OrganizationController::class.java)
    }
}
