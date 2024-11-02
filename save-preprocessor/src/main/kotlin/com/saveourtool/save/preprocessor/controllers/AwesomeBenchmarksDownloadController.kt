package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.common.entities.GitDto
import com.saveourtool.common.entities.benchmarks.BenchmarkEntity
import com.saveourtool.common.spring.utils.applyAll
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.service.GitPreprocessorService
import com.saveourtool.save.preprocessor.utils.*

import com.akuleshov7.ktoml.file.TomlFileReader
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

import java.nio.file.Path

import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlinx.serialization.serializer

/**
 * A Spring controller for git project downloading
 *
 * @param configProperties config properties
 */
@RestController
class AwesomeBenchmarksDownloadController(
    private val configProperties: ConfigProperties,
    private val gitPreprocessorService: GitPreprocessorService,
    customizers: List<WebClientCustomizer>,
) {
    private val webClientBackend = WebClient.builder()
        .baseUrl(configProperties.backend)
        .applyAll(customizers)
        .build()

    /**
     * Controller to download standard test suites
     *
     * @return Empty response entity
     */
    @Suppress("TOO_LONG_FUNCTION", "TYPE_ALIAS")
    @GetMapping("/download/awesome-benchmarks")
    fun downloadAwesomeBenchmarks(): Mono<ResponseEntity<String>> =
            Mono.just(ResponseEntity("Downloading awesome-benchmarks", HttpStatus.ACCEPTED))
                .doOnSuccess {
                    Mono.fromCallable { gitDto.detectDefaultBranchName() }
                        .flatMap { branch ->
                            log.info("Starting to download awesome-benchmarks")
                            gitPreprocessorService.cloneBranchAndProcessDirectory(
                                gitDto,
                                branch
                            ) { (repositoryDir: Path) ->
                                log.info("Awesome-benchmarks were downloaded to ${repositoryDir.absolutePathString()}")
                                processDirectoryAndCleanUp(repositoryDir)
                            }
                        }
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe()
                }.onErrorResume { exception ->
                    log.warn("Cloning ${gitDto.url} repository failed", exception)
                    Mono.just(ResponseEntity("Downloading of awesome-benchmarks failed", HttpStatus.INTERNAL_SERVER_ERROR))
                }

    private fun processDirectoryAndCleanUp(repositoryDirectory: Path): Mono<Void> {
        val resultList =
                (repositoryDirectory / "benchmarks")
                    .toFile()
                    .walk()
                    .filter { it.isFile }
                    .map { TomlFileReader.decodeFromFile<BenchmarkEntity>(serializer(), it.absolutePath) }

        log.info("Detected and decoded ${resultList.count()} benchmarks from awesome list")

        return webClientBackend
            .post()
            .uri("/upload/awesome-benchmarks")
            .body(BodyInserters.fromValue(resultList.toList()))
            .retrieve()
            .onStatus({status -> status != HttpStatus.OK }) { clientResponse ->
                log.error("Error when making request to /internal/upload/awesome-benchmarks: ${clientResponse.statusCode()}")
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Not able to post awesome-benchmarks to backend due to an internal error"
                )
            }
            .toBodilessEntity()
            .then()
    }

    companion object {
        private const val AWESOME_BENCHMARKS_URL = "https://github.com/saveourtool/awesome-benchmarks.git"

        @JvmStatic
        internal val log = LoggerFactory.getLogger(AwesomeBenchmarksDownloadController::class.java)

        @JvmStatic
        internal val gitDto = GitDto(AWESOME_BENCHMARKS_URL)
    }
}
