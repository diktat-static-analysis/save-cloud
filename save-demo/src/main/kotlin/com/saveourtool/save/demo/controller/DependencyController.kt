package com.saveourtool.save.demo.controller

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.entities.FileDto
import com.saveourtool.common.utils.StringResponse
import com.saveourtool.common.utils.asyncEffect
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.utils.info
import com.saveourtool.save.demo.entity.Dependency
import com.saveourtool.save.demo.service.DemoService
import com.saveourtool.save.demo.service.DownloadToolService
import com.saveourtool.save.demo.storage.DependencyStorage

import org.slf4j.Logger
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer
import java.time.LocalDateTime

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * Internal controller that allows to upload files to save-demo
 */
@RestController
@RequestMapping("/internal/demo/files")
class DependencyController(
    private val demoService: DemoService,
    private val downloadToolService: DownloadToolService,
    private val dependencyStorage: DependencyStorage,
) {
    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version that the file should be marked with
     * @return [Flux] of [FileDto]s present in storage
     */
    @GetMapping("/{organizationName}/{projectName}/list-file")
    fun listFiles(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
    ): Flux<FileDto> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMapMany {
            dependencyStorage.list(it, version)
        }
        .map { dependency ->
            FileDto(
                ProjectCoordinates(dependency.demo.organizationName, dependency.demo.projectName),
                dependency.fileName,
                LocalDateTime.now().toKotlinLocalDateTime(),
            )
        }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version of a file to be deleted
     * @param fileName name of a file to be deleted
     * @return [Mono] of [Unit]
     */
    @DeleteMapping("/{organizationName}/{projectName}/delete-file")
    fun deleteFile(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
        @RequestParam fileName: String,
    ): Mono<Unit> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMap { dependencyStorage.delete(it, version, fileName) }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version to attach files to
     * @param fileDtos list of [FileDto] containing information required for file download
     * @return [Mono] of [StringResponse] with response
     */
    @PostMapping("/{organizationName}/{projectName}/upload")
    fun uploadFiles(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
        @RequestBody fileDtos: List<FileDto>,
    ): Mono<StringResponse> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .asyncEffect { demo ->
            dependencyStorage.cleanDependenciesNotIn(demo, version, fileDtos.map { fileDto -> fileDto.name })
        }
        .flatMapIterable { demo -> fileDtos.map { Dependency(demo, version, it.name, it.requiredId()) to it } }
        .filterWhen { (dependency, _) -> dependencyStorage.doesExist(dependency).map(Boolean::not) }
        .flatMap { (dependency, fileDto) -> downloadToolService.downloadToStorage(fileDto, dependency) }
        .collectList()
        .map { it.size }
        .map { size ->
            log.info { "Successfully downloaded $size files from file storage." }
            StringResponse.ok(
                if (size == 0) {
                    "All files are already present in demo storage."
                } else {
                    "Successfully saved $size files to demo storage."
                },
            )
        }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version to attach [zip] to
     * @return [Flux] of [ByteBuffer] - archive with files as content
     */
    @GetMapping(
        path = ["/{organizationName}/{projectName}/download-as-zip"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE],
    )
    fun downloadFiles(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
    ): Flux<ByteBuffer> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMapMany { dependencyStorage.archive(it.organizationName, it.projectName, version) }

    companion object {
        private val log: Logger = getLogger<DependencyController>()
    }
}
