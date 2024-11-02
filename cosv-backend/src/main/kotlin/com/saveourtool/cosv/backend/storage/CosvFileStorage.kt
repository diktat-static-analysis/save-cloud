package com.saveourtool.cosv.backend.storage

import com.saveourtool.common.entitiescosv.CosvFile
import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.DefaultStorageProjectReactor
import com.saveourtool.common.storage.ReactiveStorageWithDatabase
import com.saveourtool.common.storage.deleteUnexpectedKeys
import com.saveourtool.common.utils.blockingToFlux
import com.saveourtool.common.utils.blockingToMono
import com.saveourtool.common.utils.switchIfEmptyToNotFound
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

/**
 * Storage for COSV files.
 *
 * For now, we store COSV files in S3 with `id` as a key.
 * We can migrate to NoSql database in the future.
 */
@Service
class CosvFileStorage(
    private val s3Operations: S3Operations,
    s3KeyManager: CosvFileS3KeyManager,
) : ReactiveStorageWithDatabase<CosvFile, CosvFile, CosvFileS3KeyManager>(
    s3Operations,
    s3KeyManager,
) {
    /**
     * Init method to remove deleted (unexpected) ids which are detected in storage, but missed in database
     */
    override fun doInit(underlying: DefaultStorageProjectReactor<CosvFile>): Mono<Unit> = Mono.fromFuture {
        s3Operations.deleteUnexpectedKeys(
            storageName = "${this::class.simpleName}",
            s3KeyManager = s3KeyManager,
        )
    }.publishOn(s3Operations.scheduler)

    /**
     * @param identifier
     * @return [Flux] with all [CosvFile] found by [identifier]
     */
    fun listByIdentifier(identifier: String): Flux<CosvFile> = blockingToFlux {
        s3KeyManager.findAllByIdentifier(identifier)
    }

    /**
     * @param keyId
     * @return [Flux] with all [ByteBuffer]
     */
    fun downloadByKeyId(keyId: Long): Flux<ByteBuffer> = blockingToMono { s3KeyManager.findKeyByEntityId(keyId) }
        .switchIfEmptyToNotFound {
            "Not found CosvFile by id $keyId"
        }
        .flatMapMany { download(it) }
}
