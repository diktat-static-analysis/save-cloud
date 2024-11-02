/**
 * Controller for processing links between users and their roles in organizations:
 * 1) to put new roles of users
 * 2) to get users and their roles by organization
 * 3) to remove users from organizations
 */

package com.saveourtool.cosv.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.Organization
import com.saveourtool.common.entities.OrganizationWithUsers
import com.saveourtool.common.filters.OrganizationFilter
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.permission.SetRoleRequest
import com.saveourtool.common.security.OrganizationPermissionEvaluator
import com.saveourtool.common.service.LnkUserOrganizationService
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.utils.StringResponse
import com.saveourtool.common.utils.switchIfEmptyToNotFound
import com.saveourtool.common.utils.switchIfEmptyToResponseException
import com.saveourtool.common.utils.username
import com.saveourtool.common.v1

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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Controller for processing links between users and their roles in organizations
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "roles"),
    Tag(name = "organizations"),
)
@RestController
@RequestMapping("/api/$v1/organizations")
class LnkUserOrganizationController(
    private val lnkUserOrganizationService: LnkUserOrganizationService,
    private val organizationService: OrganizationService,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
) {
    @GetMapping("/{organizationName}/users")
    @Operation(
        method = "GET",
        summary = "Get list of users that are connected with given organization.",
        description = "Get list of users that are connected with given organization.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched contest by it's name.")
    @ApiResponse(responseCode = "404", description = "Contest with such name was not found.")
    fun getAllUsersByOrganizationName(
        @PathVariable organizationName: String,
    ): Mono<List<UserInfo>> = organizationService.findByNameAndCreatedStatus(organizationName)
        .toMono()
        .switchIfEmptyToNotFound {
            ORGANIZATION_NOT_FOUND_ERROR_MESSAGE
        }
        .map {
            lnkUserOrganizationService.getAllUsersAndRolesByOrganization(it)
        }
        .map { mapOfPermissions ->
            mapOfPermissions.map { (user, role) ->
                user.toUserInfo(organizations = mapOf(organizationName to role))
            }
        }

    @GetMapping("/{organizationName}/users/roles")
    @Operation(
        method = "GET",
        summary = "Get user's role in organization with given name.",
        description = "If userName is not present, then will return the role of current user in given organization, " +
                "otherwise will return role of user with name userName in organization with name organizationName.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "name of a user", required = false),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched user's role.")
    @ApiResponse(responseCode = "403", description = "You are not allowed to see requested user's role.")
    @ApiResponse(responseCode = "404", description = "Requested user or organization doesn't exist.")
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "UnsafeCallOnNullableType")
    fun getRole(
        @PathVariable organizationName: String,
        authentication: Authentication?,
    ): Mono<Role> = authentication?.let {
        getUserAndOrganizationWithPermissions(
            authentication.username(),
            organizationName,
            Permission.READ,
            authentication,
        )
            .map { (user, organization) ->
                lnkUserOrganizationService.getRole(user, organization)
            }
    } ?: Role.NONE.toMono()

    @PostMapping("/{organizationName}/users/roles")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Set user's role in organization with given name.",
        description = "Set user's role in organization with given name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "setRoleRequest", `in` = ParameterIn.DEFAULT, description = "pair of userName and role that is requested to be set", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Permission added")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or organization doesn't exist")
    fun setRole(
        @PathVariable organizationName: String,
        @RequestBody setRoleRequest: SetRoleRequest,
        authentication: Authentication,
    ): Mono<StringResponse> = getUserAndOrganizationWithPermissions(setRoleRequest.userName, organizationName, Permission.WRITE, authentication)
        .filter { (user, organization) ->
            organizationPermissionEvaluator.canChangeRoles(organization, authentication, user, setRoleRequest.role)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            FORBIDDEN_ERROR_MESSAGE
        }
        .map { (user, organization) ->
            lnkUserOrganizationService.setRole(user, organization, setRoleRequest.role)
            ResponseEntity.ok(
                "Successfully set role ${setRoleRequest.role} to user ${user.name} in organization ${organization.name}"
            )
        }

    @DeleteMapping("/{organizationName}/users/roles/{userName}")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "DELETE",
        summary = "Remove user's role in organization with given name.",
        description = "Remove user's role in organization with given name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "userName", `in` = ParameterIn.PATH, description = "name of user whose role is requested to be removed", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Role was successfully removed")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or organization doesn't exist")
    fun removeRole(
        @PathVariable organizationName: String,
        @PathVariable userName: String,
        authentication: Authentication,
    ): Mono<StringResponse> = getUserAndOrganizationWithPermissions(userName, organizationName, Permission.WRITE, authentication)
        .filter { (user, organization) ->
            organizationPermissionEvaluator.canChangeRoles(organization, authentication, user)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            FORBIDDEN_ERROR_MESSAGE
        }
        .map { (user, organization) ->
            lnkUserOrganizationService.removeRole(user, organization)
            ResponseEntity.ok("Successfully removed role of user ${user.name} in organization ${organization.name}")
        }

    @GetMapping("/{organizationName}/users/not-from")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get all users not from organization with names starting with a given prefix.",
        description = "Get all users not connected with organization with name organizationName whose names start with the same prefix.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "prefix", `in` = ParameterIn.QUERY, description = "prefix of username", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of users")
    @ApiResponse(responseCode = "404", description = "Requested organization doesn't exist")
    @Suppress("TOO_LONG_FUNCTION")
    fun getAllUsersNotFromOrganizationWithNamesStartingWith(
        @PathVariable organizationName: String,
        @RequestParam prefix: String,
    ): Mono<List<UserInfo>> = Mono.just(organizationName)
        .filter {
            prefix.isNotEmpty()
        }
        .flatMap {
            organizationService.findByNameAndCreatedStatus(it).toMono()
        }
        .switchIfEmptyToNotFound {
            "No organization with name $organizationName was found."
        }
        .map { organization ->
            lnkUserOrganizationService.getAllUsersAndRolesByOrganization(organization)
        }
        .map { users ->
            users.map { (user, _) -> user.requiredId() }.toSet()
        }
        .map { organizationUserIds ->
            organizationUserIds to lnkUserOrganizationService.getNonOrganizationUsersByName(prefix, organizationUserIds)
        }
        .map { (organizationUserIds, exactMatchUsers) ->
            exactMatchUsers to
                    lnkUserOrganizationService.getNonOrganizationUsersByNamePrefix(
                        prefix,
                        organizationUserIds + exactMatchUsers.map { it.requiredId() },
                        PAGE_SIZE - exactMatchUsers.size,
                    )
        }
        .map { (exactMatchUsers, prefixUsers) ->
            (exactMatchUsers + prefixUsers).map { it.toUserInfo() }
        }
        .defaultIfEmpty(emptyList())

    @GetMapping("/can-create-contests")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get all user's organizations that can create contests.",
        description = "Get all organizations that can create contests where user is a member.",
    )
    @ApiResponse(responseCode = "200", description = "Role removed")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or organization doesn't exist")
    fun getAllUsersOrganizationsThatCanCreateContests(
        authentication: Authentication,
    ): Flux<Organization> = Flux.fromIterable(
        lnkUserOrganizationService.getSuperOrganizationsWithRole(authentication.username())
    )

    @PostMapping("/by-filters")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get the list of organizations available to the current user and matching the filters, if any",
        description = "Get organizations by filters available for the current user.",
    )
    @Parameters(
        Parameter(name = "filters", `in` = ParameterIn.DEFAULT, description = "organization filters", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched organization infos.")
    @ApiResponse(responseCode = "404", description = "Could not find user with this id.")
    @Suppress("UnsafeCallOnNullableType")
    fun getOrganizationWithRolesAndFilters(
        @RequestBody organizationFilter: OrganizationFilter,
        authentication: Authentication,
    ): Flux<OrganizationWithUsers> = Mono.justOrEmpty(
        lnkUserOrganizationService.findUserByName(authentication.username())
    )
        .switchIfEmptyToNotFound()
        .flatMapIterable {
            lnkUserOrganizationService.getOrganizationsAndRolesByUserAndFilters(it, organizationFilter)
        }
        .map {
            OrganizationWithUsers(
                organization = it.organization.toDto(),
                userRoles = mapOf(it.user.name to it.role),
            )
        }

    private fun getUserAndOrganizationWithPermissions(
        userName: String,
        organizationName: String,
        permission: Permission,
        authentication: Authentication?,
    ) = Mono.just(userName)
        .filter {
            it.isNotBlank()
        }
        .switchIfEmptyToNotFound {
            USER_NOT_FOUND_ERROR_MESSAGE
        }
        .zipWith(
            organizationService.findByNameAndCreatedStatus(organizationName).toMono()
        )
        .switchIfEmptyToNotFound {
            ORGANIZATION_NOT_FOUND_ERROR_MESSAGE
        }
        .filter { (_, organization) ->
            organizationPermissionEvaluator.hasPermission(authentication, organization, permission)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN)
        .flatMap { (userName, organization) ->
            Mono.zip(
                lnkUserOrganizationService.getUserByName(userName).toMono(),
                organization.toMono()
            )
        }
        .switchIfEmptyToNotFound {
            USER_NOT_FOUND_ERROR_MESSAGE
        }

    companion object {
        private const val FORBIDDEN_ERROR_MESSAGE = "Not enough permission."
        private const val ORGANIZATION_NOT_FOUND_ERROR_MESSAGE = "Organization with such name does not exist."
        const val PAGE_SIZE = 5
        private const val USER_NOT_FOUND_ERROR_MESSAGE = "User with such name does not exist."
    }
}
