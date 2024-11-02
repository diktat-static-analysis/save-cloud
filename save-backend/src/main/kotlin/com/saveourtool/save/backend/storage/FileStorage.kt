package com.saveourtool.save.backend.storage

import com.saveourtool.common.entities.*
import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.ReactiveStorageWithDatabase
import com.saveourtool.common.utils.*

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Storage for evaluated tools are loaded by users
 */
@Service
class FileStorage(
    s3Operations: S3Operations,
    s3KeyManager: FileS3KeyManager,
) : ReactiveStorageWithDatabase<FileDto, File, FileS3KeyManager>(
    s3Operations,
    s3KeyManager,
) {
    /**
     * @param project
     * @return all [FileDto]s which provided [Project] does contain
     */
    fun listByProject(
        project: Project,
    ): Flux<FileDto> = blockingToFlux {
        s3KeyManager.listByProject(project)
    }

    /**
     * @param fileId
     * @return [FileDto] for [File] with provided [fileId]
     */
    fun getFileById(
        fileId: Long,
    ): Mono<FileDto> = blockingToMono {
        s3KeyManager.findKeyByEntityId(fileId)
    }
        .switchIfEmptyToNotFound { "Not found a file by id $fileId" }
}
