package com.saveourtool.common.storage

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.key.S3KeyManager
import com.saveourtool.common.storage.request.DownloadRequest
import com.saveourtool.common.storage.request.UploadRequest

import java.nio.ByteBuffer
import java.time.Instant
import javax.annotation.PostConstruct

import kotlinx.coroutines.flow.Flow

/**
 * S3 implementation of Storage
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param K type of key
 */
abstract class AbstractSuspendingStorage<K : Any>(
    s3Operations: S3Operations,
) : SuspendingStorage<K> {
    private val initializer: StorageInitializer = StorageInitializer(this::class)

    /**
     * [S3KeyManager] manager for S3 keys
     */
    protected abstract val s3KeyManager: S3KeyManager<K>
    private val storageCoroutines by lazy { DefaultStorageCoroutines(s3Operations, s3KeyManager) }
    private val storagePreSignedUrl by lazy { DefaultStoragePreSignedUrl(s3Operations, s3KeyManager) }

    /**
     * Init method to call [initializer]
     */
    @PostConstruct
    fun init() {
        initializer.initSuspendedly {
            doInit(storageCoroutines)
        }
    }

    /**
     * @param underlying
     * @return result of suspend init method as [Unit], it's [null] by default
     */
    protected open suspend fun doInit(underlying: DefaultStorageCoroutines<K>): Unit? = null

    override suspend fun list(): Flow<K> = initializer.validateAndRunSuspend { storageCoroutines.list() }

    override suspend fun download(key: K): Flow<ByteBuffer> = initializer.validateAndRunSuspend { storageCoroutines.download(key) }

    override suspend fun upload(key: K, content: Flow<ByteBuffer>): K = initializer.validateAndRunSuspend { storageCoroutines.upload(key, content) }

    override suspend fun upload(key: K, contentLength: Long, content: Flow<ByteBuffer>): K = initializer.validateAndRunSuspend {
        storageCoroutines.upload(key, contentLength,
            content)
    }

    override suspend fun delete(key: K): Boolean = initializer.validateAndRunSuspend { storageCoroutines.delete(key) }

    override suspend fun lastModified(key: K): Instant? = initializer.validateAndRunSuspend { storageCoroutines.lastModified(key) }

    override suspend fun contentLength(key: K): Long? = initializer.validateAndRunSuspend { storageCoroutines.contentLength(key) }

    override suspend fun doesExist(key: K): Boolean = initializer.validateAndRunSuspend { storageCoroutines.doesExist(key) }

    override suspend fun move(source: K, target: K): Boolean = initializer.validateAndRunSuspend { storageCoroutines.move(source, target) }

    override fun generateRequestToDownload(key: K): DownloadRequest<K>? = initializer.validateAndRun { storagePreSignedUrl.generateRequestToDownload(key) }

    override fun generateRequestToUpload(key: K, contentLength: Long): UploadRequest<K> = initializer.validateAndRun {
        storagePreSignedUrl.generateRequestToUpload(key, contentLength)
    }
}
