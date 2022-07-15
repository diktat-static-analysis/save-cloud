package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.service.GitService
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage
import com.saveourtool.save.backend.utils.switchToNotFoundIfEmpty
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info
import kotlinx.datetime.toKotlinLocalDateTime
import org.slf4j.Logger
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@RestController
@RequestMapping("/internal/test-suites-source")
class TestSuitesSourceController(
    private val testSuitesSourceService: TestSuitesSourceService,
    private val testSuitesSourceSnapshotStorage: TestSuitesSourceSnapshotStorage,
    private val organizationService: OrganizationService,
    private val gitService: GitService,
) {
    @GetMapping("/{organizationName}/{name}")
    fun findAsDtoByName(@PathVariable organizationName: String, @PathVariable name: String): Mono<TestSuitesSourceDto> =
        organizationService.getByNameAsMono(organizationName).flatMap { organization ->
            testSuitesSourceService.findByName(organization, name).toMono()
                .switchToNotFoundIfEmpty {
                    "TestSuitesSource not found by name $name for organization ${organization.name}"
                }
        }.map { it.toDto() }


    @PostMapping("/{organizationName}/{name}/{version}/upload-snapshot", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadSnapshot(
        @PathVariable organizationName: String,
        @PathVariable name: String,
        @PathVariable version: String,
        @RequestParam creationTime: Long,
        @RequestPart content: Flux<ByteBuffer>
    ): Mono<Unit> {
        return findAsDtoByName(organizationName, name)
            .map { TestSuitesSourceSnapshotKey(it, version, LocalDateTime.ofInstant(Instant.ofEpochMilli(creationTime), ZoneOffset.UTC).toKotlinLocalDateTime()) }
            .flatMap { key ->
                testSuitesSourceSnapshotStorage.upload(key, content).map { writtenBytes ->
                    log.info { "Saved ($writtenBytes bytes) snapshot of ${key.testSuitesSourceName} in ${key.organizationName} with version $version" }
                }
            }
    }


    @GetMapping("/{organizationName}/{name}/{version}/contains")
    fun containsSnapshot(
        @PathVariable organizationName: String,
        @PathVariable name: String,
        @PathVariable version: String,
    ): Mono<Boolean> = testSuitesSourceSnapshotStorage.doesContain(organizationName, name, version)

    @GetMapping("/{organizationName}/{name}/latest")
    fun getLatestVersion(
        @PathVariable organizationName: String,
        @PathVariable name: String,
    ): Mono<String> = testSuitesSourceSnapshotStorage.latestVersion(organizationName, name)

    @PostMapping("/{organizationName}/get-or-create")
    @Transactional
    fun getOrCreate(
        @PathVariable organizationName: String,
        @RequestParam gitUrl: String,
        @RequestParam testRootPath: String,
        @RequestParam branch: String,
    ): Mono<TestSuitesSource> = organizationService.getByNameAsMono(organizationName)
        .map { organization ->
            organization to gitService.getByOrganizationAndUrl(organization, gitUrl)
        }
        .map { (organization, git) ->
            testSuitesSourceService.getOrCreate(organization, git, testRootPath, branch)
        }

    companion object {
        private val log: Logger = getLogger<TestSuitesSourceService>()
    }
}