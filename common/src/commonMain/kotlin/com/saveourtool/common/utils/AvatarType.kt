package com.saveourtool.common.utils

import kotlinx.serialization.Serializable

/**
 * Types of avatar
 *
 * @property urlPath
 */
@Serializable
enum class AvatarType(val urlPath: String) {
    /**
     * Organization avatar
     */
    ORGANIZATION("organizations"),

    /**
     * User avatar
     */
    USER("users"),
    ;

    /**
     * @param objectName
     * @return url to [objectName] for [AvatarType]
     */
    fun toUrlStr(objectName: String): String = "/$urlPath/$objectName"

    companion object {
        /**
         * @param urlPathCandidate
         * @return [AvatarType] found by provided [urlPath] or null
         */
        fun findByUrlPath(urlPathCandidate: String): AvatarType? = values().singleOrNull { it.urlPath == urlPathCandidate }
    }
}
