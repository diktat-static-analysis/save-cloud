package com.saveourtool.save.entities

import com.saveourtool.save.domain.PluginType
import com.saveourtool.save.domain.pluginName
import com.saveourtool.save.domain.toPluginType
import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteVersioned
import com.saveourtool.save.utils.DATABASE_DELIMITER
import com.saveourtool.save.utils.PRETTY_DELIMITER

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property name name of the test suite
 * @property description description of the test suite
 * @property sourceSnapshot snapshot of source, which this test suite is created from
 * @property dateAdded date and time, when this test suite was added to the project
 * @property language
 * @property tags
 * @property plugins
 * @property isPublic
 * @property sourceVersion version of source, which this test suite is created from
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
        id = this.requiredId(),
        name = this.name,
        sourceName = this.sourceSnapshot.source.name,
        organizationName = this.sourceSnapshot.source.organization.name,
        isLatestFetchedVersion = version == this.sourceSnapshot.source.latestFetchedVersion,
        description = this.description.orEmpty(),
        version = version,
        language = this.language.orEmpty(),
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
         * @param sourceVersionResolver
         * @return [TestSuite] created from [TestSuiteDto]
         */
        fun TestSuiteDto.toEntity(sourceVersionResolver: (Long) -> TestsSourceVersion): TestSuite = TestSuite(
            name = name,
            description = description,
            sourceVersion = sourceVersionResolver(sourceVersionId),
            dateAdded = null,
            language = language,
            tags = tags?.let(TestSuite::tagsFromList),
            plugins = pluginsByTypes(plugins)
        )
    }
}
