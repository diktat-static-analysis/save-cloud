package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.key.AbstractS3KeyManager
import com.saveourtool.save.storage.key.S3KeyManager

abstract class AbstractSimpleStorage<K : Any>(
    s3Operations: S3Operations,
    prefix: String,
) : AbstractS3Storage<K>(
    s3Operations,
) {
    override val s3KeyManager: S3KeyManager<K> = object : AbstractS3KeyManager<K>(prefix) {
        override fun delete(key: K): Unit = Unit
        override fun buildKeyFromSuffix(s3KeySuffix: String): K = this@AbstractSimpleStorage.buildKeyFromSuffix(s3KeySuffix)
        override fun buildS3KeySuffix(key: K): String = this@AbstractSimpleStorage.buildS3KeySuffix(key)
    }

    abstract fun buildKeyFromSuffix(s3KeySuffix: String): K

    abstract fun buildS3KeySuffix(key: K): String
}
