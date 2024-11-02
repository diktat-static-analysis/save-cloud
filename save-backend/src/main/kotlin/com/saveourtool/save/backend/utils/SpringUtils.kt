/**
 * This class contains util methods for Spring
 */

package com.saveourtool.save.backend.utils

import com.saveourtool.common.domain.EntitySaveStatus
import com.saveourtool.common.domain.Role
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication

/**
 * @return [ResponseEntity] with proper [HttpStatus]
 */
fun EntitySaveStatus.toResponseEntity(): ResponseEntity<EntitySaveStatus> = when (this) {
    EntitySaveStatus.CONFLICT, EntitySaveStatus.EXIST -> ResponseEntity.status(HttpStatus.CONFLICT).body(this)
    EntitySaveStatus.NEW, EntitySaveStatus.UPDATED -> ResponseEntity.ok(this)
    else -> throw NotImplementedError("Not supported save status $this")
}

/**
 * Check role out of [Authentication]
 *
 * @param role
 * @return true if user with [Authentication] has [role], false otherwise
 */
fun Authentication.hasRole(role: Role): Boolean = authorities.any { it.authority == role.asSpringSecurityRole() }
