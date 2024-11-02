package com.saveourtool.common.security

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.Comment
import com.saveourtool.common.entities.CommentDto
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.utils.hasRole

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * Class that is capable of assessing user permissions.
 *
 * TODO: move all the hasPermission methods here
 */
@Component
class CommentPermissionEvaluator {
    /**
     * Check permission for user to read, write and delete [Comment]s by its [CommentDto]
     *
     * @param authentication
     * @param comment
     * @param permission
     * @return true if user with [authentication] has [permission] for [comment]
     */
    fun hasPermission(
        authentication: Authentication?,
        comment: CommentDto,
        permission: Permission,
    ): Boolean {
        authentication ?: return false

        if (authentication.hasRole(Role.SUPER_ADMIN)) {
            return true
        }

        return when (permission) {
            Permission.READ -> true
            else -> comment.userName == authentication.name
        }
    }
}
