package com.saveourtool.save.backend.storage

import com.saveourtool.common.entities.TestSuitesSource
import com.saveourtool.common.entities.TestsSourceSnapshot
import com.saveourtool.common.request.TestFilesRequest
import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.ReactiveStorageWithDatabase
import com.saveourtool.common.test.TestFilesContent
import com.saveourtool.common.test.TestsSourceSnapshotDto
import com.saveourtool.common.utils.*
import com.saveourtool.save.backend.configs.ConfigProperties

import org.slf4j.Logger
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

import java.nio.file.Files

import kotlin.io.path.*

/**
 * Storage for snapshots of [com.saveourtool.save.entities.TestSuitesSource]
 */
@Component
class TestsSourceSnapshotStorage(
    s3Operations: S3Operations,
    s3KeyManager: TestsSourceSnapshotS3KeyManager,
    private val configProperties: ConfigProperties,
) : ReactiveStorageWithDatabase<TestsSourceSnapshotDto, TestsSourceSnapshot, TestsSourceSnapshotS3KeyManager>(
    s3Operations,
    s3KeyManager,
) {
    private fun createTempDirectoryForArchive() = Files.createTempDirectory(
        configProperties.workingDir,
        "source-"
    )

    /**
     * @param request
     * @return [TestFilesContent] filled with test files
     */
    fun getTestContent(request: TestFilesRequest): Mono<TestFilesContent> = blockingToMono {
        createTempDirectoryForArchive()
    }
        .flatMap { tmpSourceDir ->
            blockingToMono {
                createTempFile(tmpSourceDir, "archive-", ZIP_ARCHIVE_EXTENSION)
            }
                .flatMap { tmpArchive ->
                    download(request.testsSourceSnapshot)
                        .collectToFile(tmpArchive)
                        .blockingMap {
                            tmpArchive.extractZipHere()
                            tmpArchive.deleteExisting()
                        }
                }
                .map {
                    val testFilePath = request.test.filePath
                    val additionalTestFilePath = request.test.additionalFiles.firstOrNull()
                    TestFilesContent(
                        tmpSourceDir.resolve(testFilePath).readLines(),
                        additionalTestFilePath?.let { tmpSourceDir.resolve(it).readLines() },
                    )
                }
                .doOnTerminate {
                    tmpSourceDir.deleteRecursivelySafely(log)
                }
        }

    /**
     * @param testSuitesSource
     * @return true if all [TestsSourceSnapshot] (found by [testSuitesSource]) deleted successfully, otherwise -- false
     */
    fun deleteAll(testSuitesSource: TestSuitesSource): Mono<Boolean> = blockingToFlux { s3KeyManager.findAll(testSuitesSource) }
        .flatMap { delete(it) }
        .all { it }

    companion object {
        private val log: Logger = getLogger<TestsSourceSnapshotStorage>()
    }
}
