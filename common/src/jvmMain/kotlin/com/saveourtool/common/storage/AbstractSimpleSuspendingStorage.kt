package com.saveourtool.common.storage

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.key.AbstractS3KeyManager
import com.saveourtool.common.storage.key.S3KeyManager

/**
 * S3 implementation of Storage
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param prefix a common prefix for all S3 keys in this storage
 * @param K type of key
 */
abstract class AbstractSimpleSuspendingStorage<K : Any>(
    s3Operations: S3Operations,
    prefix: String,
) : AbstractSuspendingStorage<K>(s3Operations) {
    override val s3KeyManager: S3KeyManager<K> = object : AbstractS3KeyManager<K>(prefix) {
        override fun buildKeyFromSuffix(s3KeySuffix: String): K = doBuildKeyFromSuffix(s3KeySuffix)
        override fun buildS3KeySuffix(key: K): String = doBuildS3KeySuffix(key)
    }

    /**
     * @param s3KeySuffix cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3KeySuffix]
     */
    protected abstract fun doBuildKeyFromSuffix(s3KeySuffix: String): K

    /**
     * @param key
     * @return suffix for s3 key, cannot start with [PATH_DELIMITER]
     */
    protected abstract fun doBuildS3KeySuffix(key: K): String
}
