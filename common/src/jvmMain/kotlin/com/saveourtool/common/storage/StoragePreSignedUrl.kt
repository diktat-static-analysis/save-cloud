package com.saveourtool.common.storage

import com.saveourtool.common.storage.request.DownloadRequest
import com.saveourtool.common.storage.request.UploadRequest
import com.saveourtool.common.utils.orNotFound
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.net.URL

/**
 * Base interface for Storage with methods for [pre-signed url](https://docs.aws.amazon.com/AmazonS3/latest/userguide/example_s3_Scenario_PresignedUrl_section.html)
 *
 * @param K type of key
 */
interface StoragePreSignedUrl<K : Any> {
    /**
     * @param key a key to download content
     * @return [DownloadRequest] with [URL] to download content if [key] valid, otherwise -- null
     */
    fun generateRequestToDownload(key: K): DownloadRequest<K>?

    /**
     * @param key
     * @return generated [DownloadRequest] with [URL] to download provided [key] [K]
     * @throws ResponseStatusException with status [HttpStatus.NOT_FOUND]
     */
    fun generateRequiredRequestToDownload(key: K): DownloadRequest<K> = generateRequestToDownload(key)
        .orNotFound {
            "Not found $key in ${this::class.simpleName} storage"
        }

    /**
     * @param key a key for provided content
     * @param contentLength a content length of content
     * @return uploaded key and URL with headers to upload content as [UploadRequest]
     */
    fun generateRequestToUpload(key: K, contentLength: Long): UploadRequest<K>
}
