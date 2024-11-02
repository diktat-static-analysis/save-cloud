package com.saveourtool.common.storage

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.spring.entity.BaseEntity
import com.saveourtool.common.storage.key.AbstractS3KeyDatabaseManager
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.withContext

/**
 * Implementation of S3 storage which stores keys in database
 *
 * @param s3Operations interface to operate with S3 storage
 * @property s3KeyManager [AbstractS3KeyDatabaseManager] manager for S3 keys using database
 */
open class SuspendingStorageWithDatabase<K : Any, E : BaseEntity, M : AbstractS3KeyDatabaseManager<K, E, *>>(
    private val s3Operations: S3Operations,
    override val s3KeyManager: M,
) : AbstractSuspendingStorage<K>(s3Operations) {
    /**
     * Init method to back up unexpected ids which are detected in storage,but missed in database
     */
    override suspend fun doInit(underlying: DefaultStorageCoroutines<K>) {
        withContext(s3Operations.coroutineDispatcher) {
            s3Operations.backupUnexpectedKeys(
                storageName = "${this::class.simpleName}",
                s3KeyManager = s3KeyManager,
            )
                .asDeferred()
                .await()
        }
    }
}
