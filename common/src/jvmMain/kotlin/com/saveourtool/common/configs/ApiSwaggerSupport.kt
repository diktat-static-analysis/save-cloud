/**
 * Utilities related to Swagger
 */

package com.saveourtool.common.configs

import com.saveourtool.common.utils.AUTHORIZATION_ID
import com.saveourtool.common.utils.AUTHORIZATION_NAME
import com.saveourtool.common.utils.AUTHORIZATION_ROLES

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme

/**
 * Meta-annotation that sets common swagger annotations for a `/api` controller.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SecurityScheme(name = "basic", type = SecuritySchemeType.HTTP, scheme = "basic")
@SecurityRequirement(name = "basic")
@ApiResponses(
    ApiResponse(responseCode = "401", description = "Unauthorized", content = [])
)
annotation class ApiSwaggerSupport

/**
 * Indicates that an operation requires `X-Authorization-*` headers to be set
 */
@Parameter(
    `in` = ParameterIn.HEADER,
    name = AUTHORIZATION_ID,
    required = true,
    example = "123"
)
@Parameter(
    `in` = ParameterIn.HEADER,
    name = AUTHORIZATION_NAME,
    required = true,
    example = "user"
)
@Parameter(
    `in` = ParameterIn.HEADER,
    name = AUTHORIZATION_ROLES,
    required = true,
    example = "VIEWER"
)
annotation class RequiresAuthorizationSourceHeader
