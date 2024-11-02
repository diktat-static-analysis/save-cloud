/**
 * Utils to unzip the archive
 */

package com.saveourtool.common.utils

import okio.Path

actual fun Path.extractZipTo(targetPath: Path) {
    throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
}

actual fun Path.extractZipHere() {
    throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
}
