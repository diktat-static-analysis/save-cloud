package com.saveourtool.common.domain

import com.saveourtool.common.validation.NAMING_MAX_LENGTH
import kotlinx.serialization.Serializable

/**
 * Enum of user save status
 * @property message
 */
@Serializable
enum class UserSaveStatus(val message: String) {
    /**
     * Approved user
     */
    APPROVED("User successfully approved"),

    /**
     * Banned user
     */
    BANNED("User successfully banned"),

    /**
     * User has conflicting name
     */
    CONFLICT("This name is already taken"),

    /**
     * Deleted user
     */
    DELETED("User successfully deleted"),

    /**
     * User has conflicting name
     */
    FORBIDDEN("You are trying to update user, while it is not yet active"),

    /**
     * User name longer than [NAMING_MAX_LENGTH] characters
     */
    INVALID_NAME("Name must not be longer than $NAMING_MAX_LENGTH characters"),

    /**
     * New user
     */
    NEW("User saved successfully"),

    /**
     * Update user
     */
    UPDATE("User successfully updated"),
    ;
}
