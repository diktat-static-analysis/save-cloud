package com.saveourtool.common.demo

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.domain.Sdk
import kotlinx.serialization.Serializable

typealias RunCommandMap = Map<String, String>
typealias RunCommandPair = Pair<String, String>

/**
 * @property projectCoordinates saveourtool project coordinates
 * @property vcsTagName release tag that defines the version to be fetched
 * @property runCommands [RunCommandMap] where key is mode name and value is run command for that mode
 * @property fileName name of an input file
 * @property sdk required sdk for tool run
 * @property configName name of config file or null if no config file is consumed
 * @property outputFileName name of output file (or null if [outputFileName] is [fileName])
 * @property githubProjectCoordinates GitHub project coordinates
 */
@Serializable
data class DemoDto(
    val projectCoordinates: ProjectCoordinates,
    val vcsTagName: String,
    val runCommands: RunCommandMap,
    val fileName: String,
    val sdk: Sdk = Sdk.Default,
    val configName: String? = null,
    val outputFileName: String? = null,
    val githubProjectCoordinates: ProjectCoordinates? = null,
) {
    /**
     * @return true of [DemoDto] is valid, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun validate(): Boolean = !projectCoordinates.consideredBlank() && fileName.isNotBlank() && validateRunCommands()

    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    private fun validateRunCommands(): Boolean = with(runCommands) {
        isNotEmpty() && keys.all { key -> key.isNotBlank() } && values.all { value -> value.isNotBlank() }
    }

    /**
     * @param mode name of mode that demo should be run on
     * @return run command for [mode]
     */
    fun getRunCommand(mode: String): String {
        require(mode.isNotBlank()) { "Demo mode should not be blank." }
        return requireNotNull(runCommands[mode]) { "Could not find run command for mode $mode." }
    }

    /**
     * @return list of mode names
     */
    fun getAvailableMods(): List<String> = runCommands.keys.toList()

    companion object {
        /**
         * Amount of [DemoDto]s that should be fetched by default
         */
        const val DEFAULT_FETCH_NUMBER = 10
        val empty = emptyForProject("", "")

        /**
         * @param organizationName saveourtool organization name
         * @param projectName saveourtool project name
         * @return [DemoDto] filled only with saveourtool project data
         */
        fun emptyForProject(organizationName: String, projectName: String) = DemoDto(
            ProjectCoordinates(organizationName, projectName),
            "",
            emptyMap(),
            "",
            Sdk.Default,
            null,
            null,
            ProjectCoordinates("", ""),
        )
    }
}
