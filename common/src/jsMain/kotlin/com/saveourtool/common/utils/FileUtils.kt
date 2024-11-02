/**
 * js implementation for FileUtils - Not implemented
 */

package com.saveourtool.common.utils

import okio.FileSystem
import okio.Path

const val NOT_IMPLEMENTED_ON_JS = "Cannot be used in js."

@Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
actual val fs: FileSystem by lazy { throw NotImplementedError(NOT_IMPLEMENTED_ON_JS) }
actual fun Path.markAsExecutable() {
    throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
}

actual fun ByteArray.writeToFile(file: Path, mustCreate: Boolean) {
    throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
}

actual inline fun <reified C : Any> parseConfig(configPath: Path): C = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
