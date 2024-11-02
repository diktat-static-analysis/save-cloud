package com.saveourtool.cosv.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.common.entities.cosv.*
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isDuplicate
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isUploadedJsonFile
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.service.UserService
import com.saveourtool.common.storage.concatS3Key
import com.saveourtool.common.utils.*
import com.saveourtool.common.v1
import com.saveourtool.cosv.backend.configs.ConfigProperties
import com.saveourtool.cosv.backend.service.CosvService
import com.saveourtool.cosv.backend.storage.RawCosvFileStorage
import com.saveourtool.save.authservice.utils.userId

import org.reactivestreams.Publisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toFlux

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

import kotlin.io.path.*

typealias RawCosvFileDtoFlux = Flux<RawCosvFileDto>
typealias RawCosvFileStreamingResponseFlux = Flux<RawCosvFileStreamingResponse>
typealias PathAndSize = Pair<Path, Long>
typealias PathAndSizeAndAccumitiveSize = Triple<Path, Long, Long>

/**
 * Rest controller for raw COSV files
 */
@ApiSwaggerSupport
@RestController
@RequestMapping("/api/$v1/raw-cosv/{organizationName}")
class RawCosvFileController(
    private val cosvService: CosvService,
    private val rawCosvFileStorage: RawCosvFileStorage,
    private val userService: UserService,
    private val organizationService: OrganizationService,
    private val configProperties: ConfigProperties,
) {
    private fun createTempDirectoryForArchive() = Files.createTempDirectory(
        configProperties.workingDir.createDirectories(),
        "archive-"
    )

    /**
     * @param organizationName
     * @param filePartMono
     * @param authentication
     * @param contentLength
     * @return list of uploaded [RawCosvFileDto]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @PathVariable organizationName: String,
        @RequestPart(FILE_PART_NAME) filePartMono: Mono<FilePart>,
        @RequestHeader(CONTENT_LENGTH_CUSTOM) contentLength: Long,
        authentication: Authentication,
    ): Flux<RawCosvFileDto> = hasPermission(authentication, organizationName, Permission.WRITE, "upload")
        .flatMapMany {
            filePartMono
                .flatMapMany { filePart ->
                    doUpload(filePart, organizationName, authentication.name, contentLength)
                }
        }

    /**
     * @param organizationName
     * @param filePartFlux
     * @param authentication
     * @return list of uploaded [RawCosvFileDto]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping(
        "/batch-upload",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_NDJSON_VALUE],
    )
    fun batchUpload(
        @PathVariable organizationName: String,
        @RequestPart(FILE_PART_NAME) filePartFlux: Flux<FilePart>,
        authentication: Authentication,
    ): ResponseEntity<RawCosvFileDtoFlux> = hasPermission(authentication, organizationName, Permission.WRITE, "upload")
        .flatMapMany {
            filePartFlux
                .flatMap { filePart ->
                    doUpload(filePart, organizationName, authentication.name, contentLength = null)
                }
        }
        .let {
            ResponseEntity
                .ok()
                .cacheControlForNdjson()
                .body(it)
        }

    private fun doUpload(
        filePart: FilePart,
        organizationName: String,
        userName: String,
        contentLength: Long?,
    ): Publisher<RawCosvFileDto> {
        log.debug {
            "Processing ${filePart.filename()}"
        }
        val key = RawCosvFileDto(
            filePart.filename(),
            organizationName = organizationName,
            userName = userName,
            contentLength = contentLength,
        )
        val content = filePart.content().map { it.asByteBuffer() }
        return rawCosvFileStorage.uploadAndWrapDuplicateKeyException(key, content)
    }

    /**
     * @param organizationName
     * @param id
     * @param authentication
     * @return list of uploaded [RawCosvFileDto]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping(
        "/unzip/{id}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_NDJSON_VALUE],
    )
    fun unzip(
        @PathVariable organizationName: String,
        @PathVariable id: Long,
        authentication: Authentication,
    ): ResponseEntity<RawCosvFileStreamingResponseFlux> = hasPermission(authentication, organizationName, Permission.WRITE, "upload")
        .flatMap { rawCosvFileStorage.findById(id) }
        .flatMapMany { rawCosvFile ->
            Flux.concat(
                Mono.just(RawCosvFileStreamingResponse(PROGRESS_FOR_ARCHIVE, "Unzipping")),
                doUnzip(
                    rawCosvFile,
                    organizationName,
                    authentication.name
                )
            )
        }
        .let {
            ResponseEntity.ok()
                .cacheControlForNdjson()
                .body(it)
        }

    private fun doUnzip(
        archiveFile: RawCosvFileDto,
        organizationName: String,
        userName: String,
    ): RawCosvFileStreamingResponseFlux = blockingToMono {
        val tmpDir = createTempDirectoryForArchive()
        val tmpArchiveFile = tmpDir / archiveFile.fileName
        val contentDir = Files.createTempDirectory(tmpDir, "content-")
        Triple(tmpDir, tmpArchiveFile, contentDir)
    }
        .flatMapMany { (tmpDir, tmpArchiveFile, contentDir) ->
            log.debug {
                "Saving archive ${archiveFile.fileName} to ${tmpArchiveFile.absolutePathString()}"
            }
            rawCosvFileStorage.download(archiveFile)
                .collectToFile(tmpArchiveFile)
                .blockingMap {
                    tmpArchiveFile.extractZipTo(contentDir)
                    val entries = contentDir.listDirectoryEntries()
                    entries.map { it to it.fileSize() }
                }
                .flatMapMany { entryWithSizeList ->
                    Flux.concat(
                        doUploadArchiveEntries(
                            contentDir,
                            entryWithSizeList,
                            archiveFile.fileName,
                            organizationName,
                            userName
                        ),
                        blockingToMono {
                            tmpDir.deleteRecursivelySafely(log)
                        }
                            .then(rawCosvFileStorage.delete(archiveFile))
                            .thenReturn(RawCosvFileStreamingResponse(RawCosvFileStreamingResponse.FINAL_PROGRESS,
                                "Unzipped ${entryWithSizeList.sumOf { it.second }.toKilobytes()} Kb")),
                    )
                }
                .onErrorResume { error ->
                    log.error(error) {
                        "Failed to process archive ${archiveFile.fileName}"
                    }
                    blockingToMono { tmpDir.deleteRecursivelySafely(log) }.then(Mono.error(error))
                }
        }

    private fun doUploadArchiveEntries(
        contentDir: Path,
        entriesWithSize: List<PathAndSize>,
        entriesPrefix: String,
        organizationName: String,
        userName: String,
    ): RawCosvFileStreamingResponseFlux {
        val fullSize = entriesWithSize.sumOf { it.second }
        return entriesWithSize.runningFold(null as Pair<PathAndSize, Long>?) { current, next ->
            current?.let {
                next to next.second + it.second
            } ?: (next to next.second)
        }
            .filterNotNull()
            .toFlux()
            .flatMap { (fileAndSize, sizeSumOfPrevious) ->
                val (file, size) = fileAndSize
                log.debug {
                    "Processing ${file.absolutePathString()}"
                }
                rawCosvFileStorage.uploadAndWrapDuplicateKeyException(
                    key = RawCosvFileDto(
                        concatS3Key(entriesPrefix, file.relativeTo(contentDir).toString()),
                        organizationName = organizationName,
                        userName = userName,
                        contentLength = size,
                    ),
                    content = file.toByteBufferFlux(),
                )
                    .map {
                        RawCosvFileStreamingResponse(
                            progress = ((sizeSumOfPrevious.toDouble() / fullSize) * (100 - PROGRESS_FOR_ARCHIVE)).toInt() + PROGRESS_FOR_ARCHIVE,
                            progressMessage = "${sizeSumOfPrevious.toKilobytes()} / ${fullSize.toKilobytes()} KB",
                            result = listOf(it),
                        )
                    }
            }
    }

    /**
     * @param organizationName
     * @param authentication
     * @return [StringResponse]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/submit-all-uploaded-to-process")
    fun submitAllUploadedToProcess(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Mono<StringResponse> {
        val organization = organizationService.getOrganizationByName(organizationName)
        return rawCosvFileStorage.listByOrganizationAndUser(organization.requiredId(), authentication.userId())
            .map { files ->
                files
                    .filter { it.isUploadedJsonFile() }
                    .map { it.requiredId() }
            }
            .flatMap { ids ->
                doSubmitToProcess(organizationName, ids, authentication)
            }
    }

    private fun doSubmitToProcess(
        organizationName: String,
        ids: List<Long>,
        authentication: Authentication,
    ): Mono<StringResponse> = hasPermission(authentication, organizationName, Permission.WRITE, "submit to process")
        .flatMap {
            rawCosvFileStorage.updateAll(ids, RawCosvFileStatus.IN_PROGRESS)
        }
        .thenReturn(ResponseEntity.ok("Submitted $ids to be processed"))
        .doOnSuccess {
            blockingToMono {
                organizationService.getOrganizationByName(organizationName) to userService.getUserByName(authentication.name)
            }
                .flatMap { (organization, user) ->
                    cosvService.processAndAddTagsAndUpdateRating(ids, user, organization)
                }
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe()
        }

    /**
     * @param organizationName
     * @param authentication
     * @return statistics [RawCosvFileStatisticsDto] with counts of all, uploaded, processing, failed raw cosv files in [organizationName]
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping("/statistics")
    fun statistics(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Mono<RawCosvFileStatisticsDto> = hasPermission(authentication, organizationName, Permission.READ, "read")
        .flatMap {
            val organization = organizationService.getOrganizationByName(organizationName)
            rawCosvFileStorage.statisticsByOrganizationAndUser(organization.requiredId(), authentication.userId())
        }

    /**
     * @param organizationName
     * @param authentication
     * @param page
     * @param size
     * @return list of uploaded raw cosv files in [organizationName]
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping(
        "/list",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_NDJSON_VALUE],
    )
    fun list(
        @PathVariable organizationName: String,
        @RequestParam page: Int,
        @RequestParam size: Int,
        authentication: Authentication,
    ): ResponseEntity<RawCosvFileDtoFlux> = hasPermission(authentication, organizationName, Permission.READ, "read")
        .flatMap {
            val organization = organizationService.getOrganizationByName(organizationName)
            rawCosvFileStorage.listByOrganizationAndUser(organization.requiredId(), authentication.userId(), PageRequest.of(page, size, Sort.by("isZip").descending().and(Sort.by(
                "id"
            ))))
        }
        .flatMapIterable { it }
        .let {
            ResponseEntity.ok()
                .cacheControlForNdjson()
                .body(it)
        }

    /**
     * @param organizationName
     * @param id ID of raw COSV file
     * @param authentication
     * @return content of Raw COSV file
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping("/download/{id}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun download(
        @PathVariable organizationName: String,
        @PathVariable id: Long,
        authentication: Authentication,
    ): Mono<ByteBufferFluxResponse> = hasPermission(authentication, organizationName, Permission.READ, "download")
        .flatMap {
            rawCosvFileStorage.findById(id)
        }
        .map {
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${it.fileName}\"")
                .body(rawCosvFileStorage.download(it))
        }

    /**
     * @param organizationName
     * @param id
     * @param authentication
     * @return string response
     */
    @RequiresAuthorizationSourceHeader
    @DeleteMapping("/delete/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun delete(
        @PathVariable organizationName: String,
        @PathVariable id: Long,
        authentication: Authentication,
    ): Mono<StringResponse> = hasPermission(authentication, organizationName, Permission.DELETE, "delete")
        .flatMap {
            rawCosvFileStorage.deleteById(id)
                .filter { it }
                .switchIfEmptyToNotFound {
                    "Raw COSV file not found by id $id"
                }
                .map {
                    ResponseEntity.ok("Raw COSV file deleted successfully")
                }
        }

    /**
     * @param organizationName
     * @param authentication
     * @return [StringResponse]
     */
    @RequiresAuthorizationSourceHeader
    @DeleteMapping("/delete-all-duplicated-files")
    fun deleteAllDuplicatedFiles(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Mono<StringResponse> = hasPermission(authentication, organizationName, Permission.DELETE, "delete")
        .flatMap {
            val organization = organizationService.getOrganizationByName(organizationName)
            rawCosvFileStorage.listByOrganizationAndUser(organization.requiredId(), authentication.userId())
                .map { files ->
                    files.filter { it.isDuplicate() }
                }
                .flatMap { duplicateFiles ->
                    rawCosvFileStorage.deleteAll(duplicateFiles)
                        .filter { it }
                        .switchIfEmptyToNotFound {
                            "Duplicated COSV files can not be deleted because some of them were not found"
                        }
                        .map {
                            ResponseEntity.ok("Duplicated COSV files deleted successfully")
                        }
                }
        }

    private fun hasPermission(
        authentication: Authentication,
        organizationName: String,
        permission: Permission,
        operation: String,
    ): Mono<Unit> = blockingToMono {
        organizationService.hasPermissionInOrganization(authentication, organizationName, permission)
    }
        .filter { it }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "${authentication.name} has no permission to $operation raw COSV files from $organizationName"
        }
        .let { validated ->
            validated.takeIf { permission == Permission.WRITE }
                ?.validateCanDoBulkUpload(authentication, organizationName)
                ?: validated
        }
        .thenReturn(Unit)

    private fun Mono<*>.validateCanDoBulkUpload(
        authentication: Authentication,
        organizationName: String,
    ) = blockingMap {
        organizationService.getUserPermissionsByOrganizationName(authentication, organizationName)
    }
        .filter { it.inOrganizations[organizationName]?.canDoBulkUpload == true }
        .orResponseStatusException(HttpStatus.FORBIDDEN) {
            "You do not have permission to upload COSV files on behalf of this organization: $organizationName"
        }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<CosvController>()

        // to show progress bar
        private const val PROGRESS_FOR_ARCHIVE = 5

        private fun RawCosvFileStorage.uploadAndWrapDuplicateKeyException(
            key: RawCosvFileDto,
            content: Flux<ByteBuffer>,
        ): Mono<RawCosvFileDto> {
            val result = key.contentLength?.let {
                upload(key, it, content)
            } ?: upload(key, content)
            return result.onErrorResume { error ->
                when (error) {
                    is DataIntegrityViolationException -> Mono.error(ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate file name ${key.fileName}", error))
                    else -> Mono.error(error)
                }
            }
        }
    }
}
