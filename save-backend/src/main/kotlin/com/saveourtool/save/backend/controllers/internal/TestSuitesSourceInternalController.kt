package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.MigrationTestsSourceSnapshotStorage
import com.saveourtool.save.backend.storage.TestsSourceSnapshotStorage
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.test.TestsSourceSnapshotInfo
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.testsuite.*
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import org.springframework.context.annotation.Lazy
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Controller for [TestSuitesSource]
 */
@RestController
@RequestMapping("/internal/test-suites-sources")
class TestSuitesSourceInternalController(
    private val testsSourceVersionService: TestsSourceVersionService,
    @Lazy
    private val migrationStorage: MigrationTestsSourceSnapshotStorage,
    private val snapshotStorage: TestsSourceSnapshotStorage,
    private val executionService: ExecutionService,
    private val lnkExecutionTestSuiteService: LnkExecutionTestSuiteService,
) {
    /**
     * @param snapshotInfo
     * @param contentAsMonoPart
     * @return [Mono] without value
     */
    @PostMapping("/upload-snapshot", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadSnapshot(
        @RequestPart("snapshotInfo") snapshotInfo: TestsSourceSnapshotInfo,
        @RequestPart("content") contentAsMonoPart: Mono<Part>,
    ): Mono<Unit> {
        require(migrationStorage.isMigrated())
        return contentAsMonoPart.flatMap { part ->
            val content = part.content().map { it.asByteBuffer() }
            snapshotStorage.upload(snapshotInfo, content).thenReturn(Unit)
        }
    }

    /**
     * @param versionInfo
     * @return [Mono] without value
     */
    @PostMapping("/save-version")
    fun saveVersion(
        @RequestBody versionInfo: TestsSourceVersionInfo,
    ): Mono<Unit> = blockingToMono {
        testsSourceVersionService.save(versionInfo)
    }

    /**
     * @param snapshotInfo
     * @return [Mono] with result
     */
    @PostMapping("/contains-snapshot")
    fun containsSnapshot(
        @RequestBody snapshotInfo: TestsSourceSnapshotInfo,
    ): Mono<Boolean> = snapshotStorage.doesContain(snapshotInfo)

    /**
     * @param executionId
     * @return content of tests related to provided values
     */
    @PostMapping("/download-snapshot-by-execution-id", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadByExecutionId(
        @RequestParam executionId: Long
    ): Mono<ByteBufferFluxResponse> = blockingToMono {
        val execution = executionService.findExecution(executionId)
            .orNotFound { "Execution (id=$executionId) not found" }
        val testSuite = lnkExecutionTestSuiteService.getAllTestSuitesByExecution(execution).firstOrNull().orNotFound {
            "Execution (id=$executionId) doesn't have any testSuites"
        }
        testSuite
            .toDto()
            .let { it.source to it.version }
    }.flatMap { (source, version) ->
        source.downloadSnapshot(version)
    }

    private fun TestSuitesSourceDto.downloadSnapshot(
        version: String
    ): Mono<ByteBufferFluxResponse> = testsSourceVersionService.doesContain(organizationName, name, version)
        .filter { it }
        .switchIfEmptyToNotFound {
            "Not found a snapshot of $name in $organizationName with version=$version"
        }
        .map {
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(testsSourceVersionService.download(organizationName, name, version))
        }
}
