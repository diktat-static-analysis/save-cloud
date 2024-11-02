/**
 * Utils methods to back up unexpected keys
 */

package com.saveourtool.common.storage

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.key.AbstractS3KeyDatabaseManager
import com.saveourtool.common.utils.ListCompletableFuture
import com.saveourtool.common.utils.debug
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.utils.warn

import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response

import java.util.concurrent.CompletableFuture

import kotlinx.datetime.Clock

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
private val log = getLogger {}

/**
 * Back up unexpected s3 key (according to [s3KeyValidator]) which are detected S3 storage (by common prefix [commonPrefix])
 *
 * @param storageName
 * @param commonPrefix
 * @param s3KeyValidator
 * @return [CompletableFuture] without body
 */
fun S3Operations.backupUnexpectedKeys(
    storageName: String,
    commonPrefix: String,
    s3KeyValidator: (String) -> Boolean,
): CompletableFuture<Unit> = detectUnexpectedKeys(commonPrefix, s3KeyValidator)
    .thenComposeAsync { unexpectedKeys ->
        if (unexpectedKeys.isNotEmpty()) {
            doBackupUnexpectedKeys(storageName, commonPrefix, unexpectedKeys)
        } else {
            CompletableFuture.completedFuture(Unit)
        }
    }

/**
 * Back up unexpected s3 key (according to [AbstractS3KeyDatabaseManager]) which are detected S3 storage (by [AbstractS3KeyDatabaseManager])
 *
 * @param storageName
 * @param s3KeyManager
 * @return [CompletableFuture] without body
 */
fun S3Operations.backupUnexpectedKeys(
    storageName: String,
    s3KeyManager: AbstractS3KeyDatabaseManager<*, *, *>,
): CompletableFuture<Unit> = backupUnexpectedKeys(
    storageName = storageName,
    commonPrefix = s3KeyManager.commonPrefix,
    s3KeyValidator = s3KeyManager.asS3KeyValidator(),
)

/**
 * Delete unexpected s3 key (according to [s3KeyValidator]) which are detected S3 storage (by common prefix [commonPrefix])
 *
 * @param storageName
 * @param commonPrefix
 * @param s3KeyValidator
 * @return [CompletableFuture] without body
 */
fun S3Operations.deleteUnexpectedKeys(
    storageName: String,
    commonPrefix: String,
    s3KeyValidator: (String) -> Boolean,
): CompletableFuture<Unit> = detectUnexpectedKeys(commonPrefix, s3KeyValidator)
    .thenComposeAsync { unexpectedKeys ->
        if (unexpectedKeys.isNotEmpty()) {
            doDeleteUnexpectedKeys(storageName, unexpectedKeys)
        } else {
            CompletableFuture.completedFuture(Unit)
        }
    }

/**
 * Delete unexpected s3 key (according to [AbstractS3KeyDatabaseManager]) which are detected S3 storage (by [AbstractS3KeyDatabaseManager])
 *
 * @param storageName
 * @param s3KeyManager
 * @return [CompletableFuture] without body
 */
fun S3Operations.deleteUnexpectedKeys(
    storageName: String,
    s3KeyManager: AbstractS3KeyDatabaseManager<*, *, *>,
): CompletableFuture<Unit> = deleteUnexpectedKeys(
    storageName = storageName,
    commonPrefix = s3KeyManager.commonPrefix,
    s3KeyValidator = s3KeyManager.asS3KeyValidator(),
)

private fun AbstractS3KeyDatabaseManager<*, *, *>.asS3KeyValidator(): (String) -> Boolean = { s3Key ->
    val id = s3Key.removePrefix(commonPrefix).toLong()
    findKeyByEntityId(id) == null
}

private fun S3Operations.doBackupUnexpectedKeys(
    storageName: String,
    commonPrefix: String,
    unexpectedKeys: Collection<String>,
): CompletableFuture<Unit> {
    val backupCommonPrefix = (commonPrefix.removeSuffix(PATH_DELIMITER) + "-backup-${Clock.System.now().epochSeconds}")
        .asS3CommonPrefix()
    log.warn {
        "Found unexpected keys in storage $storageName, move them to backup common prefix $backupCommonPrefix: $unexpectedKeys"
    }
    return unexpectedKeys
        .map { unexpectedKey ->
            moveObject(
                sourceS3Key = unexpectedKey,
                targetS3Key = backupCommonPrefix + unexpectedKey.removePrefix(commonPrefix)
            )
        }
        .let { moveResponses ->
            CompletableFuture.allOf(*moveResponses.toTypedArray())
                .thenApply {
                    log.debug {
                        "Finished backing up unexpected keys in storage $storageName"
                    }
                }
        }
}

private fun S3Operations.doDeleteUnexpectedKeys(
    storageName: String,
    unexpectedKeys: Collection<String>,
): CompletableFuture<Unit> {
    log.warn {
        "Found unexpected keys in storage $storageName, delete them: $unexpectedKeys"
    }
    return unexpectedKeys
        .map { unexpectedKey -> deleteObject(unexpectedKey) }
        .let { moveResponses ->
            CompletableFuture.allOf(*moveResponses.toTypedArray())
                .thenApply {
                    log.debug {
                        "Finished deleting of unexpected keys in storage $storageName"
                    }
                }
        }
}

private fun S3Operations.detectUnexpectedKeys(
    commonPrefix: String,
    s3KeyValidator: (String) -> Boolean,
): ListCompletableFuture<String> = listObjectsV2Fully(commonPrefix)
    .thenApply { responses ->
        responses
            .asSequence()
            .flatMap { it.contents() }
            .map { it.key() }
            .filter(s3KeyValidator)
            .toList()
    }

private fun S3Operations.listObjectsV2Fully(commonPrefix: String): ListCompletableFuture<ListObjectsV2Response> =
        listObjectsV2(commonPrefix)
            .thenComposeAsync { firstResponse ->
                doListObjectsV2(listOf(firstResponse))
            }

private fun S3Operations.doListObjectsV2(responses: List<ListObjectsV2Response>): ListCompletableFuture<ListObjectsV2Response> {
    val lastResponse = responses.last()
    return if (lastResponse.isTruncated) {
        listObjectsV2(lastResponse.prefix(), lastResponse.nextContinuationToken())
            .thenComposeAsync { newResponse ->
                doListObjectsV2(responses + newResponse)
            }
    } else {
        CompletableFuture.completedFuture(responses)
    }
}

private fun S3Operations.moveObject(sourceS3Key: String, targetS3Key: String): CompletableFuture<DeleteObjectResponse?> = copyObject(sourceS3Key, targetS3Key)
    .thenComposeAsync { copyResponse ->
        copyResponse?.let {
            deleteObject(sourceS3Key)
        } ?: CompletableFuture.completedFuture(null)
    }
