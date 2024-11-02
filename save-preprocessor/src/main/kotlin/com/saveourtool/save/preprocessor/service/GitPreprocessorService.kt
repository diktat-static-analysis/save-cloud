package com.saveourtool.save.preprocessor.service

import com.saveourtool.common.entities.GitDto
import com.saveourtool.common.utils.*
import com.saveourtool.save.preprocessor.common.CloneResult
import com.saveourtool.save.preprocessor.common.GitRepositoryProcessor
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.utils.GitCommitInfo
import com.saveourtool.save.preprocessor.utils.cloneBranchToDirectory
import com.saveourtool.save.preprocessor.utils.cloneCommitToDirectory
import com.saveourtool.save.preprocessor.utils.cloneTagToDirectory

import org.eclipse.jgit.util.FileUtils
import org.jetbrains.annotations.NonBlocking
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories

typealias ArchiveProcessor<T> = (Path) -> Mono<T>

/**
 * Additional service for Git based [com.saveourtool.save.entities.TestSuitesSource]s
 */
@Service
class GitPreprocessorService(
    configProperties: ConfigProperties,
) {
    private val workingDir = Paths.get(configProperties.repository, "tmp")
        .also { it.createDirectories() }

    private fun createTempDirectoryForRepository() = Files.createTempDirectory(
        workingDir,
        "repository-"
    )

    private fun createTempTarFile() = Files.createTempFile(
        workingDir,
        "archive-",
        ZIP_ARCHIVE_EXTENSION
    )

    /**
     * @param gitDto the Git URL with optional credentials.
     * @param tagName the Git tag.
     * @param repositoryProcessor folder processing should be finished here
     *   &mdash; the folder will be removed afterwards.
     * @return the result returned by [GitRepositoryProcessor.processAsync].
     * @throws IllegalStateException
     * @throws Exception
     * @see cloneTagAndProcessDirectoryMany
     * @see cloneBranchAndProcessDirectory
     * @see cloneBranchAndProcessDirectoryMany
     * @see cloneCommitAndProcessDirectory
     * @see cloneCommitAndProcessDirectoryMany
     */
    @Suppress("TYPE_ALIAS")
    fun <T : Any> cloneTagAndProcessDirectory(
        gitDto: GitDto,
        tagName: String,
        repositoryProcessor: GitRepositoryProcessor<Mono<T>>,
    ): Mono<T> = doCloneAndProcessDirectory(gitDto, repositoryProcessor) {
        cloneTagToDirectory(tagName, it)
    }

    /**
     * @param gitDto the Git URL with optional credentials.
     * @param branchName the Git branch.
     * @param repositoryProcessor folder processing should be finished here
     *   &mdash; the folder will be removed afterwards.
     * @return the result returned by [GitRepositoryProcessor.processAsync].
     * @throws IllegalStateException
     * @throws Exception
     * @see cloneTagAndProcessDirectory
     * @see cloneTagAndProcessDirectoryMany
     * @see cloneBranchAndProcessDirectoryMany
     * @see cloneCommitAndProcessDirectory
     * @see cloneCommitAndProcessDirectoryMany
     */
    @Suppress("TYPE_ALIAS")
    fun <T : Any> cloneBranchAndProcessDirectory(
        gitDto: GitDto,
        branchName: String,
        repositoryProcessor: GitRepositoryProcessor<Mono<T>>,
    ): Mono<T> = doCloneAndProcessDirectory(gitDto, repositoryProcessor) {
        cloneBranchToDirectory(branchName, it)
    }

    /**
     * @param gitDto the Git URL with optional credentials.
     * @param commitId the Git commit hash.
     * @param repositoryProcessor folder processing should be finished here
     *   &mdash; the folder will be removed afterwards.
     * @return the result returned by [GitRepositoryProcessor.processAsync].
     * @throws IllegalStateException
     * @throws Exception
     * @see cloneTagAndProcessDirectory
     * @see cloneTagAndProcessDirectoryMany
     * @see cloneBranchAndProcessDirectory
     * @see cloneBranchAndProcessDirectoryMany
     * @see cloneCommitAndProcessDirectoryMany
     */
    @Suppress("TYPE_ALIAS")
    fun <T : Any> cloneCommitAndProcessDirectory(
        gitDto: GitDto,
        commitId: String,
        repositoryProcessor: GitRepositoryProcessor<Mono<T>>,
    ): Mono<T> = doCloneAndProcessDirectory(gitDto, repositoryProcessor) {
        cloneCommitToDirectory(commitId, it)
    }

    /**
     * @param gitDto the Git URL with optional credentials.
     * @param tagName the Git tag.
     * @param repositoryProcessor folder processing should be finished here
     *   &mdash; the folder will be removed afterwards.
     * @return the result returned by [GitRepositoryProcessor.processAsync].
     * @see cloneTagAndProcessDirectory
     * @see cloneBranchAndProcessDirectory
     * @see cloneBranchAndProcessDirectoryMany
     * @see cloneCommitAndProcessDirectory
     * @see cloneCommitAndProcessDirectoryMany
     */
    @Suppress("TYPE_ALIAS")
    fun <T : Any> cloneTagAndProcessDirectoryMany(
        gitDto: GitDto,
        tagName: String,
        repositoryProcessor: GitRepositoryProcessor<Flux<T>>,
    ): Flux<T> = doCloneAndProcessDirectoryMany(gitDto, repositoryProcessor) {
        cloneTagToDirectory(tagName, it)
    }

    /**
     * @param gitDto the Git URL with optional credentials.
     * @param branchName the Git branch.
     * @param repositoryProcessor folder processing should be finished here
     *   &mdash; the folder will be removed afterwards.
     * @return the result returned by [GitRepositoryProcessor.processAsync].
     * @see cloneTagAndProcessDirectory
     * @see cloneTagAndProcessDirectoryMany
     * @see cloneBranchAndProcessDirectory
     * @see cloneCommitAndProcessDirectory
     * @see cloneCommitAndProcessDirectoryMany
     */
    @Suppress("TYPE_ALIAS")
    fun <T : Any> cloneBranchAndProcessDirectoryMany(
        gitDto: GitDto,
        branchName: String,
        repositoryProcessor: GitRepositoryProcessor<Flux<T>>,
    ): Flux<T> = doCloneAndProcessDirectoryMany(gitDto, repositoryProcessor) {
        cloneBranchToDirectory(branchName, it)
    }

    /**
     * @param gitDto
     * @param commitId the Git commit hash.
     * @param repositoryProcessor folder processing should be finished here
     *   &mdash; the folder will be removed afterwards.
     * @return result of [GitRepositoryProcessor.processAsync]
     * @see cloneTagAndProcessDirectory
     * @see cloneTagAndProcessDirectoryMany
     * @see cloneBranchAndProcessDirectory
     * @see cloneBranchAndProcessDirectoryMany
     * @see cloneCommitAndProcessDirectory
     */
    @Suppress("TYPE_ALIAS")
    fun <T : Any> cloneCommitAndProcessDirectoryMany(
        gitDto: GitDto,
        commitId: String,
        repositoryProcessor: GitRepositoryProcessor<Flux<T>>,
    ): Flux<T> = doCloneAndProcessDirectoryMany(gitDto, repositoryProcessor) {
        cloneCommitToDirectory(commitId, it)
    }

    @Suppress("TooGenericExceptionCaught")
    @NonBlocking
    private fun GitDto.cloneAsync(doCloneToDirectory: GitDto.(Path) -> GitCommitInfo): Mono<CloneResult> =
            blockingToMono {
                val tmpDir = createTempDirectoryForRepository()
                val gitCommitInfo = try {
                    doCloneToDirectory(tmpDir)
                } catch (ex: Exception) {
                    log.error(ex) { "Failed to clone git repository $url" }
                    tmpDir.deleteRecursivelySafely()
                    throw ex
                }
                CloneResult(tmpDir, gitCommitInfo)
            }

    @NonBlocking
    private fun CloneResult.cleanupAsync(): Mono<Unit> =
            directory.deleteRecursivelySafelyAsync()

    /**
     * @param doCloneToDirectory a blocking `git-clone` action (will be wrapped
     *   with [blockingToMono]).
     * @see doCloneAndProcessDirectoryMany
     */
    @NonBlocking
    @Suppress("TYPE_ALIAS")
    private fun <T : Any> doCloneAndProcessDirectory(
        gitDto: GitDto,
        repositoryProcessor: GitRepositoryProcessor<Mono<T>>,
        doCloneToDirectory: GitDto.(Path) -> GitCommitInfo,
    ): Mono<T> =
            Mono.usingWhen(
                gitDto.cloneAsync(doCloneToDirectory),
                repositoryProcessor::processAsync,
            ) { cloneResult -> cloneResult.cleanupAsync() }

    /**
     * @param doCloneToDirectory a blocking `git-clone` action (will be wrapped
     *   with [blockingToMono]).
     * @see doCloneAndProcessDirectory
     */
    @NonBlocking
    @Suppress("TYPE_ALIAS")
    private fun <T : Any> doCloneAndProcessDirectoryMany(
        gitDto: GitDto,
        repositoryProcessor: GitRepositoryProcessor<Flux<T>>,
        doCloneToDirectory: GitDto.(Path) -> GitCommitInfo,
    ): Flux<T> =
            Flux.usingWhen(
                gitDto.cloneAsync(doCloneToDirectory),
                repositoryProcessor::processAsync,
            ) { cloneResult -> cloneResult.cleanupAsync() }

    /**
     * @param pathToRepository
     * @param archiveProcessor operation on file should be finished here -- file will be removed after it
     * @return archived git repository, file will be deleted after release Flux
     * @throws IOException
     * @throws Exception
     */
    @NonBlocking
    @Suppress("TooGenericExceptionCaught")
    fun <T> archiveToTar(
        pathToRepository: Path,
        archiveProcessor: ArchiveProcessor<T>
    ): Mono<T> {
        val archiveAction: () -> Path = {
            val tmpFile = createTempTarFile()
            try {
                pathToRepository.compressAsZipTo(tmpFile)
            } catch (ex: Exception) {
                log.error(ex) { "Failed to archive git repository $pathToRepository" }
                tmpFile.deleteRecursivelySafely()
                throw ex
            }
            tmpFile
        }
        return Mono.usingWhen(
            blockingToMono(archiveAction),
            { tmpFile -> archiveProcessor(tmpFile) },
            { tmpFile -> tmpFile.deleteRecursivelySafelyAsync() }
        )
    }

    @NonBlocking
    private fun Path.deleteRecursivelySafelyAsync() = blockingToMono { deleteRecursivelySafely() }

    @Suppress("TooGenericExceptionCaught")
    private fun Path.deleteRecursivelySafely() {
        try {
            log.info {
                "Start cleanup of ${absolutePathString()}"
            }
            FileUtils.delete(toFile(), FileUtils.RECURSIVE or FileUtils.IGNORE_ERRORS or FileUtils.RETRY)
        } catch (ex: Exception) {
            log.error(ex) { "Skip error during clean-up folder" }
        }
    }

    companion object {
        private val log: Logger = getLogger<GitPreprocessorService>()
    }
}
