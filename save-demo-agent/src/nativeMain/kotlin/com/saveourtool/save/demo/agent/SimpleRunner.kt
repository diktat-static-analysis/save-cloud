/**
 * Simple runner that uses ProcessBuilder to run the tool
 */

package com.saveourtool.save.demo.agent

import com.saveourtool.common.demo.DemoAgentConfig
import com.saveourtool.common.demo.DemoResult
import com.saveourtool.common.demo.DemoRunRequest
import com.saveourtool.common.demo.RunConfiguration
import com.saveourtool.common.utils.createAndWrite
import com.saveourtool.common.utils.createAndWriteIfNeeded
import com.saveourtool.common.utils.createTempDir
import com.saveourtool.common.utils.fs
import com.saveourtool.save.core.files.readLines
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.utils.ProcessBuilder

import okio.Path
import okio.Path.Companion.toPath

import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val PROCESS_BUILDER_TIMEOUT_MILLIS = 20_000L

/**
 * @param runRequest [DemoRunRequest]
 * @param dirPath [Path] to directory where files are expected to be present (e.g. tempdir path)
 * @return run command where [RunConfiguration.configFileName], [RunConfiguration.inputFileName] and [RunConfiguration.outputFileName]
 *  have [dirPath] prepended
 */
fun RunConfiguration.getRelativeRunCommand(
    runRequest: DemoRunRequest,
    dirPath: Path,
) = requireNotNull(runCommands[runRequest.mode]) { "Could not find run command for mode ${runRequest.mode}." }
    .appendPath(inputFileName, dirPath)
    .appendPath(configFileName, dirPath)
    .appendPath(outputFileName, dirPath)

private fun String.appendPath(oldSubstring: String?, pathToPrepend: Path) = oldSubstring?.let {
    replace(it, "${pathToPrepend / it}")
} ?: this

/**
 * @param demoRunRequest [DemoRunRequest] that contains all the information about current run request
 * @param deferredConfig [CompletableDeferred] filled with [DemoAgentConfig] corresponding to _this_ demo
 * @return [DemoResult] filled with the results of demo run
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runDemo(demoRunRequest: DemoRunRequest, deferredConfig: CompletableDeferred<DemoAgentConfig>): DemoResult {
    require(demoRunRequest.mode.isNotBlank()) { "Demo mode should not be blank." }

    val runConfig = deferredConfig.getCompleted().runConfiguration
    val tempDir = fs.createTempDir()
    val runCommand = runConfig.getRelativeRunCommand(demoRunRequest, tempDir)

    val (inputFile, _) = createRequiredFiles(demoRunRequest, runConfig, tempDir)
    val outputFile = runConfig.outputFileName?.let { tempDir / it }

    return try {
        run(runCommand, inputFile, outputFile)
    } finally {
        cleanUp(tempDir)
    }
}

private fun createRequiredFiles(
    demoRunRequest: DemoRunRequest,
    config: RunConfiguration,
    dirPath: Path = ".".toPath(),
): Pair<Path, Path?> = fs.createAndWrite(config.inputFileName, demoRunRequest.codeLines, dirPath) to
        fs.createAndWriteIfNeeded(config.configFileName, demoRunRequest.config)

private fun cleanUp(dirPath: Path) = fs.deleteRecursively(dirPath, true)

@OptIn(ExperimentalTime::class)
private fun run(
    runCommand: String,
    inputFile: Path,
    outputFile: Path?
): DemoResult {
    val pb = ProcessBuilder(false, fs)
    val executionResult = measureTimedValue {
        pb.exec(runCommand, ".", null, PROCESS_BUILDER_TIMEOUT_MILLIS)
    }
        .let {
            logDebug("ProcessBuilder took ${it.duration}")
            it.value
        }
    val warnings = outputFile?.takeIf { fs.exists(it) }?.let { fs.readLines(it) }.orEmpty()

    return DemoResult(
        warnings = warnings,
        stdout = executionResult.stdout,
        stderr = executionResult.stderr,
        outputText = fs.readLines(inputFile),
        terminationCode = executionResult.code,
    )
}
