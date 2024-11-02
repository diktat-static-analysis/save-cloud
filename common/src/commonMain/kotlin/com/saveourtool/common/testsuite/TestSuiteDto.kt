package com.saveourtool.common.testsuite

import com.saveourtool.common.domain.PluginType
import com.saveourtool.common.entities.DtoWithId
import com.saveourtool.common.test.TestsSourceSnapshotDto
import kotlinx.serialization.Serializable

/**
 * @property name [com.saveourtool.save.entities.TestSuite.name]
 * @property description [com.saveourtool.save.entities.TestSuite.description]
 * @property sourceSnapshot [com.saveourtool.save.entities.TestsSourceSnapshot]
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
    val sourceSnapshot: TestsSourceSnapshotDto,
    val language: String? = null,
    val tags: List<String>? = null,
    override val id: Long? = null,
    val plugins: List<PluginType> = emptyList(),
    val isPublic: Boolean = true,
) : DtoWithId()
