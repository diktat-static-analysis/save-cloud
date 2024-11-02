package com.saveourtool.save.backend.storage

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.DefaultStorageCoroutines
import com.saveourtool.common.storage.impl.AbstractInternalFileStorage
import com.saveourtool.common.storage.impl.InternalFileKey
import com.saveourtool.save.backend.configs.ConfigProperties

import generated.SAVE_CLI_VERSION
import org.springframework.stereotype.Component

/**
 * Storage for internal files used by backend: save-cli and save-agent
 */
@Component
class BackendInternalFileStorage(
    configProperties: ConfigProperties,
    s3Operations: S3Operations,
) : AbstractInternalFileStorage(
    listOf(InternalFileKey.saveAgentKey, InternalFileKey.saveCliKey(SAVE_CLI_VERSION)),
    configProperties.s3Storage.prefix,
    s3Operations,
) {
    override suspend fun doInitAdditionally(underlying: DefaultStorageCoroutines<InternalFileKey>) {
        underlying.downloadSaveCliFromGithub()
    }
}
