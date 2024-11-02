/**
 * Utils to unzip the archive
 */

package com.saveourtool.common.utils

import com.saveourtool.save.core.logging.logDebug
import okio.Path

actual fun Path.extractZipTo(targetPath: Path) {
    require(fs.metadata(targetPath).isDirectory)
    logDebug("Unzip ${fs.canonicalize(this)} into ${fs.canonicalize(targetPath)}")
    platform.posix.system("unzip $this -d $targetPath")
}

actual fun Path.extractZipHere() = parent?.let {
    extractZipTo(it)
} ?: throw IllegalStateException("Path to archive is set incorrectly.")
