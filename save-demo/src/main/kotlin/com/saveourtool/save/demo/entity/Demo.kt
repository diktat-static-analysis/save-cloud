package com.saveourtool.save.demo.entity

import com.saveourtool.common.demo.DemoConfiguration
import com.saveourtool.common.demo.DemoDto
import com.saveourtool.common.demo.RunConfiguration
import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.domain.toSdk
import com.saveourtool.common.spring.entity.BaseEntityWithDto
import javax.persistence.*

/**
 * Entity that encapsulates all the information required for tool download and run
 *
 * @property organizationName name of organization from saveourtool
 * @property projectName name of project from saveourtool
 * @property sdk sdk required for demo run
 * @property runCommands list of [RunCommand] entities
 * @property fileName name that the tested input file should have
 * @property configName name of tool config file (or null if no config is needed)
 * @property outputFileName name of output file (or null if [outputFileName] is [fileName])
 * @property githubOrganizationName name of organization from GitHub
 * @property githubProjectName name of project from GitHub
 */
@Entity
@Suppress("LongParameterList")
class Demo(
    var organizationName: String,
    var projectName: String,
    var sdk: String,
    var fileName: String,
    var configName: String?,
    var outputFileName: String?,
    @Column(name = "github_organization")
    var githubOrganizationName: String?,
    @Column(name = "github_project")
    var githubProjectName: String?,
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "demo",
        targetEntity = RunCommand::class,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    var runCommands: List<RunCommand> = emptyList(),
) : BaseEntityWithDto<DemoDto>() {
    private fun githubProjectCoordinates() = githubOrganizationName?.let { organization ->
        githubProjectName?.let { project ->
            ProjectCoordinates(
                organization,
                project,
            )
        }
    }

    /**
     * @return saveourtool [ProjectCoordinates]
     */
    fun projectCoordinates() = ProjectCoordinates(
        organizationName,
        projectName,
    )

    override fun toDto(): DemoDto = DemoDto(
        projectCoordinates(),
        "",
        runCommands.toRunCommandsMap(),
        fileName,
        sdk.toSdk(),
        configName,
        outputFileName,
        githubProjectCoordinates(),
    )

    /**
     * @return [RunConfiguration] for agent filled with data from this [Demo]
     */
    fun toRunConfiguration() = RunConfiguration(
        fileName,
        configName,
        runCommands.toRunCommandsMap(),
        outputFileName,
    )

    /**
     * @param version version that is requested for demo
     * @return [DemoConfiguration] for agent filled with data from this [Demo]
     */
    fun toDemoConfiguration(version: String) = DemoConfiguration(
        organizationName,
        projectName,
        version,
    )

    /**
     * @param mode name of mode that demo should be run on
     * @return run command for [mode]
     */
    fun getRunCommand(mode: String): String {
        require(mode.isNotBlank()) { "Demo mode should not be blank." }
        return requireNotNull(runCommands.find { it.modeName == mode }) {
            "Could not find run command for mode $mode."
        }.command
    }
}

/**
 * __Notice__ that newly created demo has neither [Demo.id] nor [Demo.runCommands] initialized
 *
 * @return [Demo] entity filled with [DemoDto] data
 */
fun DemoDto.toDemo() = Demo(
    projectCoordinates.organizationName,
    projectCoordinates.projectName,
    sdk.toString(),
    fileName,
    configName,
    outputFileName,
    githubProjectCoordinates?.organizationName,
    githubProjectCoordinates?.projectName,
    emptyList()
)
