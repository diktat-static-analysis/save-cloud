package com.saveourtool.save.gateway.controller

import com.saveourtool.common.info.OauthProviderInfo
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.web.bind.annotation.*

/**
 * Controller that returns various security information from Gateway
 */
@RestController
@RequestMapping("/sec")
class SecurityInfoController(
    private val clientRegistrationRepository: InMemoryReactiveClientRegistrationRepository,
) {
    /**
     * @return a list of [OauthProviderInfo] for all configured providers
     */
    @GetMapping("/oauth-providers")
    fun listOauthProviders() = clientRegistrationRepository.map {
        OauthProviderInfo(
            it.registrationId,
            // Default authorization link format,
            // see https://docs.spring.io/spring-security/reference/reactive/oauth2/login/advanced.html#webflux-oauth2-login-advanced-login-page
            "/oauth2/authorization/${it.registrationId}",
        )
    }
}
