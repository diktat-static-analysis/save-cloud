package com.saveourtool.save.gateway.utils

import com.saveourtool.common.utils.SAVE_USER_ID_ATTRIBUTE
import com.saveourtool.save.gateway.service.BackendService

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import reactor.core.publisher.Mono

/**
 * [ServerAuthenticationSuccessHandler] that sends user data to backend on successful login
 */
class StoringServerAuthenticationSuccessHandler(
    private val backendService: BackendService,
) : ServerAuthenticationSuccessHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        logger.info("Authenticated user ${authentication.name} with authentication type ${authentication::class}, will send data to backend")

        val (source, nameInSource) = if (authentication is OAuth2AuthenticationToken) {
            authentication.authorizedClientRegistrationId to authentication.principal.name
        } else {
            throw BadCredentialsException("Not supported authentication type ${authentication::class}")
        }
        return backendService.createNewIfRequired(source, nameInSource).flatMap { saveUser ->
            webFilterExchange.exchange.session.map {
                it.attributes[SAVE_USER_ID_ATTRIBUTE] = saveUser.id
            }
        }.then()
    }
}
