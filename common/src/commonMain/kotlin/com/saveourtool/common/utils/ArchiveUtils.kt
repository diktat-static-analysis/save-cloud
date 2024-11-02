/**
 * Utils to unzip the archive
 */

package com.saveourtool.common.utils

import okio.Path

const val ZIP_ARCHIVE_EXTENSION = ".zip"

/**
 * Extract zip to dir with [targetPath]
 *
 * @param targetPath path to extract archive to
 */
expect fun Path.extractZipTo(targetPath: Path)

/**
 * Extract zip to dir where the archive is located
 */
expect fun Path.extractZipHere()
