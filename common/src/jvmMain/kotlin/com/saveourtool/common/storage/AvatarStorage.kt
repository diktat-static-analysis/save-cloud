package com.saveourtool.common.storage

import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.s3.S3OperationsProperties
import com.saveourtool.common.utils.AvatarType
import com.saveourtool.common.utils.orNotFound
import org.springframework.stereotype.Service

/**
 * Storage for Avatars
 * Currently, key is (AvatarType, ObjectName)
 */
@Service
class AvatarStorage(
    configProperties: S3OperationsProperties.Provider,
    s3Operations: S3Operations,
) : AbstractSimpleReactiveStorage<AvatarKey>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "images", "avatars")
) {
    override fun doBuildKeyFromSuffix(s3KeySuffix: String): AvatarKey {
        val (typeStr, objectName) = s3KeySuffix.s3KeyToParts()
        return AvatarKey(
            type = AvatarType.findByUrlPath(typeStr)
                .orNotFound {
                    "Not supported type for path: $typeStr"
                },
            objectName = objectName,
        )
    }

    override fun doBuildS3KeySuffix(key: AvatarKey): String = concatS3Key(key.type.urlPath, key.objectName)
}
