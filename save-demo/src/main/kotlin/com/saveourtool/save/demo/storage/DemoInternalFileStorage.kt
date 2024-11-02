package com.saveourtool.save.demo.storage

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.impl.AbstractInternalFileStorage
import com.saveourtool.common.storage.impl.InternalFileKey
import com.saveourtool.save.demo.config.ConfigProperties

import org.springframework.stereotype.Component

/**
 * Storage for internal files used by demo: save-demo-agent
 */
@Component
class DemoInternalFileStorage(
    configProperties: ConfigProperties,
    s3Operations: S3Operations,
) : AbstractInternalFileStorage(
    listOf(saveDemoAgent),
    configProperties.s3Storage.prefix,
    s3Operations,
) {
    companion object {
        /**
         * [InternalFileKey] for *save-demo-agent*
         */
        val saveDemoAgent: InternalFileKey = InternalFileKey.latest("save-demo-agent", "save-demo-agent.kexe")
    }
}
