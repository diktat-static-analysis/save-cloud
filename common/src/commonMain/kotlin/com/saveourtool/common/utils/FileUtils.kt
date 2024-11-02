/**
 * Utility methods to work with files using Okio
 */

package com.saveourtool.common.utils

import com.saveourtool.save.core.files.findAllFilesMatching
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logInfo
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

expect val fs: FileSystem

/**
 * Mark [this] file as executable. Sets permissions to rwxr--r--
 */
expect fun Path.markAsExecutable()

/**
 * Write content of [this] into a file [file]
 *
 * @receiver [ByteArray] to be written into a file
 * @param file target [Path]
 * @param mustCreate will be passed to Okio's [FileSystem.write]
 */
expect fun ByteArray.writeToFile(file: Path, mustCreate: Boolean = true)

/**
 * Unzip [Path] if it is .zip file.
 *
 * @param markAsExecutables if true, marks all files as executables (true by default)
 */
fun Path.unzipIfRequired(markAsExecutables: Boolean = true) {
    if (name.endsWith(".zip")) {
        unzip(markAsExecutables)
    }
}

/**
 * Unzip the archive by [Path].
 *
 * Notice that [Path] is not checked to really be an existing path to zip-archive.
 *
 * @param markAsExecutables if true, marks all files as executables (true by default)
 */
fun Path.unzip(markAsExecutables: Boolean = true) {
    val parentDir = requireNotNull(parent)
    extractZipTo(parentDir)
    fs.delete(this, mustExist = true)
    // fixme: need to store information about isExecutable in FileKey
    if (markAsExecutables) {
        parentDir.findAllFilesMatching {
            if (fs.metadata(it).isRegularFile) {
                it.markAsExecutable()
            }
            true
        }
    }
    logDebug("Extracted archive into working dir and deleted $this")
}

/**
 * Parse config file
 * Notice that [C] should be serializable
 *
 * @param configPath path to toml config file
 * @return [C] filled with configuration information
 */
expect inline fun <reified C : Any> parseConfig(configPath: Path): C

/**
 * Parse config file
 * Notice that [C] should be serializable
 *
 * @param configName name of a toml config file, agent.toml by default
 * @return [C] filled with configuration information
 */
inline fun <reified C : Any> parseConfig(configName: String = "agent.toml"): C = parseConfig<C>(configName.toPath())
    .also { logInfo("Found ${configName.toPath()}.") }

/**
 * Parse config file or apply default if none was found
 * Notice that [C] should be serializable
 *
 * @param defaultConfig config that should be set if no config was found ([FileNotFoundException])
 * @param configName name of a toml config file, agent.toml by default
 * @return [C] filled with configuration information
 */
inline fun <reified C : Any> parseConfigOrDefault(
    defaultConfig: C,
    configName: String = "agent.toml",
): C = try {
    parseConfig(configName)
} catch (e: FileNotFoundException) {
    logInfo("Config file $configName not found, falling back to default config.")
    defaultConfig
}
