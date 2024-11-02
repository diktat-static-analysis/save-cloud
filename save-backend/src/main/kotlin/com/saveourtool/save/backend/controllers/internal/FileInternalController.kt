package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.common.domain.TestResultDebugInfo
import com.saveourtool.common.utils.ByteBufferFluxResponse
import com.saveourtool.common.utils.switchIfEmptyToNotFound
import com.saveourtool.save.backend.storage.DebugInfoStorage
import com.saveourtool.save.backend.storage.FileStorage

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Internal controller for [FileStorage]
 *
 * @param fileStorage
 */
@RestController
@RequestMapping("/internal/files")
class FileInternalController(
    private val fileStorage: FileStorage,
    private val debugInfoStorage: DebugInfoStorage,
) {
    /**
     * @param fileId
     * @return [Mono] with file contents
     */
    @PostMapping(path = ["/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun internalDownload(
        @RequestParam fileId: Long,
    ): Mono<ByteBufferFluxResponse> = fileStorage.getFileById(fileId)
        .flatMap { fileDto ->
            fileStorage.doesExist(fileDto)
                .filter { it }
                .switchIfEmptyToNotFound {
                    "File with key $fileDto is not found"
                }
                .map {
                    log.info("Sending file ${fileDto.name} to an agent")
                    ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(fileStorage.download(fileDto))
                }
        }

    /**
     * @param executionId ID of execution that was executed for the test
     * @param testResultDebugInfo additional info that should be stored
     * @return [Mono] with count of uploaded bytes
     */
    @PostMapping(value = ["/debug-info"])
    fun uploadDebugInfo(
        @RequestParam executionId: Long,
        @RequestBody testResultDebugInfo: TestResultDebugInfo,
    ): Mono<Long> = debugInfoStorage.upload(executionId, testResultDebugInfo)

    companion object {
        private val log = LoggerFactory.getLogger(FileInternalController::class.java)
    }
}
