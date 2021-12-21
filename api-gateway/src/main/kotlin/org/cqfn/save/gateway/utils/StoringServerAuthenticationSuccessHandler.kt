package org.cqfn.save.gateway.utils

import org.cqfn.save.entities.User
import org.cqfn.save.gateway.config.ConfigurationProperties

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.jackson2.CoreJackson2Module
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * [ServerAuthenticationSuccessHandler] that sends user data to backend on successful login
 */
class StoringServerAuthenticationSuccessHandler(
    configurationProperties: ConfigurationProperties,
) : ServerAuthenticationSuccessHandler {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()
        .findAndRegisterModules()
        .registerModule(CoreJackson2Module())
        .registerModule(OAuth2ClientJackson2Module())
    private val webClient = WebClient.create(configurationProperties.backend.url)

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        logger.info("Authenticated user with authentication type ${authentication::class}")

        logger.info("Will send authentication as `${objectMapper.writeValueAsString(authentication)}`")

        val user = authentication.toUser()
        return webClient.post()
            .uri("/internal/users/new")
            .bodyValue(objectMapper.writeValueAsString(user))
            .retrieve()
            .toBodilessEntity()
            .then()
    }
}

/**
 * @return [User] with data from this [Authentication]
 */
fun Authentication.toUser(): User = User(
    userName(),
    null,
    authorities.joinToString(",") { it.authority },
    toIdentitySource(),
)
