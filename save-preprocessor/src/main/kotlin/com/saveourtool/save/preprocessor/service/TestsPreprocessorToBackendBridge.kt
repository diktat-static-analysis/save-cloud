package com.saveourtool.save.preprocessor.service

import com.saveourtool.common.entities.*
import com.saveourtool.common.spring.utils.applyAll
import com.saveourtool.common.storage.request.UploadRequest
import com.saveourtool.common.test.TestDto
import com.saveourtool.common.test.TestsSourceSnapshotDto
import com.saveourtool.common.test.TestsSourceVersionDto
import com.saveourtool.common.testsuite.*
import com.saveourtool.common.utils.*
import com.saveourtool.save.preprocessor.config.ConfigProperties

import org.jetbrains.annotations.NonBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

typealias TestsSourceSnapshotUploadRequest = UploadRequest<TestsSourceSnapshotDto>

/**
 * A bridge from preprocessor to backend (rest api wrapper)
 */
@Service
class TestsPreprocessorToBackendBridge(
    configProperties: ConfigProperties,
    customizers: List<WebClientCustomizer>,
) {
    private val webClientBackend = WebClient.builder()
        .baseUrl(configProperties.backend)
        .applyAll(customizers)
        .build()
    private val uploadWebClient = WebClient.create()

    /**
     * @param snapshotDto
     * @param resourceWithContent
     * @return updated [snapshotDto]
     */
    @NonBlocking
    fun saveTestsSuiteSourceSnapshot(
        snapshotDto: TestsSourceSnapshotDto,
        resourceWithContent: Resource,
    ): Mono<TestsSourceSnapshotDto> = webClientBackend.post()
        .uri("/test-suites-sources/generate-url-to-upload-snapshot")
        .bodyValue(snapshotDto)
        .header(CONTENT_LENGTH_CUSTOM, resourceWithContent.contentLength().toString())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono<TestsSourceSnapshotUploadRequest>()
        .flatMap { uploadRequest ->
            uploadWebClient.put()
                .uri(uploadRequest.url.toURI())
                .headers { it.putAll(uploadRequest.headers) }
                // a workaround to avoid overriding Content-Type by Spring which uses resource extension to resolve it
                .body(BodyInserters.fromResource(InputStreamResource(resourceWithContent.inputStream)))
                .retrieve()
                .blockingToBodilessEntity()
                .onErrorResume { ex ->
                    webClientBackend.delete()
                        .uri("/test-suites-sources/delete-snapshot?snapshotId={id}", uploadRequest.key.requiredId())
                        .retrieve()
                        .toBodilessEntity()
                        .then(Mono.error(ex))
                }
                .thenReturn(uploadRequest.key)
        }

    /**
     * @param sourceId
     * @param commitId
     * @return [TestsSourceSnapshotDto] found by provided values
     */
    @NonBlocking
    fun findTestsSourceSnapshot(sourceId: Long, commitId: String): Mono<TestsSourceSnapshotDto> = webClientBackend.get()
        .uri("/test-suites-sources/find-snapshot?sourceId={sourceId}&commitId={commitId}", sourceId, commitId)
        .retrieve()
        .blockingBodyToMono()

    /**
     * @param testsSourceVersionDto the version to save.
     * @return `true` if the [version][testsSourceVersionDto] was saved, `false`
     *   if the version with the same [name][TestsSourceVersionDto.name] and
     *   numeric [snapshot id][TestsSourceVersionDto.snapshotId] already exists.
     */
    @NonBlocking
    fun saveTestsSourceVersion(testsSourceVersionDto: TestsSourceVersionDto): Mono<Boolean> = webClientBackend
        .post()
        .uri("/test-suites-sources/save-version")
        .bodyValue(testsSourceVersionDto)
        .retrieve()
        .blockingBodyToMono()

    /**
     * @param testSuiteDto
     * @return saved [TestSuite]
     */
    @NonBlocking
    fun saveTestSuite(testSuiteDto: TestSuiteDto): Mono<TestSuite> = webClientBackend.post()
        .uri("/test-suites/save")
        .bodyValue(testSuiteDto)
        .retrieve()
        .blockingBodyToMono()

    /**
     * @param tests
     * @return empty response
     */
    @NonBlocking
    fun saveTests(tests: Flux<TestDto>): Flux<EmptyResponse> = tests
        .buffer(TESTS_BUFFER_SIZE)
        .doOnNext {
            log.debug { "Processing chuck of tests [${it.first()} ... ${it.last()}]" }
        }
        .flatMap { chunk ->
            webClientBackend.post()
                .uri("/initializeTests")
                .bodyValue(chunk)
                .retrieve()
                .blockingToBodilessEntity()
        }

    companion object {
        private val log = LoggerFactory.getLogger(TestsPreprocessorToBackendBridge::class.java)

        // default Webflux in-memory buffer is 256 KiB
        private const val TESTS_BUFFER_SIZE = 128
    }
}
