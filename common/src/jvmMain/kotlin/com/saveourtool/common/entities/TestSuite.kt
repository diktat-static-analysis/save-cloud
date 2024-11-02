package com.saveourtool.common.entities

import com.saveourtool.common.domain.PluginType
import com.saveourtool.common.domain.pluginName
import com.saveourtool.common.domain.toPluginType
import com.saveourtool.common.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.common.testsuite.TestSuiteDto
import com.saveourtool.common.testsuite.TestSuiteVersioned
import com.saveourtool.common.utils.DATABASE_DELIMITER
import com.saveourtool.common.utils.PRETTY_DELIMITER

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * Keep in mind ExampleMatcher is used with this Entity in TestSuitesService
 * Do not forget to add new fields to ignorePaths of ExampleMatcher
 *
 * @property name name of the test suite
 * @property description description of the test suite
 * @property sourceSnapshot snapshot of source, which this test suite is created from
 * @property dateAdded date and time, when this test suite was added to the project
 * @property language
 * @property tags
 * @property plugins
 * @property isPublic
 */
@Suppress("LongParameterList")
@Entity
class TestSuite(
    var name: String = "Undefined",

    var description: String? = "Undefined",

    @ManyToOne
    @JoinColumn(name = "source_snapshot_id")
    var sourceSnapshot: TestsSourceSnapshot,

    var dateAdded: LocalDateTime? = null,

    var language: String? = null,

    var tags: String? = null,

    var plugins: String = "",

    var isPublic: Boolean = true,
) : BaseEntityWithDtoWithId<TestSuiteDto>() {
    /**
     * @return [plugins] as a list of string
     */
    fun pluginsAsListOfPluginType() = plugins.split(DATABASE_DELIMITER)
        .map { pluginName ->
            pluginName.toPluginType()
        }
        .filter { it != PluginType.GENERAL }

    /**
     * @return [tags] as a list of strings
     */
    fun tagsAsList() = tags?.split(DATABASE_DELIMITER)?.filter { it.isNotBlank() }.orEmpty()

    /**
     * @return Dto of testSuite
     */
    override fun toDto() =
            TestSuiteDto(
                this.name,
                this.description,
                this.sourceSnapshot.toDto(),
                this.language,
                this.tagsAsList(),
                this.id,
                this.pluginsAsListOfPluginType(),
                this.isPublic,
            )

    /**
     * @param version
     * @return [TestSuiteVersioned] created from [TestSuiteDto]
     */
    fun toVersioned(
        version: String,
    ): TestSuiteVersioned = TestSuiteVersioned(
        id = requiredId(),
        name = name,
        sourceName = sourceSnapshot.source.name,
        organizationName = sourceSnapshot.source.organization.name,
        isLatestFetchedVersion = version == sourceSnapshot.source.latestFetchedVersion,
        description = description.orEmpty(),
        version = version,
        language = language.orEmpty(),
        tags = tagsAsList().joinToString(PRETTY_DELIMITER),
        plugins = pluginsAsListOfPluginType().joinToString(PRETTY_DELIMITER) { it.pluginName() },
    )

    companion object {
        /**
         * Concatenates [tags] using same format as [TestSuite.tagsAsList]
         *
         * @param tags list of tags
         * @return representation of [tags] as a single string understood by [TestSuite.tagsAsList]
         */
        fun tagsFromList(tags: List<String>) = tags.joinToString(separator = DATABASE_DELIMITER)

        /**
         *  [plugins] by list of strings
         *
         * @param pluginNamesAsList list of string names of plugins
         * @return [String] of plugins separated by [DATABASE_DELIMITER] from [List] of [String]s
         */
        fun pluginsByNames(pluginNamesAsList: List<String>) = pluginNamesAsList.joinToString(DATABASE_DELIMITER)

        /**
         * Update [plugins] by list of strings
         *
         * @param pluginTypesAsList list of [PluginType]
         * @return [String] of plugins separated by [DATABASE_DELIMITER] from [List] of [PluginType]s
         */
        fun pluginsByTypes(pluginTypesAsList: List<PluginType>) = pluginsByNames(pluginTypesAsList.map { it.pluginName() })

        /**
         * @param sourceSnapshotResolver
         * @return [TestSuite] created from [TestSuiteDto]
         */
        fun TestSuiteDto.toEntity(sourceSnapshotResolver: (Long) -> TestsSourceSnapshot): TestSuite = TestSuite(
            name = name,
            description = description,
            sourceSnapshot = sourceSnapshotResolver(sourceSnapshot.requiredId()),
            dateAdded = null,
            language = language,
            tags = tags?.let(TestSuite::tagsFromList),
            plugins = pluginsByTypes(plugins)
        )
    }
}
