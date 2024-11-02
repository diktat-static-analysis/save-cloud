package com.saveourtool.common.storage

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.spring.entity.BaseEntity
import com.saveourtool.common.storage.key.AbstractS3KeyDatabaseManager
import reactor.core.publisher.Mono

/**
 * Implementation of S3 storage which stores keys in database
 *
 * @param s3Operations interface to operate with S3 storage
 * @property s3KeyManager [AbstractS3KeyDatabaseManager] manager for S3 keys using database
 */
open class ReactiveStorageWithDatabase<K : Any, E : BaseEntity, M : AbstractS3KeyDatabaseManager<K, E, *>>(
    private val s3Operations: S3Operations,
    override val s3KeyManager: M,
) : AbstractReactiveStorage<K>(s3Operations) {
    /**
     * Init method to back up unexpected ids which are detected in storage,but missed in database
     */
    override fun doInit(underlying: DefaultStorageProjectReactor<K>): Mono<Unit> = Mono.fromFuture {
        s3Operations.backupUnexpectedKeys(
            storageName = "${this::class.simpleName}",
            s3KeyManager = s3KeyManager,
        )
    }.publishOn(s3Operations.scheduler)
}
