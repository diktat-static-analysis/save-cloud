/**
 * Utilities for testing of security-related logic
 */

package com.saveourtool.save.backend.utils

import com.saveourtool.save.authservice.utils.SaveUserDetails
import com.saveourtool.common.info.UserStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

/**
 * @param id user.id for a mocked user in test security context
 */
internal fun mutateMockedUser(id: Long, status: UserStatus = UserStatus.ACTIVE) {
    SecurityContextHolder.getContext().apply {
        authentication = SaveUserDetails(
            id = id,
            name = authentication.name,
            role = (authentication as UsernamePasswordAuthenticationToken).authorities.joinToString(",") { it.authority },
            status = status.toString(),
            token = null,
        ).toPreAuthenticatedAuthenticationToken()
    }
}
