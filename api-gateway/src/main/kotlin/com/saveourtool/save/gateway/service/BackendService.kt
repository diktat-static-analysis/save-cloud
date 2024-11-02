package com.saveourtool.save.gateway.service

import com.saveourtool.common.entities.User
import com.saveourtool.common.utils.SAVE_USER_ID_ATTRIBUTE
import com.saveourtool.common.utils.orNotFound
import com.saveourtool.common.utils.switchIfEmptyToResponseException
import com.saveourtool.save.authservice.utils.SaveUserDetails
import com.saveourtool.save.gateway.config.ConfigurationProperties

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

import java.security.Principal

/**
 * A service to backend to lookup users in DB
 */
@Service
class BackendService(
    configurationProperties: ConfigurationProperties,
    private val saveUserDetailsCache: SaveUserDetailsCache,
) {
    private val webClient = WebClient.create(configurationProperties.backend.url)

    /**
     * @param username
     * @return [UserDetails] found in DB by received name
     */
    fun findByName(
        username: String,
    ): Mono<SaveUserDetails> = findAuthenticationUserDetails("/internal/users/find-by-name/$username")

    /**
     * @param source
     * @param nameInSource
     * @return [UserDetails] found in DB by source and name in this source
     */
    fun findByOriginalLogin(
        source: String,
        nameInSource: String,
    ): Mono<SaveUserDetails> = findAuthenticationUserDetails("/internal/users/find-by-original-login/$source/$nameInSource")

    private fun findAuthenticationUserDetails(uri: String): Mono<SaveUserDetails> = webClient.get()
        .uri(uri)
        .retrieve()
        .getSaveUserDetails()

    /**
     * Find current user [SaveUserDetails] by [principal].
     *
     * @param principal current user [Principal]
     * @param session current [WebSession]
     * @return current user [SaveUserDetails]
     */
    fun findByPrincipal(principal: Principal, session: WebSession): Mono<SaveUserDetails> = when (principal) {
        is OAuth2AuthenticationToken -> session.getSaveUserDetails().switchIfEmpty {
            findByOriginalLogin(principal.authorizedClientRegistrationId, principal.name)
        }
        is UsernamePasswordAuthenticationToken -> (principal.principal as? SaveUserDetails)
            .toMono()
            .switchIfEmptyToResponseException(HttpStatus.INTERNAL_SERVER_ERROR) {
                "Unexpected principal type ${principal.principal.javaClass} in ${UsernamePasswordAuthenticationToken::class}"
            }
        else -> Mono.error(BadCredentialsException("Unsupported authentication type: ${principal::class}"))
    }

    /**
     * Saves a new [User] in DB
     *
     * @param source
     * @param nameInSource
     * @return empty [Mono]
     */
    fun createNewIfRequired(source: String, nameInSource: String): Mono<SaveUserDetails> = webClient.post()
        .uri("/internal/users/new/$source/$nameInSource")
        .contentType(MediaType.APPLICATION_JSON)
        .retrieve()
        .getSaveUserDetails()
        .doOnNext {
            saveUserDetailsCache.save(it)
        }

    private fun WebClient.ResponseSpec.getSaveUserDetails(): Mono<SaveUserDetails> = this
        .onStatus({ it.is4xxClientError }) {
            Mono.error(ResponseStatusException(it.statusCode()))
        }
        .toEntity<SaveUserDetails>()
        .flatMap { responseEntity ->
            responseEntity.body.toMono().orNotFound { "Authentication body is empty" }
        }

    private fun WebSession.getSaveUserDetails(): Mono<SaveUserDetails> = this
        .getAttribute<Long>(SAVE_USER_ID_ATTRIBUTE)
        .toMono()
        .switchIfEmptyToResponseException(HttpStatus.INTERNAL_SERVER_ERROR) {
            "Not found attribute $SAVE_USER_ID_ATTRIBUTE for ${OAuth2AuthenticationToken::class}"
        }
        .mapNotNull { id ->
            saveUserDetailsCache.get(id)
        }
}
