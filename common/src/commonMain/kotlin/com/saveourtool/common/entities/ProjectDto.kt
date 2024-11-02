package com.saveourtool.common.entities

import com.saveourtool.common.validation.Validatable
import com.saveourtool.common.validation.isValidEmail
import com.saveourtool.common.validation.isValidName
import com.saveourtool.common.validation.isValidUrl
import kotlinx.serialization.Serializable

/**
 * Project data transfer object
 *
 * @property name name of a project
 * @property url url of a project
 * @property description description of a project
 * @property status
 * @property isPublic flag is project public or not
 * @property email email of a project
 * @property numberOfContainers
 * @property organizationName name of an organization that is connected to the project
 * @property contestRating
 */
@Serializable
data class ProjectDto(
    val name: String,
    val url: String = "",
    val description: String = "",
    var status: ProjectStatus = ProjectStatus.CREATED,
    val isPublic: Boolean = true,
    val email: String = "",
    var numberOfContainers: Int = 3,
    val organizationName: String,
    var contestRating: Double = 0.0,
) : Validatable {
    /**
     * Project name validation
     *
     * @return true if name is valid, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun validateProjectName(): Boolean = name.isValidName()

    /**
     * Project's organization name validation
     *
     * @return true if organizationName is valid, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun validateOrganizationName(): Boolean = organizationName.isValidName()

    /**
     * Project's URL validation
     *
     * @return true if URL is valid or empty, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun validateUrl(): Boolean = if (url.isNotEmpty()) {
        url.isValidUrl()
    } else {
        true
    }

    /**
     * Project's email validation
     *
     * @return true if email is valid or empty, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun validateEmail() = if (email.isNotEmpty()) {
        email.isValidEmail()
    } else {
        true
    }

    /**
     * [ProjectDto]'s validation
     *
     * @return true if name, organizationName, url and email are valid, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    override fun validate(): Boolean = validateProjectName() && validateOrganizationName() && validateUrl() && validateEmail()

    companion object {
        /**
         * Value of an empty [ProjectDto]
         */
        val empty = ProjectDto(
            name = "",
            organizationName = "",
        )
    }
}
