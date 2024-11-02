/**
 * DTOs for public test transfer
 */

package com.saveourtool.common.test

import kotlinx.serialization.Serializable

/**
 * @property testLines public test's test file lines
 * @property expectedLines public test's expected file lines
 * @property tags list of tags of current test
 * @property language
 */
@Serializable
data class TestFilesContent(
    val testLines: List<String>,
    val expectedLines: List<String>?,
    val language: String? = null,
    val tags: List<String> = emptyList(),
) {
    companion object {
        val empty = TestFilesContent(emptyList(), null)
    }
}
