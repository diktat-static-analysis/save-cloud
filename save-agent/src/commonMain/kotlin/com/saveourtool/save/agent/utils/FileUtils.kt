/**
 * Utility methods to work with files using Okio
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.common.utils.fs
import okio.FileNotFoundException
import okio.Path.Companion.toPath

/**
 * Read file as a list of strings
 *
 * @param filePath a file to read
 * @return list of string from file
 */
internal fun readFile(filePath: String): List<String> = try {
    val path = filePath.toPath()
    fs.read(path) {
        generateSequence { readUtf8Line() }.toList()
    }
} catch (e: FileNotFoundException) {
    logErrorCustom("Not able to find file in the following path: $filePath")
    emptyList()
}
