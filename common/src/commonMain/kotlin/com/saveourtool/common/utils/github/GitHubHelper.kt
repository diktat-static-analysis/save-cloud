package com.saveourtool.common.utils.github

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json

/**
 * A singleton to download assets from GitHub
 */
object GitHubHelper {
    /**
     * A base url for GitHub api
     */
    const val API_URL = "https://api.github.com/repos"

    /**
     * Version which specified the latest version
     */
    const val LATEST_VERSION = "latest"
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            val json = Json { ignoreUnknownKeys = true }
            json(json)
        }
    }

    /**
     * Queries metadata from GitHub in provided [repo] with provided [tagName]
     *
     * @param repo
     * @param tagName
     * @return metadata as [ReleaseMetadata]
     */
    suspend fun queryMetadata(repo: GitHubRepoInfo, tagName: String): ReleaseMetadata = httpClient.get(repo.getMetadataUrl(tagName)).body()

    /**
     * Downloads provided [asset] from GitHub
     *
     * @param asset
     * @param consumer consumer of content of provided asset
     * @return result of consumer [R] or null
     */
    suspend fun <R : Any> downloadAsset(
        asset: ReleaseAsset,
        consumer: suspend (ByteReadChannel) -> R,
    ): R? = httpClient.prepareGet {
        url(asset.downloadUrl)
        accept(asset.contentType())
    }
        .execute { response ->
            if (response.status.isSuccess()) {
                consumer(response.bodyAsChannel())
            } else {
                null
            }
        }

    /**
     * Fetches a list of tags from GitHub in provided [repo]
     *
     * @param repo
     * @return list of tags
     */
    suspend fun availableTags(repo: GitHubRepoInfo): List<String> = httpClient.get(repo.getTagsUrl())
        .body<List<TagMetadata>>()
        .map { it.name }

    /**
     * Downloads asset from GitHub in provided [repo] with provided [tagName]
     * It expects that there is only a single asset
     *
     * @param repo
     * @param tagName
     * @param assetName
     * @param consumer consumer of content of found asset with its size
     * @return result of consumer [R] or null
     */
    suspend fun <R : Any> download(
        repo: GitHubRepoInfo,
        tagName: String,
        assetName: String,
        consumer: suspend (Pair<ByteReadChannel, Long>) -> R
    ): R? = queryMetadata(repo, tagName)
        .assets
        .singleOrNull { asset -> assetName == asset.name }
        ?.let { asset ->
            downloadAsset(asset) { content ->
                consumer(content to asset.size)
            }
        }
}
