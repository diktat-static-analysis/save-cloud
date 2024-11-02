package com.saveourtool.common.utils.github

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @property name the name of the file.
 * @property size the size of the file.
 * @property rawContentType the MIME type of the file.
 * @property downloadUrl the URL at which the file can be downloaded.
 */
@Serializable
data class ReleaseAsset(
    val name: String,
    val size: Long,
    @SerialName("content_type")
    val rawContentType: String,
    @SerialName("browser_download_url")
    val downloadUrl: String
) {
    /**
     * @return `true` if this asset is an MD5 or an SHA digest, `false` otherwise.
     */
    fun isDigest(): Boolean =
            digestSuffixes().any { suffix ->
                name.endsWith(suffix)
            }

    /**
     * @return the MIME `Content-Type` of the file.
     */
    fun contentType(): ContentType =
            when (val separatorIndex = rawContentType.indexOf('/')) {
                -1 -> ContentType(rawContentType, "*")
                else -> ContentType(
                    rawContentType.substring(0, separatorIndex),
                    rawContentType.substring(separatorIndex + 1)
                )
            }

    private companion object {
        private val knownDigestNames = arrayOf("md5")

        private fun digestSuffixes(): Sequence<String> =
                sequence {
                    yield(".asc")

                    yieldAll(knownDigestNames.asSequence().map(String::lowercase).flatMap { digest ->
                        sequenceOf(".$digest", ".asc.$digest")
                    })
                }
    }
}
