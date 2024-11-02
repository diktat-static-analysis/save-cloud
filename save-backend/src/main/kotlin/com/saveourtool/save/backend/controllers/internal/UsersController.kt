package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.common.service.UserService
import com.saveourtool.common.utils.blockingToMono
import com.saveourtool.save.authservice.utils.SaveUserDetails

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

typealias SaveUserDetailsResponse = ResponseEntity<SaveUserDetails>

/**
 * Controller that handles operation with users
 */
@RestController
@RequestMapping("/internal/users")
class UsersController(
    private val userService: UserService,
) {
    /**
     * Stores user in the DB with provided [name] with default role.
     * And add a link to [source] for created user
     *
     * @param source user source
     * @param name user name
     */
    @PostMapping("/new/{source}/{name}")
    fun saveNewUserIfRequired(
        @PathVariable source: String,
        @PathVariable name: String,
    ): Mono<SaveUserDetailsResponse> = blockingToMono { userService.saveNewUserIfRequired(source, name) }
        .map {
            ResponseEntity.ok().body(SaveUserDetails(it))
        }

    /**
     * Find user by name
     *
     * @param userName user name
     * @return found Spring's UserDetails as a String
     */
    @GetMapping("/find-by-name/{userName}")
    fun findByName(
        @PathVariable userName: String,
    ): Mono<SaveUserDetailsResponse> = blockingToMono { userService.findByName(userName) }
        .map {
            ResponseEntity.ok().body(SaveUserDetails(it))
        }

    /**
     * Find user by name and source
     *
     * @param source user source
     * @param nameInSource user name
     * @return found Spring's UserDetails as a String
     */
    @GetMapping("/find-by-original-login/{source}/{nameInSource}")
    fun findByOriginalLogin(
        @PathVariable source: String,
        @PathVariable nameInSource: String,
    ): Mono<SaveUserDetailsResponse> = blockingToMono { userService.findByOriginalLogin(nameInSource, source) }
        .map {
            ResponseEntity.ok().body(SaveUserDetails(it))
        }
}
