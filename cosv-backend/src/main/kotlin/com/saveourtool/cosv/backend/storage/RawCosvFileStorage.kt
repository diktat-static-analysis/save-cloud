package com.saveourtool.cosv.backend.storage

import com.saveourtool.common.entities.Organization
import com.saveourtool.common.entities.User
import com.saveourtool.common.entities.cosv.RawCosvFileDto
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isDuplicate
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isHasErrors
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isPendingRemoved
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isProcessing
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isUploadedJsonFile
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isZipArchive
import com.saveourtool.common.entities.cosv.RawCosvFileStatisticsDto
import com.saveourtool.common.entities.cosv.RawCosvFileStatus
import com.saveourtool.common.entitiescosv.RawCosvFile
import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.DefaultStorageProjectReactor
import com.saveourtool.common.storage.ReactiveStorageWithDatabase
import com.saveourtool.common.storage.deleteUnexpectedKeys
import com.saveourtool.common.utils.*

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer

typealias OrganizationIdAndOwnerId = Pair<Long, Long>
typealias RawCosvFileDtoCollection = Collection<RawCosvFileDto>

/**
 * S3 storage for [RawCosvFile]
 */
@Component
class RawCosvFileStorage(
    private val s3Operations: S3Operations,
    s3KeyManager: RawCosvFileS3KeyManager,
) : ReactiveStorageWithDatabase<RawCosvFileDto, RawCosvFile, RawCosvFileS3KeyManager>(
    s3Operations,
    s3KeyManager,
) {
    /**
     * Init method to remove deleted (unexpected) ids which are detected in storage, but missed in database
     */
    override fun doInit(underlying: DefaultStorageProjectReactor<RawCosvFileDto>): Mono<Unit> = Mono.fromFuture {
        s3Operations.deleteUnexpectedKeys(
            storageName = "${this::class.simpleName}",
            s3KeyManager = s3KeyManager,
        )
    }
        .flatMap {
            underlying.list()
                .filter { it.status == RawCosvFileStatus.PROCESSED }
                .collectList()
                .flatMap { rawCosvFiles ->
                    underlying.deleteAll(rawCosvFiles)
                }
        }
        .flatMap {
            underlying.list()
                .filter { it.contentLength == null }
                .flatMap { key ->
                    underlying.contentLength(key).map { key to it }
                }
                .blockingMap { (key, contentLength) ->
                    s3KeyManager.updateKeyByContentLength(key, contentLength)
                }
                .thenJust(Unit)
        }
        .publishOn(s3Operations.scheduler)

    /**
     * @param organizationId
     * @param userId
     * @return statistics [RawCosvFileStatisticDto] for all [RawCosvFileDto]s which belongs to [organizationId] and uploaded by [userId]
     */
    fun statisticsByOrganizationAndUser(
        organizationId: Long,
        userId: Long,
    ): Mono<RawCosvFileStatisticsDto> = blockingToMono {
        val filesList = s3KeyManager.listByOrganizationAndUser(organizationId, userId).toList()
        RawCosvFileStatisticsDto(
            filesList.count(),
            filesList.count { it.isZipArchive() },
            filesList.count { it.isUploadedJsonFile() },
            filesList.count { it.isProcessing() },
            filesList.count { it.isPendingRemoved() },
            filesList.count { it.isDuplicate() },
            filesList.count { it.isHasErrors() }
        )
    }

    /**
     * @param organizationId
     * @param userId
     * @param pageRequest
     * @return all [RawCosvFileDto]s which belongs to [organizationId] and uploaded by [userId]
     */
    fun listByOrganizationAndUser(
        organizationId: Long,
        userId: Long,
        pageRequest: PageRequest? = null,
    ): Mono<RawCosvFileDtoCollection> = blockingToMono {
        s3KeyManager.listByOrganizationAndUser(organizationId, userId, pageRequest).toList()
    }

    /**
     * @param ids
     * @param newStatus
     * @return empty [Mono]
     */
    fun updateAll(
        ids: Collection<Long>,
        newStatus: RawCosvFileStatus,
    ): Mono<Unit> = blockingToMono { s3KeyManager.updateAll(ids, newStatus) }

    /**
     * @param id
     * @param newStatus
     * @param statusMessage
     * @return empty [Mono]
     */
    fun update(
        id: Long,
        newStatus: RawCosvFileStatus,
        statusMessage: String,
    ): Mono<Unit> = blockingToMono { s3KeyManager.update(id, newStatus, statusMessage) }

    /**
     * @param id
     * @return [Organization] to which is uploaded and [User] who uploaded
     */
    fun getOrganizationIdAndOwnerId(
        id: Long,
    ): Mono<OrganizationIdAndOwnerId> = blockingToMono { s3KeyManager.getOrganizationAndOwner(id) }

    /**
     * @param id
     * @return [RawCosvFileDto]
     */
    fun findById(
        id: Long,
    ): Mono<RawCosvFileDto> = blockingToMono { s3KeyManager.findKeyByEntityId(id) }
        .switchIfEmptyToNotFound { "Not found raw COSV file id=$id" }

    /**
     * @param id
     * @return content of raw COSV file
     */
    fun downloadById(
        id: Long,
    ): Flux<ByteBuffer> = findById(id)
        .flatMapMany { download(it) }

    /**
     * @param id
     * @return result of deletion
     */
    fun deleteById(
        id: Long,
    ): Mono<Boolean> = findById(id)
        .flatMap { delete(it) }
}
