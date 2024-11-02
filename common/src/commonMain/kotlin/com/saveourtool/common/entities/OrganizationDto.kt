package com.saveourtool.common.entities

import com.saveourtool.common.utils.getCurrentLocalDateTime
import com.saveourtool.common.validation.Validatable
import com.saveourtool.common.validation.isValidName
import kotlinx.datetime.LocalDateTime

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * @property name organization
 * @property status
 * @property dateCreated date created organization
 * @property avatar
 * @property description
 * @property canCreateContests
 * @property canBulkUpload ability to bulk upload cosv files
 * @property rating
 */
@Serializable
data class OrganizationDto(
    val name: String,
    var status: OrganizationStatus = OrganizationStatus.CREATED,
    @Contextual
    val dateCreated: LocalDateTime = getCurrentLocalDateTime(),
    val avatar: String? = null,
    val description: String = "",
    val canCreateContests: Boolean = false,
    val canBulkUpload: Boolean = false,
    val rating: Long = 0,
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

    companion object {
        /**
         * Value that represents an empty [OrganizationDto]
         */
        val empty = OrganizationDto(
            name = "",
        )
    }
}
