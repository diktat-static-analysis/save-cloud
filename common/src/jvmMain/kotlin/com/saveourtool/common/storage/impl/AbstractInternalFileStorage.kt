package com.saveourtool.common.storage.impl

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.*
import com.saveourtool.common.storage.key.AbstractS3KeyManager
import com.saveourtool.common.storage.key.S3KeyManager
import com.saveourtool.common.utils.*
import com.saveourtool.common.utils.github.GitHubHelper
import com.saveourtool.common.utils.github.GitHubRepoInfo

import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.net.URL
import javax.annotation.PostConstruct

/**
 * Storage for internal files used by backend and demo: save-cli and save-agent/save-demo-agent
 *
 * @param keysToLoadFromClasspath a list of keys which need to load on init
 * @param s3StoragePrefix a common prefix for s3 storage
 * @param s3Operations
 */
open class AbstractInternalFileStorage(
    private val keysToLoadFromClasspath: Collection<InternalFileKey>,
    s3StoragePrefix: String,
    s3Operations: S3Operations,
) {
    private val log: Logger = getLogger(this::class)
    private val initializer: StorageInitializer = StorageInitializer(this::class)
    private val s3KeyManager: S3KeyManager<InternalFileKey> = object : AbstractS3KeyManager<InternalFileKey>(concatS3Key(s3StoragePrefix, "internal-storage")) {
        override fun buildKeyFromSuffix(s3KeySuffix: String): InternalFileKey {
            val (name, version, fileName) = s3KeySuffix.s3KeyToParts()
            return InternalFileKey(
                name = name,
                version = version,
                fileName = fileName,
            )
        }
        override fun buildS3KeySuffix(key: InternalFileKey): String = concatS3Key(key.name, key.version, key.fileName)
    }
    private val storageProjectReactor by lazy { DefaultStorageProjectReactor(s3Operations, s3KeyManager) }
    private val storageCoroutines by lazy { DefaultStorageCoroutines(s3Operations, s3KeyManager) }
    private val storagePreSignedUrl by lazy { DefaultStoragePreSignedUrl(s3Operations, s3KeyManager) }

    /**
     * An init method to upload internal files to S3 from classpath or github
     */
    @PostConstruct
    fun doInit() {
        initializer.init(
            doInitReactively = {
                Flux.concat(
                    keysToLoadFromClasspath.toFlux()
                        .flatMap {
                            storageProjectReactor.uploadFromClasspath(it)
                        },
                    doInitAdditionally(storageProjectReactor)
                ).last()
            },
            doInitSuspendedly = { doInitAdditionally(storageCoroutines) }
        )
    }

    /**
     * Async method to init this storage: copy required files to storage
     *
     * @param underlying
     * @return [Mono] without body
     */
    protected open fun doInitAdditionally(underlying: DefaultStorageProjectReactor<InternalFileKey>): Mono<Unit> = Mono.empty()

    /**
     * Suspend method to init this storage: copy required files to storage
     *
     * @param underlying
     * @return [Mono] without body
     */
    protected open suspend fun doInitAdditionally(underlying: DefaultStorageCoroutines<InternalFileKey>): Unit? = null

    /**
     * @param key
     * @return generated [URL] to download provided [key] [InternalFileKey] from container
     * @throws ResponseStatusException with status [HttpStatus.NOT_FOUND]
     */
    fun generateRequiredUrlToDownloadFromContainer(key: InternalFileKey): URL = storagePreSignedUrl.generateRequiredRequestToDownload(key).urlFromContainer

    /**
     * @param function
     * @return result of [function] which is run using [StorageProjectReactor]
     */
    fun <T> usingProjectReactor(function: StorageProjectReactor<InternalFileKey>.() -> T): T = initializer.validateAndRun {
        function(storageProjectReactor)
    }

    /**
     * @param function
     * @return result of [function] which is run using [StorageCoroutines]
     */
    suspend fun <T> usingCoroutines(function: suspend StorageCoroutines<InternalFileKey>.() -> T): T = initializer.validateAndRunSuspend {
        function(storageCoroutines)
    }

    /**
     * @param function
     * @return result of [function] which is run using [StoragePreSignedUrl]
     */
    fun <T> usingPreSignedUrl(function: StoragePreSignedUrl<InternalFileKey>.() -> T): T = initializer.validateAndRun {
        function(storagePreSignedUrl)
    }

    private fun StorageProjectReactor<InternalFileKey>.uploadFromClasspath(key: InternalFileKey): Mono<Unit> = doesExist(key)
        .filterWhen { exists ->
            if (exists && key.isLatest()) {
                delete(key)
            } else {
                exists.not().toMono()
            }
        }
        .flatMap {
            tryDownloadFromClasspath(key.fileName)
                .flatMap { resource ->
                    upload(
                        key,
                        resource.contentLength(),
                        resource.toByteBufferFlux(),
                    )
                }
                .effectIfEmpty {
                    log.info {
                        "There is no ${key.name} with version ${key.version} in classpath"
                    }
                }
        }
        .thenReturn(Unit)
        .defaultIfEmpty(Unit)

    /**
     * Download save-cli from GitHub ([saveCliRepo]) latest [SAVE_CLI_LIMIT] versions
     */
    protected suspend fun DefaultStorageCoroutines<InternalFileKey>.downloadSaveCliFromGithub() {
        GitHubHelper.availableTags(saveCliRepo)
            .sorted()
            .takeLast(SAVE_CLI_LIMIT)
            .map { tagName ->
                InternalFileKey.saveCliKey(tagName.removePrefix("v")) to tagName
            }
            .filterNot { (key, _) ->
                doesExist(key)
            }
            .forEach { (key, tagName) ->
                GitHubHelper.download(saveCliRepo, tagName, key.fileName) { (content, contentLength) ->
                    val uploadedKey = upload(key, contentLength, content.toByteBufferFlow())
                    log.info {
                        "Uploaded $uploadedKey to internal storage"
                    }
                } ?: log.warn {
                    "Not found $key in github"
                }
            }
    }

    companion object {
        private const val SAVE_CLI_LIMIT = 3
        private val saveCliRepo = object : GitHubRepoInfo {
            override val organizationName: String = "saveourtool"
            override val projectName: String = "save-cli"
        }
    }
}
