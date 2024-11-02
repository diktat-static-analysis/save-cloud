package com.saveourtool.common.storage

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.key.AbstractS3KeyDatabaseManager
import com.saveourtool.common.storage.key.S3KeyManager
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.utils.isNotNull
import com.saveourtool.common.utils.warn

import org.slf4j.Logger
import reactor.kotlin.core.publisher.toFlux
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response
import software.amazon.awssdk.services.s3.model.S3Exception

import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher

/**
 * S3 implementation of [StorageCoroutines]
 *
 * @param K type of key
 * @param s3Operations [S3Operations] to operate with S3
 * @param s3KeyManager [S3KeyManager] manager for S3 keys
 */
class DefaultStorageCoroutines<K : Any>(
    private val s3Operations: S3Operations,
    private val s3KeyManager: S3KeyManager<K>,
) : StorageCoroutines<K> {
    private val log: Logger = getLogger(this::class)

    override suspend fun list(): Flow<K> = flow {
        var lastResponse = doListObjectV2()
        emitKeys(lastResponse)
        while (lastResponse.isTruncated) {
            lastResponse = doListObjectV2(lastResponse.nextContinuationToken())
            emitKeys(lastResponse)
        }
    }

    private suspend fun doListObjectV2(continuationToken: String? = null): ListObjectsV2Response =
            s3Operations.listObjectsV2(s3KeyManager.commonPrefix, continuationToken).asDeferred().await()

    private suspend fun FlowCollector<K>.emitKeys(response: ListObjectsV2Response) {
        response.contents()
            .forEach { s3Object ->
                val s3Key = s3Object.key()
                val key = findKey(s3Key)
                key?.let { emit(it) } ?: run {
                    log.warn {
                        "Found s3 key $s3Key which is not valid by ${s3KeyManager::class.simpleName}"
                    }
                }
            }
    }

    override suspend fun doesExist(key: K): Boolean = findExistedS3Key(key)?.let { s3Key ->
        s3Operations.headObject(s3Key)
            .asDeferred()
            .await()
    }.isNotNull()

    override suspend fun contentLength(key: K): Long? = findExistedS3Key(key)?.let { s3Key ->
        s3Operations.headObject(s3Key)
            .asDeferred()
            .await()
            ?.contentLength()
    }

    override suspend fun lastModified(key: K): Instant? = findExistedS3Key(key)?.let { s3Key ->
        s3Operations.headObject(s3Key)
            .asDeferred()
            .await()
            ?.lastModified()
    }

    override suspend fun delete(key: K): Boolean = findExistedS3Key(key)?.let { s3Key ->
        s3Operations.deleteObject(s3Key)
            .asDeferred()
            .await()
            .isNotNull()
            .also {
                deleteKey(key)
            }
    } == true

    override suspend fun upload(key: K, content: Flow<ByteBuffer>): K {
        val s3Key = createNewS3Key(key)
        try {
            val uploadResponse = s3Operations.createMultipartUpload(s3Key)
                .asDeferred()
                .await()
            val partCounter = AtomicLong()
            val completedParts = content.map { buffer ->
                s3Operations.uploadPart(
                    uploadResponse,
                    partCounter.incrementAndGet(),
                    AsyncRequestBody.fromByteBuffer(buffer)
                ).asDeferred().await()
            }.toList()
            s3Operations.completeMultipartUpload(uploadResponse, completedParts)
                .asDeferred()
                .await()
            return requireNotNull(findKey(s3Key)) {
                "Cannot find updated key for uploaded key $key"
            }
        } catch (ex: S3Exception) {
            deleteKey(key)
            throw ex
        }
    }

    override suspend fun upload(key: K, contentLength: Long, content: Flow<ByteBuffer>): K {
        val s3Key = createNewS3Key(key)
        try {
            s3Operations.putObject(
                s3Key,
                contentLength,
                AsyncRequestBody.fromPublisher(content.asPublisher(s3Operations.coroutineDispatcher)),
            )
                .asDeferred()
                .await()
            return requireNotNull(findKey(s3Key)) {
                "Cannot find updated key for uploaded key $key"
            }
        } catch (ex: S3Exception) {
            deleteKey(key)
            throw ex
        }
    }

    override suspend fun download(key: K): Flow<ByteBuffer> = findExistedS3Key(key)
        ?.let { s3Key ->
            s3Operations.getObject(s3Key)
                .thenApply { getResponse ->
                    getResponse?.toFlux()?.asFlow() ?: emptyFlow()
                }
                .asDeferred()
                .await()
        } ?: emptyFlow()

    override suspend fun move(source: K, target: K): Boolean = findExistedS3Key(source)?.let { sourceS3Key ->
        val targetS3Key = createNewS3Key(target)
        s3Operations.copyObject(sourceS3Key, targetS3Key)
            .thenCompose { copyResponse ->
                copyResponse?.let {
                    s3Operations.deleteObject(sourceS3Key)
                        .thenApply { it.isNotNull() }
                } ?: CompletableFuture.completedFuture(false)
            }
            .asDeferred()
            .await()
    } ?: false

    private suspend fun deleteKey(key: K): Unit = s3KeyManager.callAsSuspend { delete(key) }

    private suspend fun findKey(s3Key: String): K? = s3KeyManager.callAsSuspend { findKey(s3Key) }

    private suspend fun findExistedS3Key(key: K): String? = s3KeyManager.callAsSuspend { findExistedS3Key(key) }

    private suspend fun createNewS3Key(key: K): String = s3KeyManager.callAsSuspend { createNewS3Key(key) }

    private suspend fun <R> S3KeyManager<K>.callAsSuspend(function: S3KeyManager<K>.() -> R): R =
            if (s3KeyManager is AbstractS3KeyDatabaseManager<*, *, *>) {
                s3KeyManager.blockingBridge.blockingToSuspend {
                    function(this@callAsSuspend)
                }
            } else {
                function(this)
            }
}
