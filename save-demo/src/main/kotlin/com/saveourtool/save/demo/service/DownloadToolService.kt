package com.saveourtool.save.demo.service

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.entities.FileDto
import com.saveourtool.common.utils.*
import com.saveourtool.common.utils.github.GitHubHelper.downloadAsset
import com.saveourtool.common.utils.github.GitHubHelper.queryMetadata
import com.saveourtool.common.utils.github.ReleaseAsset
import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.entity.*
import com.saveourtool.save.demo.storage.DependencyStorage

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.CancellationException
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.nio.ByteBuffer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.flux
import kotlinx.serialization.json.Json

/**
 * Service that downloads tools from GitHub when starting
 */
@Service
class DownloadToolService(
    private val dependencyStorage: DependencyStorage,
    private val toolService: ToolService,
    private val githubRepoService: GithubRepoService,
    private val snapshotService: SnapshotService,
    private val configProperties: ConfigProperties,
    private val demoService: DemoService,
) {
    @Suppress("InjectDispatcher")
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val httpClient = httpClient()

    private fun downloadFileByFileIdAsFlux(fileId: Long): Flux<ByteBuffer> = flux {
        httpClient.post {
            url("${configProperties.backendUrl}/files/download?fileId=$fileId")
            accept(ContentType.Application.OctetStream)
        }
            .bodyAsChannel()
            .toByteBufferFlow()
            .collect {
                channel.send(it)
            }
    }

    /**
     * @param repo
     * @param vcsTagName
     * @return [DisposableHandle]
     */
    fun downloadFromGithubAndUploadToStorage(repo: GithubRepo, vcsTagName: String) = getExecutable(repo, vcsTagName)
        .let { asset ->
            scope.launch {
                downloadAsset(asset) { content ->
                    dependencyStorage.findDependency(repo.organizationName, repo.projectName, vcsTagName, asset.name)
                        .switchIfEmpty {
                            demoService.findBySaveourtoolProjectOrNotFound(repo.organizationName, repo.projectName) {
                                "Not found demo for ${repo.toPrettyString()}"
                            }
                                .map {
                                    Dependency(it, vcsTagName, asset.name, -1L)
                                }
                        }
                        .flatMap { dependencyStorage.overwrite(it, asset.size, content.toByteBufferFlow().asFlux()) }
                        .subscribe()
                }
            }
                .invokeOnCompletion { exception ->
                    exception?.let {
                        if (it is CancellationException) {
                            logger.debug("Download of ${repo.toPrettyString()} was cancelled.")
                        } else {
                            logger.error("Error while downloading ${repo.toPrettyString()}: ${it.message}")
                        }
                    } ?: logger.debug("${repo.toPrettyString()} was successfully downloaded.")
                }
        }

    private fun getExecutable(repo: GithubRepo, vcsTagName: String): ReleaseAsset {
        val channel: Channel<ReleaseAsset> = Channel()
        scope.launch {
            queryMetadata(repo, vcsTagName).assets
                .filterNot(ReleaseAsset::isDigest)
                .first()
                .let {
                    channel.send(it)
                    channel.close()
                }
        }
        return runBlocking { channel.receive() }
    }

    /**
     * Perform GitHub tool download if [githubProjectCoordinates] is not null
     *
     * @param githubProjectCoordinates
     * @param vcsTagName
     * @return [Tool] that has been downloaded
     */
    fun initializeGithubDownload(githubProjectCoordinates: ProjectCoordinates?, vcsTagName: String): Mono<Tool> = blockingToMono {
        githubProjectCoordinates?.toGithubRepo()?.let { githubRepoService.saveIfNotPresent(it) }
    }
        .zipWhen { repo ->
            Snapshot(vcsTagName, getExecutableName(repo, vcsTagName))
                .let { blockingToMono { snapshotService.saveIfNotPresent(it) } }
        }
        .flatMap { (repo, snapshot) ->
            blockingToMono { toolService.saveIfNotPresent(repo, snapshot) }
        }
        .asyncEffectIf({ this.id != null }) {
            blockingToMono { downloadFromGithubAndUploadToStorage(it.githubRepo, it.snapshot.version) }
        }

    /**
     * Get name of GitHub downloaded asset
     *
     * @param repo GitHub repo coordinates
     * @param vcsTagName string that represents the version control system tag
     * @return executable name
     */
    fun getExecutableName(repo: GithubRepo, vcsTagName: String) = getExecutable(repo, vcsTagName).name

    /**
     * Upload [dependency] to storage (will be overwritten if already present)
     *
     * @param backendFile [FileDto] taken from backend
     * @param dependency that should be saved to [dependencyStorage]
     * @return updated [Dependency]
     */
    fun downloadToStorage(backendFile: FileDto, dependency: Dependency): Mono<Dependency> = run {
        require(backendFile.requiredId() == dependency.fileId) {
            "Invalid link between backend file $backendFile and dependency $dependency"
        }
        require(backendFile.sizeBytes > 0) {
            "Invalid content length of backend file $backendFile"
        }
        with(dependency) {
            dependencyStorage.overwrite(dependency, backendFile.sizeBytes, downloadFileByFileIdAsFlux(backendFile.requiredId()))
                .doOnSuccess { logger.debug("Successfully downloaded $fileName for ${demo.organizationName}/${demo.projectName}.") }
        }
    }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<DownloadToolService>()
        private fun httpClient(): HttpClient = HttpClient {
            install(KubernetesServiceAccountAuthHeaderPlugin)
            install(ContentNegotiation) {
                val json = Json { ignoreUnknownKeys = true }
                json(json)
            }
        }
    }
}
