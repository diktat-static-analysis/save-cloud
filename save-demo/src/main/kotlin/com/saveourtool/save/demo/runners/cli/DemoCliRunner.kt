package com.saveourtool.save.demo.runners.cli

import com.saveourtool.common.demo.DemoRunRequest
import com.saveourtool.common.utils.getLogger
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.runners.command.CommandBuilder
import com.saveourtool.save.demo.runners.command.CommandContext
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.demo.storage.toToolKey

import org.slf4j.Logger

import java.nio.file.Path

import kotlin.io.path.createTempDirectory

/**
 * [CliRunner] for provided [demo] with specific [version]
 */
class DemoCliRunner(
    dependencyStorage: DependencyStorage,
    private val commandBuilder: CommandBuilder,
    private val demo: Demo,
    private val version: String,
) : AbstractCliRunner(dependencyStorage) {
    override val log: Logger = Companion.log
    override val configName: String? = demo.configName
    override val testFileName: String = demo.fileName
    override val tmpDir: Path = createTempDirectory("demo-${demo.organizationName}-${demo.projectName}-")

    override fun getRunCommand(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        demoRunRequest: DemoRunRequest
    ): String {
        // download probably should be moved to another place
        val tools = dependencyStorage.blockingList(demo, version)
            .associate { dependency ->
                dependency.fileName to getExecutable(workingDir, dependency.toToolKey())
            }
        return commandBuilder.build(
            demo.getRunCommand(demoRunRequest.mode),
            CommandContext(
                testPath,
                tools,
                outputPath,
                configPath,
            )
        )
    }

    companion object {
        private val log: Logger = getLogger<DemoCliRunner>()
    }
}
