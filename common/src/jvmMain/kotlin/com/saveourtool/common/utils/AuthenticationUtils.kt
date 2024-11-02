@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.common.utils

import com.saveourtool.common.domain.Role
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails

/**
 * Check role out of [Authentication]
 *
 * @param role
 * @return true if user with [Authentication] has [role], false otherwise
 */
fun Authentication.hasRole(role: Role): Boolean = authorities.any { it.authority == role.asSpringSecurityRole() }

/**
 * Extract username from this [Authentication].
 * We assume here that most of the authentications are created by [ConvertingAuthenticationManager],
 * so `principal` is a String, containing identity source.
 *
 * @return username
 */
fun Authentication.username(): String = when (principal) {
    // this should be the most common branch, as requests are authenticated by `ConvertingAuthenticationManager`
    is String -> (principal as String).split(':').last()
    is UserDetails -> (principal as UserDetails).username
    else -> error("Authentication instance $this has unsupported type of principal: $principal of type ${principal::class}")
}
