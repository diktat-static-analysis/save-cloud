package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.key.S3KeyManager
import java.net.URL
import kotlin.time.Duration.Companion.minutes

/**
 * S3 implementation of [StoragePreSignedUrl]
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param K type of key
 */
abstract class AbstractSimpleStoragePreSignedUrl<K : Any>(
    private val s3Operations: S3Operations,
) : StoragePreSignedUrl<K> {
    /**
     * [S3KeyManager] manager for S3 keys
     */
    protected abstract val s3KeyManager: S3KeyManager<K>

    override fun generateUrlToDownload(key: K): URL? = s3KeyManager.findExistedS3Key(key)?.let { s3Key ->
        s3Operations.requestToDownloadObject(s3Key, downloadDuration)
            .also { request ->
                require(request.isBrowserExecutable) {
                    "Pre-singer url to download object should be browser executable (header-less)"
                }
            }
            .url()
    }

    companion object {
        private val downloadDuration = 15.minutes
    }
}
