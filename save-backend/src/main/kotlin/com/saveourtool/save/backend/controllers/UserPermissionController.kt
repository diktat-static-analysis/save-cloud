package com.saveourtool.save.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.info.UserPermissions
import com.saveourtool.common.utils.blockingToMono
import com.saveourtool.common.v1
import com.saveourtool.save.backend.security.UserPermissionEvaluator

import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Controller for user permissions.
 */
@ApiSwaggerSupport
@RestController
@RequestMapping(path = ["/api/$v1"])
class UserPermissionController(
    private val userPermissionEvaluator: UserPermissionEvaluator,
) {
    /**
     * @param authentication
     * @return UserPermissions
     */
    @GetMapping("/users/permissions")
    fun getUserPermissions(
        authentication: Authentication,
    ): Mono<UserPermissions> = blockingToMono {
        userPermissionEvaluator.getUserPermissions(authentication)
    }

    /**
     * @param authentication
     * @param organizationName
     * @return UserPermissions
     */
    @GetMapping("/users/permissions-by-organization")
    fun getUserPermissions(
        authentication: Authentication,
        @RequestParam organizationName: String,
    ): Mono<UserPermissions> = blockingToMono {
        userPermissionEvaluator.getUserPermissionsByOrganizationName(authentication, organizationName)
    }
}
