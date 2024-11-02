package com.saveourtool.common.storage

import com.saveourtool.common.utils.AvatarType

/**
 * @property type
 * @property objectName
 */
data class AvatarKey(
    val type: AvatarType,
    val objectName: String,
)
