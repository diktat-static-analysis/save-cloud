package com.saveourtool.save.testsuite

import com.saveourtool.save.domain.PluginType
import com.saveourtool.save.entities.DtoWithId
import kotlinx.serialization.Serializable

/**
 * @property name [com.saveourtool.save.entities.TestSuite.name]
 * @property description [com.saveourtool.save.entities.TestSuite.description]
 * @property sourceVersionId ID of [com.saveourtool.save.entities.TestSuitesSourceVersion]
 * @property language [com.saveourtool.save.entities.TestSuite.language]
 * @property tags [com.saveourtool.save.entities.TestSuite.tags]
 * @property id ID of saved entity or null
 * @property plugins
 * @property isPublic
 */
@Serializable
data class TestSuiteDto(
    val name: String,
    val description: String?,
    val sourceVersionId: Long,
    val language: String? = null,
    val tags: List<String>? = null,
    override val id: Long? = null,
    val plugins: List<PluginType> = emptyList(),
    val isPublic: Boolean = true,
) : DtoWithId()
