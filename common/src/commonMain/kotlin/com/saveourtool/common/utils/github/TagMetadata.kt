package com.saveourtool.common.utils.github

import kotlinx.serialization.Serializable

/**
 * _GitHub_ tag metadata.
 *
 * @property name the human-readable release name (may differ from the VCS tag).
 */
@Serializable
data class TagMetadata(
    val name: String,
)
