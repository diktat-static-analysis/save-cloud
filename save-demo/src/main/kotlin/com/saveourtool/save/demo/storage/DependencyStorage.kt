package com.saveourtool.save.demo.storage

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.ReactiveStorageWithDatabase
import com.saveourtool.common.utils.*
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.entity.Dependency

import org.slf4j.Logger
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer
import java.nio.file.Path

import kotlin.io.path.*

/**
 * Storage to keep all the tools on the disk
 */
@Component
class DependencyStorage(
    s3Operations: S3Operations,
    s3KeyManager: DependencyKeyManager,
) : ReactiveStorageWithDatabase<Dependency, Dependency, DependencyKeyManager>(
    s3Operations,
    s3KeyManager,
) {
    /**
     * @param demo
     * @param version version of a tool that the file is connected to
     * @return list of files present in storage for required version
     */
    fun blockingList(
        demo: Demo,
        version: String,
    ): List<Dependency> = s3KeyManager.findAllDependencies(
        demo.organizationName,
        demo.projectName,
        version,
    )

    /**
     * @param demo
     * @param version version of a tool that the file is connected to
     * @return list of files present in storage for required version
     */
    fun list(
        demo: Demo,
        version: String,
    ): Flux<Dependency> = blockingToFlux { blockingList(demo, version) }

    /**
     * @param demo
     * @param version version of a tool that the file is connected to
     * @param fileName name of a file to be deleted
     * @return true if file is successfully deleted, false otherwise
     */
    fun delete(
        demo: Demo,
        version: String,
        fileName: String,
    ): Mono<Unit> = blockingToMono {
        s3KeyManager.findDependency(
            demo.organizationName,
            demo.projectName,
            version,
            fileName,
        )
    }
        .flatMap { delete(it) }
        .map {
            log.debug {
                "Deleted $fileName associated with version $version from $demo"
            }
        }

    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @param fileName
     * @return true if storage contains some dependency with provided values, otherwise -- false
     */
    fun doesExist(
        organizationName: String,
        projectName: String,
        version: String,
        fileName: String,
    ): Mono<Boolean> = findDependency(organizationName, projectName, version, fileName)
        .map { true }
        .defaultIfEmpty(false)

    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @param fileName
     * @return [Dependency] found by provided values
     */
    fun findDependency(
        organizationName: String,
        projectName: String,
        version: String,
        fileName: String,
    ): Mono<Dependency> = blockingToMono {
        s3KeyManager.findDependency(
            organizationName,
            projectName,
            version,
            fileName,
        )
    }

    /**
     * @param demo Demo entity
     * @param version version of a tool
     * @param dependencyNames list of dependency names that should be present in storage
     * @return number of deleted files, wrapped into [Mono]
     */
    fun cleanDependenciesNotIn(
        demo: Demo,
        version: String,
        dependencyNames: List<String>,
    ): Mono<Int> = blockingToMono {
        s3KeyManager.findAllDependencies(
            demo.organizationName,
            demo.projectName,
            version,
        )
    }
        .flatMapIterable { it }
        .filter { it.fileName !in dependencyNames }
        .flatMap { delete(it) }
        .collectList()
        .map { it.size }
        .doOnSuccess { log.debug { "Cleaned $it useless dependencies." } }

    private fun downloadToTempDir(
        tempDir: Path,
        organizationName: String,
        projectName: String,
        version: String
    ) = blockingToFlux {
        s3KeyManager.findAllDependencies(organizationName, projectName, version)
    }
        .flatMap { download(it).collectToFile(tempDir / it.fileName) }
        .collectList()

    /**
     * Get all the files of requested [version] of [organizationName]/[projectName] demo as zip archive
     *
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version of the demo
     * @param archiveName name that an archive should have
     * @return [Flux] with zip as content
     */
    @OptIn(ExperimentalPathApi::class)
    fun archive(
        organizationName: String,
        projectName: String,
        version: String,
        archiveName: String = "archive.zip",
    ): Flux<ByteBuffer> = createTempDirectory().div("archive").createDirectory().let { tempDir ->
        createArchive(tempDir, organizationName, projectName, version, archiveName)
            .doOnComplete { tempDir.deleteRecursively() }
            .doOnError { tempDir.deleteRecursively() }
    }

    private fun createArchive(
        tmpDir: Path,
        organizationName: String,
        projectName: String,
        version: String,
        archiveName: String = "archive.zip",
    ): Flux<ByteBuffer> = downloadToTempDir(tmpDir, organizationName, projectName, version).map {
        tmpDir.parent.div(archiveName).also { dirToZip -> tmpDir.compressAsZipTo(dirToZip) }
    }
        .flatMapMany { it.toByteBufferFlux() }

    companion object {
        private val log: Logger = getLogger<DependencyStorage>()
    }
}
