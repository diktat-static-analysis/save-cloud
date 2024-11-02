package com.saveourtool.common.info

import com.saveourtool.common.domain.Role
import com.saveourtool.common.validation.Validatable
import com.saveourtool.common.validation.isValidName

import kotlin.js.JsExport
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Represents all data related to the User
 *
 * @property name name/login of the user
 * @property projects [String] of project name to [Role] of the user
 * @property email
 * @property avatar avatar of user
 * @property company
 * @property location
 * @property linkedin
 * @property gitHub
 * @property twitter
 * @property organizations
 * @property globalRole
 * @property id
 * @property status
 * @property originalLogins
 * @property rating
 * @property website
 * @property freeText
 * @property realName
 * @property createDate
 */
@Serializable
@JsExport
data class UserInfo(
    val name: String,
    val id: Long? = null,
    val originalLogins: Map<String, String> = emptyMap(),
    val projects: Map<String, Role> = emptyMap(),
    val organizations: Map<String, Role> = emptyMap(),
    val email: String? = null,
    val avatar: String? = null,
    val company: String? = null,
    val location: String? = null,
    val linkedin: String? = null,
    val gitHub: String? = null,
    val twitter: String? = null,
    val globalRole: Role? = null,
    val status: UserStatus = UserStatus.CREATED,
    val rating: Long = 0,
    val website: String? = null,
    val freeText: String? = null,
    val realName: String? = null,
    val createDate: LocalDateTime? = null,
) : Validatable {
    /**
     * Validation of organization name
     *
     * @return true if name is valid, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun validateName(): Boolean = name.isValidName()

    /**
     * Validation of an organization
     *
     * @return true if organization is valid, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    override fun validate(): Boolean = validateName()
}
