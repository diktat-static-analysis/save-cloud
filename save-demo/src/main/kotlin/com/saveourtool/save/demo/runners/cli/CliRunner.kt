package com.saveourtool.save.demo.runners.cli

import com.saveourtool.common.demo.DemoResult
import com.saveourtool.common.demo.DemoRunRequest
import com.saveourtool.save.demo.runners.Runner
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.utils.isWindows
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Interface that should be implemented by all the runners that use [ProcessBuilder] in order to run tools for demo.
 */
@Component
interface CliRunner : Runner {
    /**
     * Path to temp directory
     */
    val tmpDir: Path

    /**
     * Name of an input file
     */
    val testFileName: String

    /**
     * Save [lines] into file with [filePath]
     *
     * @param filePath path to a new file
     * @param lines string that should be printed to file, if null, nothing is done
     * @return file with [lines]
     */
    fun prepareFile(filePath: Path, lines: String?): Path? = lines?.let { fileLines ->
        filePath.apply {
            writeText(fileLines)
        }
    }

    /**
     * @param workingDir the directory where the tool is run
     * @param toolKey storage key to find requested tool
     * @return executable file diktat
     */
    fun getExecutable(workingDir: Path, toolKey: ToolKey): Path

    /**
     * @param workingDir the directory where the tool is run
     * @param testPath path to test file
     * @param outputPath path to file with tool run report
     * @param configPath path to config file, if null, default config is used
     * @param demoRunRequest params of type [DemoRunRequest]
     * @return run command that depends on the system it is run on
     */
    fun getRunCommand(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        demoRunRequest: DemoRunRequest
    ): String

    /**
     * @param runCommand command that should be used to run tool
     * @return [ProcessBuilder] that is almost ready to be run
     * @throws NotImplementedError when run on system that is not supported
     */
    fun createProcessBuilder(runCommand: String): ProcessBuilder = when {
        isWindows() -> ProcessBuilder("cmd.exe", "/C", "\"$runCommand\"")
        else -> ProcessBuilder("sh", "-c", runCommand)
    }

    /**
     * @param testPath path to test file
     * @param demoRunRequest params of type [DemoRunRequest]
     * @return result of demo run as [DemoResult] wrapped into [Mono]
     */
    fun run(testPath: Path, demoRunRequest: DemoRunRequest): Mono<DemoResult>

    override fun run(demoRunRequest: DemoRunRequest): Mono<DemoResult> = runInTempDir(
        demoRunRequest,
        tmpDir,
        testFileName,
    )

    /**
     * @param demoRunRequest params of type [DemoRunRequest]
     * @param tempRootDir path to root of temp directories (somewhere in storage)
     * @param testFileName test file name that should be
     * @param additionalDirectoryTree additional directory names that should be in directory hierarchy to working dir (below randomly generated dir)
     * @return result as [DemoResult]
     */
    private fun runInTempDir(
        demoRunRequest: DemoRunRequest,
        tempRootDir: Path,
        testFileName: String,
        additionalDirectoryTree: List<String> = emptyList(),
    ) = run {
        if (!tempRootDir.exists()) {
            tempRootDir.createDirectory()
        }
    }
        .let {
            createTempDirectory(tempRootDir)
        }
        .let { currentTempRootPath ->
            currentTempRootPath to additionalDirectoryTree.fold(currentTempRootPath) { tempRootPath, pathItem ->
                tempRootPath / pathItem
            }
                .also { tempRootPath -> tempRootPath.createDirectories() }
        }
        .let { (createdTempDir, workingDir) ->
            try {
                val testPath = requireNotNull(prepareFile(workingDir / testFileName, demoRunRequest.codeLines.joinToString("\n")))
                run(testPath, demoRunRequest)
            } finally {
                createdTempDir.toFile().deleteRecursively()
            }
        }
}
