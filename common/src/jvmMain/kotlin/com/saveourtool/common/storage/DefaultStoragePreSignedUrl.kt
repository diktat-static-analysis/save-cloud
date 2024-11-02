package com.saveourtool.common.storage

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.key.S3KeyManager
import com.saveourtool.common.storage.request.DownloadRequest
import com.saveourtool.common.storage.request.UploadRequest
import com.saveourtool.common.utils.orNotFound
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import kotlin.time.Duration.Companion.minutes

/**
 * S3 implementation of [StoragePreSignedUrl]
 *
 * @param K type of key
 * @param s3Operations [S3Operations] to operate with S3
 * @param s3KeyManager [S3KeyManager] manager for S3 keys
 */
class DefaultStoragePreSignedUrl<K : Any>(
    private val s3Operations: S3Operations,
    private val s3KeyManager: S3KeyManager<K>,
) : StoragePreSignedUrl<K> {
    override fun generateRequestToDownload(key: K): DownloadRequest<K>? =
            s3KeyManager.findExistedS3Key(key)?.let { s3Key ->
                val request = s3Operations.requestToDownloadObject(s3Key, downloadDuration)
                    .validate()
                val requestFromContainer = s3Operations.requestToDownloadObject(s3Key, downloadDuration, true)
                    .validate()
                DownloadRequest(
                    key,
                    request.url(),
                    requestFromContainer.url(),
                )
            }

    override fun generateRequestToUpload(key: K, contentLength: Long): UploadRequest<K> {
        val s3Key = s3KeyManager.createNewS3Key(key)
        val request = s3Operations.requestToUploadObject(s3Key, contentLength, uploadDuration)
            .validate()
        val requestFromContainer = s3Operations.requestToUploadObject(s3Key, contentLength, uploadDuration, true)
            .validate()
        return UploadRequest(
            s3KeyManager.findKey(s3Key).orNotFound {
                "Not found inserted updated key for $key"
            },
            request.url(),
            request.signedHeaders(),
            requestFromContainer.url(),
            requestFromContainer.signedHeaders(),
        )
    }

    companion object {
        private val downloadDuration = 15.minutes
        private val uploadDuration = 15.minutes

        private fun PresignedGetObjectRequest.validate(): PresignedGetObjectRequest = also { request ->
            require(request.isBrowserExecutable) {
                "Pre-singer url to download object should be browser executable (header-less)"
            }
        }

        private fun PresignedPutObjectRequest.validate(): PresignedPutObjectRequest = also { request ->
            require(request.signedPayload().isEmpty) {
                "Pre-singer url to download object should be without payload"
            }
        }
    }
}
