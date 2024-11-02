package com.saveourtool.save.demo.runners.cli

import com.saveourtool.common.demo.DemoResult
import com.saveourtool.common.demo.DemoRunRequest
import com.saveourtool.common.utils.blockingToMono
import com.saveourtool.common.utils.collectToFile
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.utils.prependPath

import org.slf4j.Logger
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

import java.io.FileNotFoundException
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path

import kotlin.io.path.*

/**
 * @property dependencyStorage
 */
abstract class AbstractCliRunner(
    protected val dependencyStorage: DependencyStorage,
    private val reportFileName: String = "report",
    private val stdoutFileName: String = "stdout",
    private val stderrFileName: String = "stderr",
) : CliRunner {
    /**
     * logger from child
     */
    protected abstract val log: Logger

    /**
     * configName, can be missed
     */
    protected abstract val configName: String?

    override fun run(testPath: Path, demoRunRequest: DemoRunRequest): Mono<DemoResult> = blockingToMono {
        val workingDir = testPath.parent
        val configPath = demoRunRequest.config?.joinToString("\n")
            ?.let { configContent ->
                val requiredConfigName = requireNotNull(configName) {
                    "config is provided for cli runner which doesn't support it"
                }
                prepareFile(workingDir / requiredConfigName, configContent)
            }
        val outputPath = workingDir / reportFileName
        val stdoutPath = workingDir / stdoutFileName
        val stderrPath = workingDir / stderrFileName
        val command = getRunCommand(workingDir, testPath, outputPath, configPath, demoRunRequest)
        val processBuilder = createProcessBuilder(command).apply {
            redirectErrorStream(true)
            redirectOutput(ProcessBuilder.Redirect.appendTo(stdoutPath.toFile()))
            redirectError(ProcessBuilder.Redirect.appendTo(stderrPath.toFile()))

            /*
             * Inherit JAVA_HOME for the child process.
             */
            val javaHome = System.getProperty("java.home")
            environment()["JAVA_HOME"] = javaHome
            /*
             * Need to remove JAVA_TOOL_OPTIONS (and _JAVA_OPTIONS just in case) because JAVA_TOOL_OPTIONS is set by spring,
             * so it breaks ktlint's "java -version" parsing (for ktlint 0.46.1, fixed in ktlint 0.47.1)
             */
            environment().remove("JAVA_TOOL_OPTIONS")
            environment().remove("_JAVA_OPTIONS")
            prependPath(Path(javaHome) / "bin")
        }

        log.debug("Running command [$command].")
        val terminationCode = processBuilder.start().waitFor()

        val stdout = stdoutPath.readLines()
        val stderr = stderrPath.readLines()

        val warnings = try {
            outputPath.readLines()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

        stdout.forEach(log::trace)
        stderr.forEach(log::error)

        log.trace("Found ${warnings.size} warning(s): [${warnings.joinToString(", ")}]")

        DemoResult(
            warnings.map { it.replace(testPath.absolutePathString(), testPath.name) },
            testPath.readLines(),
            stdout,
            stderr,
            terminationCode,
        )
    }

    override fun getExecutable(workingDir: Path, toolKey: ToolKey): Path = with(toolKey) {
        dependencyStorage.findDependency(organizationName, projectName, version, fileName)
    }
        .switchIfEmpty {
            throw FileNotFoundException("Could not find file with key $toolKey")
        }
        .flatMapMany { dependencyStorage.download(it) }
        .collectToFile(workingDir / toolKey.fileName)
        .thenReturn(workingDir / toolKey.fileName)
        .block()
        .let { requireNotNull(it) }
        .apply { toFile().setExecutable(true, false) }
}
