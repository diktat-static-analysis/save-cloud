package com.saveourtool.save.backend.controllers

import com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.common.domain.UserSaveStatus
import com.saveourtool.common.entities.User
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.info.UserStatus
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.service.UserService
import com.saveourtool.common.utils.*
import com.saveourtool.common.v1
import com.saveourtool.common.validation.isValidLengthName
import com.saveourtool.save.authservice.utils.SaveUserDetails
import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.backend.configs.ConfigProperties

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

/**
 * Controller that handles operation with users
 */
@RestController
@RequestMapping(path = ["/api/$v1/users"])
class UsersDetailsController(
    private val userRepository: UserRepository,
    private val userDetailsService: UserService,
    configProperties: ConfigProperties,
    jackson2WebClientCustomizer: WebClientCustomizer,
) {
    private val gatewayWebClient = WebClient.builder()
        .apply(jackson2WebClientCustomizer::customize)
        .baseUrl(configProperties.gatewayUrl)
        .build()

    /**
     * @param userName username
     * @return [UserInfo] info about user's
     */
    @GetMapping("/{userName}")
    @PreAuthorize("permitAll()")
    fun findByName(
        @PathVariable userName: String,
    ): Mono<UserInfo> = blockingToMono { userRepository.findByName(userName) }
        .map { it.toUserInfo() }
        .orNotFound()

    @GetMapping("/by-prefix")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get users by prefix.",
        description = "Get list of users by prefix with ids not in.",
    )
    @Parameters(
        Parameter(name = "prefix", `in` = ParameterIn.QUERY, description = "username prefix", required = true),
        Parameter(
            name = "pageSize",
            `in` = ParameterIn.QUERY,
            description = "amount of users that should be returned, default: 5",
            required = false
        ),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched users.")
    @RequiresAuthorizationSourceHeader
    fun findByPrefix(
        @RequestParam prefix: String,
        @RequestParam(required = false, defaultValue = "5") pageSize: Int,
        @RequestParam(required = false, defaultValue = "") namesToSkip: String,
    ): Flux<UserInfo> = namesToSkip.toMono()
        .map { stringNames -> stringNames.split(DATABASE_DELIMITER) }
        .map { stringNameList -> stringNameList.filter { it.isNotBlank() }.toSet() }
        .flatMapMany { nameList ->
            // `userRepository.findByNameStartingWithAndIdNotIn` with empty `nameList` results with empty list for some reason
            blockingToFlux {
                if (nameList.isNotEmpty()) {
                    userRepository.findByNameStartingWithAndNameNotIn(prefix, nameList, Pageable.ofSize(pageSize))
                } else {
                    userRepository.findByNameStartingWith(prefix, Pageable.ofSize(pageSize))
                }
            }
        }
        .map { it.toUserInfo() }

    /**
     * @return list of [UserInfo] info about user's
     */
    @GetMapping("/all")
    @PreAuthorize("permitAll()")
    fun findAll(): Flux<UserInfo> = blockingToFlux {
        userRepository.findAll().map { it.toUserInfo() }
    }

    /**
     * @param newUserInfo
     * @param authentication
     */
    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    @Suppress("MagicNumber")
    fun saveUser(@RequestBody newUserInfo: UserInfo, authentication: Authentication): Mono<StringResponse> = Mono.just(newUserInfo)
        .filter { newUserInfo.name.isValidLengthName() }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            UserSaveStatus.INVALID_NAME.message
        }
        .blockingMap {
            userRepository.findByIdOrNull(authentication.userId())
        }
        .switchIfEmptyToNotFound {
            "User with id ${authentication.userId()} not found in database"
        }
        .filter { user ->
            user.status in (userStatusWorkflow.keys)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            UserSaveStatus.FORBIDDEN.message
        }
        .blockingMap { user ->
            val oldName = user.name.takeUnless { it == newUserInfo.name }
            val oldStatus = user.status
            val newUser = user.apply {
                name = newUserInfo.name
                email = newUserInfo.email
                company = newUserInfo.company
                location = newUserInfo.location
                gitHub = newUserInfo.gitHub
                linkedin = newUserInfo.linkedin
                twitter = newUserInfo.twitter
                status = userStatusWorkflow.getValue(oldStatus)
                website = newUserInfo.website
                realName = newUserInfo.realName
                freeText = newUserInfo.freeText
            }
            userDetailsService.saveUser(
                newUser,
                oldName,
                oldStatus,
            ) to newUser
        }
        .flatMap { (status, newUser) ->
            when (status) {
                UserSaveStatus.UPDATE -> notifyGateway(newUser).map {
                    ResponseEntity.ok(status.message)
                }
                else -> ResponseEntity.status(HttpStatus.CONFLICT).body(status.message).toMono()
            }
        }

    /**
     * @param userName
     * @param token
     * @param authentication an [Authentication] representing an authenticated request
     * @return response
     */
    @PostMapping("{userName}/save/token")
    @PreAuthorize("isAuthenticated()")
    fun saveUserToken(
        @PathVariable userName: String,
        @RequestBody token: String,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono { userRepository.findByName(userName) }
        .requireOrSwitchToForbidden({ id == authentication.userId() })
        .blockingMap { user ->
            userRepository.save(user.apply {
                password = "{bcrypt}${BCryptPasswordEncoder().encode(token)}"
            })
        }
        .map {
            ResponseEntity.ok("User token saved successfully")
        }

    /**
     * @param authentication
     * @return [UserInfo] of authenticated user
     */
    @GetMapping("/user-info")
    fun getSelfUserInfo(authentication: Authentication?): Mono<UserInfo> = blockingToMono {
        authentication?.userId()?.let { userDetailsService.findById(it) }?.toUserInfo()
    }

    /**
     * @param userName
     * @param authentication
     */
    @GetMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    fun deleteUser(
        @RequestParam userName: String,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono {
        userDetailsService.deleteUser(userName, authentication)
    }
        .flatMap { status ->
            when (status) {
                UserSaveStatus.DELETED -> notifyGateway(authentication.userId()).map {
                    ResponseEntity.ok(status.message)
                }
                else -> ResponseEntity.status(HttpStatus.CONFLICT).body(status.message)
                    .toMono()
            }
        }

    /**
     * @param userName
     */
    @GetMapping("/ban")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    fun banUser(
        @RequestParam userName: String,
    ): Mono<StringResponse> = blockingToMono {
        userDetailsService.banUser(userName)
    }
        .flatMap { status ->
            when (status) {
                UserSaveStatus.BANNED -> notifyGateway(userName).map {
                    ResponseEntity.ok(status.message)
                }
                else -> ResponseEntity.status(HttpStatus.CONFLICT).body(status.message)
                    .toMono()
            }
        }

    /**
     * @return list of [UserInfo] info about users
     */
    @GetMapping("/new-users")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    fun findNewUsers(): Flux<UserInfo> = blockingToFlux { userRepository.findByStatus(UserStatus.NOT_APPROVED).map { it.toUserInfo() } }

    /**
     * @return count new users
     */
    @GetMapping("/new-users-count")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    fun findCountNewUsers(): Mono<Int> = blockingToMono { userRepository.countByStatus(UserStatus.NOT_APPROVED) }

    /**
     * @param userName
     */
    @GetMapping("/approve")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    fun approveUser(
        @RequestParam userName: String,
    ): Mono<StringResponse> = blockingToMono {
        userDetailsService.approveUser(userName)
    }
        .flatMap { status ->
            when (status) {
                UserSaveStatus.APPROVED -> notifyGateway(userName).map {
                    ResponseEntity.ok(status.message)
                }
                else -> ResponseEntity.status(HttpStatus.CONFLICT).body(status.message)
                    .toMono()
            }
        }

    private fun notifyGateway(userId: Long): Mono<Unit> = blockingToMono {
        userRepository.findByIdOrNull(userId)
    }
        .switchIfEmptyToNotFound { "Not found user by id=$userId" }
        .flatMap { user -> notifyGateway(user) }

    private fun notifyGateway(userName: String): Mono<Unit> = blockingToMono {
        userRepository.findByName(userName)
    }
        .switchIfEmptyToNotFound { "Not found user by name=$userName" }
        .flatMap { user -> notifyGateway(user) }

    private fun notifyGateway(user: User): Mono<Unit> = gatewayWebClient.patch()
        .uri("/internal/sec/update")
        .bodyValue(SaveUserDetails(user))
        .retrieve()
        .toBodilessEntity()
        .thenReturn(Unit)

    companion object {
        private val userStatusWorkflow = mapOf(
            UserStatus.CREATED to UserStatus.NOT_APPROVED,
            UserStatus.ACTIVE to UserStatus.ACTIVE,
        )
    }
}
