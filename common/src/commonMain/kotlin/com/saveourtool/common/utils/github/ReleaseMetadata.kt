package com.saveourtool.common.utils.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * _GitHub_ release metadata.
 *
 * @property name the human-readable release name (may differ from the VCS tag).
 * @property tagName the VCS tag name.
 * @property draft whether this is a draft release.
 * @property prerelease whether this is a pre-release.
 * @property assets the list of assets (files) included in this release.
 */
@Serializable
data class ReleaseMetadata(
    val name: String,
    @SerialName("tag_name")
    val tagName: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val assets: List<ReleaseAsset>
)
