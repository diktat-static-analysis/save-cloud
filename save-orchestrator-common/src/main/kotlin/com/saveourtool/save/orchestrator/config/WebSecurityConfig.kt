package com.saveourtool.save.orchestrator.config

import com.saveourtool.save.spring.security.KubernetesAuthenticationUtils
import com.saveourtool.save.spring.security.ServiceAccountAuthenticatingManager
import com.saveourtool.save.spring.security.ServiceAccountTokenExtractorConverter
import com.saveourtool.save.spring.security.serviceAccountTokenAuthentication
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform
import org.springframework.boot.cloud.CloudPlatform
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

/**
 * Configuration class to set up Spring Security
 */
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Import(KubernetesAuthenticationUtils::class)
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
class KubernetesServiceAccountWebSecurityConfig {
    /**
     * Configures spring-security to use ServiceAccount based authentication.
     * Beans [serviceAccountTokenExtractorConverter] and [serviceAccountAuthenticatingManager] need to be passed into
     * [serviceAccountTokenAuthentication].
     */
    @Bean(name = ["kubernetesServiceAccountSecurityWebFilterChain"])
    @Suppress("KDOC_WITHOUT_PARAM_TAG", "KDOC_WITHOUT_RETURN_TAG")
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        serviceAccountTokenExtractorConverter: ServiceAccountTokenExtractorConverter,
        serviceAccountAuthenticatingManager: ServiceAccountAuthenticatingManager,
    ): SecurityWebFilterChain = http
        .securityMatcher(
            AndServerWebExchangeMatcher(
                ServerWebExchangeMatchers.anyExchange(),
                NegatedServerWebExchangeMatcher(
                    ServerWebExchangeMatchers.pathMatchers("/actuator/**")
                )
            )
        )
        .serviceAccountTokenAuthentication(serviceAccountTokenExtractorConverter, serviceAccountAuthenticatingManager)
        .csrf()
        .disable()
        .build()
}
