package com.saveourtool.common.testsuite

import com.saveourtool.common.entities.DtoWithId
import com.saveourtool.common.entities.GitDto
import com.saveourtool.common.validation.Validatable
import com.saveourtool.common.validation.isValidName
import com.saveourtool.common.validation.isValidPath
import kotlinx.serialization.Serializable

typealias TestSuitesSourceDtoList = List<TestSuitesSourceDto>

/**
 * @property organizationName
 * @property name
 * @property description
 * @property gitDto
 * @property testRootPath
 * @property latestFetchedVersion
 * @property id ID of saved entity or null
 */
@Serializable
data class TestSuitesSourceDto(
    val organizationName: String,
    val name: String,
    val description: String?,
    val gitDto: GitDto,
    val testRootPath: String,
    val latestFetchedVersion: String?,
    override val id: Long? = null,
) : Validatable, DtoWithId() {
    override fun validate(): Boolean = validateName() && validateOrganizationName() && validateTestRootPath() && gitDto.validate()

    /**
     * @return true if name is valid, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun validateName(): Boolean = name.isValidName()

    /**
     * @return true if [organizationName] is set, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    private fun validateOrganizationName(): Boolean = name.isNotBlank()

    /**
     * @return true if [testRootPath] is valid, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun validateTestRootPath(): Boolean = testRootPath.isBlank() || testRootPath.isValidPath()

    companion object {
        val empty = TestSuitesSourceDto(
            "",
            "",
            "",
            GitDto.empty,
            "",
            null,
        )
    }
}
