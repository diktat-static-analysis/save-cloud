package com.saveourtool.save.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.entities.FileDto
import com.saveourtool.common.entities.Project
import com.saveourtool.common.permission.Permission
import com.saveourtool.common.service.ProjectService
import com.saveourtool.common.utils.*
import com.saveourtool.common.utils.ByteBufferFluxResponse
import com.saveourtool.common.v1
import com.saveourtool.save.backend.storage.FileStorage

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

typealias FileDtoResponse = ResponseEntity<FileDto>

/**
 * A Spring controller for [FileDto]
 */
@RestController
@RequestMapping("/api/$v1/files")
@ApiSwaggerSupport
@Tags(
    Tag(name = "files"),
)
class FileController(
    private val fileStorage: FileStorage,
    private val projectService: ProjectService,
) {
    @Operation(
        method = "GET",
        summary = "Get a list by project coordinates.",
        description = "Get a list by project coordinates.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "organization name of a file key", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "project name of a file key", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Returns the list of files.")
    @ApiResponse(responseCode = "404", description = "Not found project by provided values.")
    @GetMapping(path = ["/{organizationName}/{projectName}/list"])
    fun list(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Flux<FileDto> = getProjectAsMonoAndValidatePermission(
        organizationName = organizationName,
        projectName = projectName,
        authentication = authentication,
        permission = Permission.READ,
    )
        .flatMapMany { project ->
            fileStorage.listByProject(project)
        }

    @Operation(
        method = "DELETE",
        summary = "Delete a file by id.",
        description = "Delete a file by id.",
    )
    @Parameters(
        Parameter(name = "fileId", `in` = ParameterIn.QUERY, description = "ID of additional file", required = true),
    )
    @ApiResponse(responseCode = "200", description = "File deleted successfully.")
    @ApiResponse(responseCode = "404", description = "Not found file by provided values.")
    @DeleteMapping(path = ["/delete"])
    fun delete(
        @RequestParam fileId: Long,
        authentication: Authentication,
    ): Mono<StringResponse> = fileStorage.getFileById(fileId)
        .validatePermission(authentication, Permission.DELETE)
        .flatMap { fileStorage.delete(it) }
        .map { deleted ->
            if (deleted) {
                ResponseEntity.ok("File deleted successfully")
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found by id $fileId")
            }
        }

    @Operation(
        method = "GET",
        summary = "Download a file by id.",
        description = "Download a file by id.",
    )
    @Parameters(
        Parameter(name = "fileId", `in` = ParameterIn.QUERY, description = "ID of additional file", required = true),
    )
    @ApiResponse(responseCode = "200", description = "The file uploaded successfully.")
    @ApiResponse(responseCode = "404", description = "Not found file by provided values.")
    @GetMapping(
        path = ["/download"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE],
    )
    fun download(
        @RequestParam fileId: Long,
        authentication: Authentication,
    ): Mono<ByteBufferFluxResponse> = fileStorage.getFileById(fileId)
        .validatePermission(authentication, Permission.READ)
        .flatMap { fileDto ->
            fileStorage.doesExist(fileDto)
                .filter { it }
                .switchIfEmptyToNotFound {
                    "File with key $fileDto is not found"
                }
                .map {
                    log.info("Sending file ${fileDto.name} to a client")
                    ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(fileStorage.download(fileDto))
                }
        }

    @Operation(
        method = "POST",
        summary = "Upload a file by project coordinates, name and uploaded time in millis.",
        description = "Upload a file by project coordinates, name and uploaded time in millis.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "organization name of a file key", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "project name of a file key", required = true),
        Parameter(name = FILE_PART_NAME, `in` = ParameterIn.DEFAULT, description = "a file to upload", required = true),
        Parameter(name = CONTENT_LENGTH_CUSTOM, `in` = ParameterIn.HEADER, description = "size in bytes of a file", required = true),
    )
    @ApiResponse(responseCode = "200", description = "The file uploaded successfully.")
    @ApiResponse(responseCode = "404", description = "Not found project or file by provided values.")
    @PostMapping(path = ["/{organizationName}/{projectName}/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestPart(FILE_PART_NAME) filePartMono: Mono<FilePart>,
        @RequestHeader(CONTENT_LENGTH_CUSTOM) contentLength: Long,
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<FileDtoResponse> = getProjectAsMonoAndValidatePermission(
        organizationName = organizationName,
        projectName = projectName,
        authentication = authentication,
        permission = Permission.WRITE,
    )
        .flatMap { project ->
            filePartMono.flatMap { filePart ->
                val fileDto = FileDto(
                    projectCoordinates = project.toProjectCoordinates(),
                    name = filePart.filename(),
                    uploadedTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                    sizeBytes = contentLength,
                )
                fileStorage.doesExist(fileDto)
                    .filter { !it }
                    .switchIfEmptyToResponseException(HttpStatus.CONFLICT)
                    .flatMap {
                        fileStorage.upload(fileDto, fileDto.sizeBytes, filePart.content().map { it.asByteBuffer() })
                    }
                    .switchIfEmptyToResponseException(HttpStatus.INTERNAL_SERVER_ERROR)
                    .map {
                        ResponseEntity.ok(it)
                    }
            }
        }

    private fun getProjectAsMonoAndValidatePermission(
        organizationName: String,
        projectName: String,
        authentication: Authentication,
        permission: Permission,
    ): Mono<Project> = projectService.findWithPermissionByNameAndOrganization(
        authentication, projectName, organizationName, permission
    )

    private fun Mono<FileDto>.validatePermission(
        authentication: Authentication,
        permission: Permission,
    ): Mono<FileDto> = flatMap { fileDto ->
        getProjectAsMonoAndValidatePermission(
            organizationName = fileDto.projectCoordinates.organizationName,
            projectName = fileDto.projectCoordinates.projectName,
            authentication = authentication,
            permission = permission,
        ).map { fileDto }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FileController::class.java)
    }
}
