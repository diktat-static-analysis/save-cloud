/**
 * Utils for building curl command that downloads save-agent and save-demo-agent to pod
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.common.utils

import com.saveourtool.common.storage.impl.InternalFileKey
import org.intellij.lang.annotations.Language

@Language("bash")
private val defaultShellOptions: Sequence<String> = sequenceOf(
    "errexit",
    "nounset",
    "xtrace",
)

@Language("bash")
private val defaultCurlOptions: Sequence<String> = sequenceOf(
    "-vvv",
    "--fail",
)

/**
 * Get command for newly created pod that would download agent [fileKey] from [downloadUrl]
 * using curl with [curlOptions], chmod downloaded executable and launch it.
 *
 * Default options for shell ([defaultShellOptions]):
 * - `set -e` | `set -o errexit`: exit immediately if any command has a non-zero status;
 * - `set -u` | `set -o nounset`: exit immediately if a referenced variable is undefined;
 * - `set -x` | `set -o xtrace`: enable debugging (PS4 followed by command & args).
 *
 * Default options for curl ([defaultCurlOptions]):
 * - `--fail` is necessary so that `curl` exits immediately upon an HTTP 404;
 * - `-vvv` logging level.
 *
 * @param downloadUrl url to download agent
 * @param fileKey [InternalFileKey] for agent to download
 * @param shellOptions options to be set on command execution, [defaultShellOptions] by default
 * @param curlOptions options to be passed to curl, [defaultCurlOptions] by default
 * @return run command
 */
fun downloadAndRunAgentCommand(
    downloadUrl: String,
    fileKey: InternalFileKey,
    shellOptions: Sequence<String> = defaultShellOptions,
    curlOptions: Sequence<String> = defaultCurlOptions,
): String = with(fileKey) {
    "set ${getShellOptions(shellOptions)}" +
            " && curl ${getCurlOptions(curlOptions)} '$downloadUrl' --output $fileName" +
            " && chmod +x $fileName" +
            " && ./$fileName"
}

/**
 * @return [shellOptions] as a single string.
 * @see shellOptions
 */
@Language("bash")
private fun getShellOptions(shellOptions: Sequence<String>): String =
        shellOptions.map { option ->
            "-o $option"
        }.joinToString(separator = " ")

/**
 * @return [curlOptions] as a single string.
 * @see curlOptions
 */
@Language("bash")
private fun getCurlOptions(curlOptions: Sequence<String>): String = curlOptions.joinToString(separator = " ")
