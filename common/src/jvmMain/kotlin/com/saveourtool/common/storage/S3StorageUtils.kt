/**
 * Utilities for [Storage] which implements [AbstractS3Storage]
 */

package com.saveourtool.common.storage

/**
 * Delimiter for S3 key
 */
const val PATH_DELIMITER = "/"

/**
 * @return [this] as S3 common prefix -- ends on [PATH_DELIMITER]
 */
fun String.asS3CommonPrefix(): String = removeSuffix(PATH_DELIMITER) + PATH_DELIMITER

/**
 * @receiver key in S3 as [String]
 * @return parts [this] split by [PATH_DELIMITER]
 */
fun String.s3KeyToParts(): List<String> = removePrefix(PATH_DELIMITER).removeSuffix(PATH_DELIMITER).split(PATH_DELIMITER)

/**
 * @param parts should not end or start with [PATH_DELIMITER] -- will be deleted
 * @return a s3 key by concat [parts] and a single [PATH_DELIMITER] between them
 */
fun concatS3Key(vararg parts: String): String = parts.map { it.removePrefix(PATH_DELIMITER).removeSuffix(PATH_DELIMITER) }
    .filterNot { it.isEmpty() }
    .also {
        if (it.isEmpty()) {
            throw IllegalArgumentException("all parts cannot be empty")
        }
    }
    .joinToString(PATH_DELIMITER)
