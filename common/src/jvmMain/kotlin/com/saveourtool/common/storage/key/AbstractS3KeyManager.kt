package com.saveourtool.common.storage.key

import com.saveourtool.common.storage.PATH_DELIMITER
import com.saveourtool.common.storage.asS3CommonPrefix

/**
 * A common implementation for [S3KeyManager]
 *
 * @param prefix a common prefix for all S3 keys in this storage
 */
abstract class AbstractS3KeyManager<K : Any>(
    prefix: String,
) : S3KeyManager<K> {
    final override val commonPrefix: String = prefix.asS3CommonPrefix()

    /**
     * @param s3Key cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3Key]
     */
    final override fun findKey(s3Key: String): K = buildKeyFromSuffix(s3Key.removePrefix(commonPrefix))

    /**
     * @param s3KeySuffix cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3KeySuffix]
     */
    protected abstract fun buildKeyFromSuffix(s3KeySuffix: String): K

    override fun createNewS3Key(key: K): String = commonPrefix + buildS3KeySuffix(key).validateSuffix()

    override fun findExistedS3Key(key: K): String? = commonPrefix + buildS3KeySuffix(key).validateSuffix()

    /**
     * @param key
     * @return suffix for s3 key, cannot start with [PATH_DELIMITER]
     */
    protected abstract fun buildS3KeySuffix(key: K): String

    override fun delete(key: K): Unit = Unit

    override fun deleteAll(keys: Collection<K>) = Unit

    companion object {
        private fun String.validateSuffix(): String = also { suffix ->
            require(!suffix.startsWith(PATH_DELIMITER)) {
                "Suffix cannot start with $PATH_DELIMITER: $suffix"
            }
        }
    }
}
