package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.common.entities.GitDto
import com.saveourtool.common.request.TestsSourceFetchRequest
import com.saveourtool.common.test.TestSuiteValidationResult
import com.saveourtool.common.test.TestsSourceSnapshotDto
import com.saveourtool.common.testsuite.TestSuitesSourceFetchMode
import com.saveourtool.common.utils.*
import com.saveourtool.save.preprocessor.common.CloneAndProcessDirectoryAction
import com.saveourtool.save.preprocessor.common.GitRepositoryProcessor
import com.saveourtool.save.preprocessor.service.GitPreprocessorService
import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.service.TestsPreprocessorToBackendBridge
import com.saveourtool.save.preprocessor.utils.GitCommitInfo

import org.jetbrains.annotations.NonBlocking
import org.slf4j.Logger
import org.springframework.core.io.FileSystemResource
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.nio.file.Path

import kotlin.io.path.div

/**
 * Preprocessor's controller for [com.saveourtool.save.entities.TestSuitesSource]
 */
@RestController
@RequestMapping("/test-suites-sources")
class TestSuitesPreprocessorController(
    private val gitPreprocessorService: GitPreprocessorService,
    private val testDiscoveringService: TestDiscoveringService,
    private val testsPreprocessorToBackendBridge: TestsPreprocessorToBackendBridge,
) {
    /**
     * Fetch new tests suites from provided source from provided version
     *
     * @param request request to fetch tests from tests source
     * @return empty response
     */
    @PostMapping("/fetch")
    fun fetch(
        @RequestBody request: TestsSourceFetchRequest,
    ): Flux<TestSuiteValidationResult> {
        @Suppress("TYPE_ALIAS")
        val cloneAndProcessDirectoryAction: GitPreprocessorService.(GitDto, String, GitRepositoryProcessor<Flux<TestSuiteValidationResult>>) -> Flux<TestSuiteValidationResult> =
                when (request.mode) {
                    TestSuitesSourceFetchMode.BY_BRANCH -> GitPreprocessorService::cloneBranchAndProcessDirectoryMany
                    TestSuitesSourceFetchMode.BY_COMMIT -> GitPreprocessorService::cloneCommitAndProcessDirectoryMany
                    TestSuitesSourceFetchMode.BY_TAG -> GitPreprocessorService::cloneTagAndProcessDirectoryMany
                }

        return fetchTestSuites(
            request = request,
            cloneAndProcessDirectoryAction = cloneAndProcessDirectoryAction
        )
    }

    @NonBlocking
    @Suppress("TYPE_ALIAS")
    private fun fetchTestSuites(
        request: TestsSourceFetchRequest,
        cloneAndProcessDirectoryAction: CloneAndProcessDirectoryAction<Flux<TestSuiteValidationResult>>,
    ): Flux<TestSuiteValidationResult> = with(cloneAndProcessDirectoryAction) {
        gitPreprocessorService.cloneAndProcessDirectoryAsync(
            request.source.gitDto,
            request.version
        ) { (repositoryDirectory, gitCommitInfo) ->
            testsPreprocessorToBackendBridge.findTestsSourceSnapshot(request.source.requiredId(), gitCommitInfo.id)
                .switchIfEmpty {
                    doFetchTests(repositoryDirectory, gitCommitInfo, request)
                }
                .flatMap { snapshot ->
                    testsPreprocessorToBackendBridge.saveTestsSourceVersion(request.createVersion(snapshot))
                }.doOnNext { isSaved: Boolean ->
                    log.info {
                        val messagePrefix = "Tests from ${request.source.gitDto.url}"
                        val status = when {
                            isSaved -> "saved"
                            else -> "not saved: the snapshot already exists"
                        }
                        val messageSuffix = "(version \"${request.version}\"; commit ${gitCommitInfo.id})."

                        "$messagePrefix $status $messageSuffix"
                    }
                }.thenMany(Flux.empty())
        }
    }

    @NonBlocking
    private fun doFetchTests(
        repositoryDirectory: Path,
        gitCommitInfo: GitCommitInfo,
        request: TestsSourceFetchRequest,
    ): Mono<TestsSourceSnapshotDto> = (repositoryDirectory / request.source.testRootPath).let { pathToRepository ->
        gitPreprocessorService.archiveToTar(pathToRepository) { archive ->
            testsPreprocessorToBackendBridge.saveTestsSuiteSourceSnapshot(
                snapshotDto = request.createSnapshot(gitCommitInfo.id, gitCommitInfo.time),
                resourceWithContent = FileSystemResource(archive)
            ).zipWhen { snapshot ->
                testDiscoveringService.detectAndSaveAllTestSuitesAndTests(
                    repositoryPath = repositoryDirectory,
                    testRootPath = request.source.testRootPath,
                    sourceSnapshot = snapshot,
                )
            }
        }
            .map { (snapshot, testSuites) ->
                log.info { "Loaded ${testSuites.size} test suites from tests snapshot id ${snapshot.requiredId()} with version ${snapshot.commitId}" }
                snapshot
            }
            .doOnError(
                Exception
                ::class.java
            ) { ex ->
                log.error(ex) { "Failed to fetch from ${gitCommitInfo.id}" }
            }
    }

    companion object {
        private val log: Logger = getLogger<TestSuitesPreprocessorController>()
    }
}
