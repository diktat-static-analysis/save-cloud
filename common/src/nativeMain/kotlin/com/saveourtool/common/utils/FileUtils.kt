/**
 * Native implementation for FileUtils
 */

package com.saveourtool.common.utils

import com.akuleshov7.ktoml.file.TomlFileReader
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.*

import kotlin.system.getTimeNanos
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.serialization.serializer

actual val fs: FileSystem = FileSystem.SYSTEM

@OptIn(ExperimentalForeignApi::class)
actual fun Path.markAsExecutable() {
    val mode: mode_t = (S_IRUSR or S_IWUSR or S_IXUSR or S_IRGRP or S_IROTH).convert()
    chmod(this.toString(), mode)
}

actual fun ByteArray.writeToFile(file: Path, mustCreate: Boolean) {
    fs.write(file = file, mustCreate = mustCreate) { write(this@writeToFile).flush() }
}

/**
 * Write [lines] to file with name [fileName]
 *
 * @param fileName name of a file
 * @param lines lines to be written to file with name [fileName]
 * @param dirPath path to directory where a file should be created
 * @return path to file
 */
fun FileSystem.createAndWrite(
    fileName: String,
    lines: List<String>,
    dirPath: Path = ".".toPath(),
) = dirPath.div(fileName).also { path ->
    write(path, true) { lines.forEach { codeLine -> writeUtf8("$codeLine\n") } }
}

/**
 * Write [lines] to file with name [fileName] if needed
 *
 * @param fileName name of a file
 * @param lines lines to be written to file with name [fileName]
 * @param dirPath path to directory where a file should be created
 * @return path to file if both [fileName] and [lines] are provided, null otherwise
 */
fun FileSystem.createAndWriteIfNeeded(
    fileName: String?,
    lines: List<String>?,
    dirPath: Path = ".".toPath(),
) = fileName?.let { dirPath / it }?.also { path ->
    write(path, true) { lines?.forEach { codeLine -> writeUtf8("$codeLine\n") } }
}

/**
 * Create temporary directory
 *
 * @param mustCreate if true and file is already created, IOException is thrown
 * @return path to newly-created temp dir
 */
fun FileSystem.createTempDir(mustCreate: Boolean = true) = getTimeNanos()
    .toString()
    .toPath()
    .also { createDirectory(it, mustCreate) }

actual inline fun <reified C : Any> parseConfig(configPath: Path): C = TomlFileReader.decodeFromFile(
    serializer(),
    configPath.toString(),
)
