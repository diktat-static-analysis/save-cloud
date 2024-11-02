/**
 * Utils to set up the environment for demo
 */

package com.saveourtool.save.demo.agent.utils

import com.saveourtool.common.demo.DemoConfiguration
import com.saveourtool.common.utils.*
import com.saveourtool.common.utils.fs
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logError
import com.saveourtool.save.core.logging.logInfo
import com.saveourtool.save.core.utils.ExecutionResult
import com.saveourtool.save.core.utils.ProcessBuilder

import okio.Path
import okio.Path.Companion.toPath

private const val SETUP_SH_LOGS_FILENAME = "setup.logs"
private const val CWD = "."

/**
 * Download all the required files from save-demo
 *
 * @param demoUrl url to save-demo
 * @param setupShTimeoutMillis amount of milliseconds to run setup.sh if it is present
 * @param demoConfiguration all the information required for tool download
 * @throws IllegalStateException when it was caught from [downloadDemoFiles]
 */
suspend fun setupEnvironment(demoUrl: String, setupShTimeoutMillis: Long, demoConfiguration: DemoConfiguration) {
    logInfo("Setting up the environment...")

    try {
        downloadDemoFiles(demoUrl, demoConfiguration)
    } catch (e: IllegalStateException) {
        logError("Error while downloading files to agent: ${e.describe()}.")
        throw e
    }.unzip()

    logDebug("All files successfully downloaded.")

    val executionResult = executeSetupSh(setupShTimeoutMillis)
    executionResult?.let {
        if (executionResult.code != 0) {
            logError("Setup script has finished with ${executionResult.code} code.")
        } else {
            logInfo("The environment is successfully set up.")
        }
    } ?: logInfo("No setup script was executed.")

    logInfo("The environment is successfully set up.")
}

private fun executeSetupSh(setupShTimeoutMillis: Long, setupShName: String = "setup.sh"): ExecutionResult? = setupShName.takeIf {
    fs.exists(it.toPath())
}
    ?.let { setupSh ->
        ProcessBuilder(true, fs).exec(
            "./$setupSh",
            CWD,
            SETUP_SH_LOGS_FILENAME.toPath(),
            setupShTimeoutMillis,
        )
    }

private suspend fun downloadDemoFiles(demoUrl: String, demoConfiguration: DemoConfiguration): Path {
    val url = with(demoConfiguration) { "$demoUrl/internal/demo/files/$organizationName/$projectName/download-as-zip?version=$version" }
    return downloadDemoFiles(url)
}

private suspend fun downloadDemoFiles(url: String, archiveName: String = "archive.zip"): Path = archiveName.toPath()
    .also { download("tool", url, it) }
