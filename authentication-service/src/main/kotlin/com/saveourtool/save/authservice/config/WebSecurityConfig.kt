/**
 * Configuration beans for security in different profiles
 */

package com.saveourtool.save.authservice.config

import com.saveourtool.common.v1
import com.saveourtool.save.authservice.utils.SaveUserDetails.Companion.toSaveUserDetails
import com.saveourtool.save.authservice.utils.roleHierarchy

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.web.server.WebFilter
import reactor.kotlin.core.publisher.toMono

import javax.annotation.PostConstruct

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Profile("secure")
@Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
class WebSecurityConfig(
    private val defaultMethodSecurityExpressionHandler: DefaultMethodSecurityExpressionHandler
) {
    @Bean
    fun saveUserPreAuthenticatedProcessingWebFilter(): WebFilter {
        val authenticationManager = ReactiveAuthenticationManager { authentication -> authentication.toMono() }
        return AuthenticationWebFilter(authenticationManager)
            .also { authenticationWebFilter ->
                authenticationWebFilter.setServerAuthenticationConverter { exchange ->
                    exchange.request.headers.toSaveUserDetails()?.toPreAuthenticatedAuthenticationToken().toMono()
                }
            }
    }

    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.run {
        authorizeExchange()
            .pathMatchers(*publicEndpoints.toTypedArray())
            .permitAll()
            // resources for frontend
            .pathMatchers("/*.html", "/*.js*", "/*.css", "/img/**", "/*.ico", "/*.png", "/particles.json")
            .permitAll()
    }
        .and()
        .run {
            authorizeExchange()
                .pathMatchers("/**")
                .authenticated()
        }
        .and()
        .run {
            // FixMe: Properly support CSRF protection https://github.com/saveourtool/save-cloud/issues/34
            csrf().disable()
        }
        .addFilterAt(saveUserPreAuthenticatedProcessingWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
        .exceptionHandling {
            it.authenticationEntryPoint(
                HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
            )
        }
        .logout()
        .disable()
        .formLogin()
        .disable()
        .build()

    @PostConstruct
    fun postConstruct() {
        defaultMethodSecurityExpressionHandler.setRoleHierarchy(roleHierarchy())
    }

    companion object {
        /**
         * These endpoints will have `permitAll` enabled on them. We can't selectively put `@PreAuthorize("permitAll")` in the code,
         * because it won't allow us to configure authenticated access to all other endpoints by default.
         * Or we can use custom AccessDecisionManager later.
         */
        internal val publicEndpoints = listOf(
            "/",
            "/error",
            // All `/internal/**` and `/actuator/**` requests should be sent only from internal network,
            // they are not proxied from gateway.
            "/actuator/**",
            "/internal/**",
            // Agents should communicate without authorization
            "/heartbeat",
            // `CollectionView` is a public page
            "/api/$v1/projects/by-filters",
            "/api/$v1/awesome-benchmarks",
            "/api/$v1/check-git-connectivity-adaptor",
            // `OrganizationView` is a public page
            // fixme: when we will want to make organizations accessible for everyone, wi will need to add more endpoints here
            "/api/$v1/organizations/**",
            "/api/$v1/projects/get/projects-by-organization",
            // `ContestListView` and `ContestView` are public pages
            "/api/$v1/contests/*",
            "/api/$v1/contests/active",
            "/api/$v1/contests/finished",
            "/api/$v1/contests/*/public-test",
            "/api/$v1/contests/*/scores",
            "/api/$v1/contests/*/*/best",
            "/api/demo/*/run",
            "/api/$v1/vulnerabilities/by-filter",
            "/api/$v1/vulnerabilities/count/by-filter",
            // `fossGraphView` is public page
            "/api/$v1/vulnerabilities/by-identifier-with-description",
            "/api/$v1/vulnerabilities/download",
            "/api/$v1/comments/get-all",
            "/api/$v1/comments/get-all-count",
            "/api/$v1/users/all",
            "/api/$v1/users/by-prefix",
            "/api/$v1/users/*",
            "/api/$v1/avatar/**",
            // `ProjectView`'s getProject should be public, all the permission filtering is done on backend
            "/api/$v1/projects/get/organization-name",
            // info on tags should be public
            "/api/$v1/tags/**",
            // cosv history and changes tabs should be public
            "/api/$v1/cosv/list-versions",
            "/api/$v1/cosv/cosv-content",
        )
    }
}

@EnableWebFluxSecurity
@Profile("!secure")
@Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
class NoopWebSecurityConfig {
    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.authorizeExchange()
        .anyExchange()
        .permitAll()
        .and()
        .csrf()
        .disable()
        .build()
}

/**
 * @return a bean with default [PasswordEncoder], that can be used throughout the application
 */
@Bean
fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
