package com.saveourtool.common.filters

import com.saveourtool.common.utils.DATABASE_DELIMITER
import kotlinx.serialization.Serializable

/**
 * Filters for test suites
 * @property name
 * @property language
 * @property tags
 */
@Serializable
data class TestSuiteFilter(
    val name: String,
    val language: String,
    val tags: String,
) {
    /**
     * @return [tags] as list
     */
    fun tagsAsList() = tags.split(DATABASE_DELIMITER).distinct()

    /**
     * @param additionalParams some extra parameters that should be in query
     * @return [TestSuiteFilter] as query params for request
     */
    fun toQueryParams(vararg additionalParams: Pair<String, String>) = listOf(
        "tags" to tags,
        "name" to name,
        "language" to language
    )
        .filter { it.second.isNotBlank() }
        .plus(additionalParams)
        .joinToString("&") {
            "${it.first}=${it.second}"
        }
        .let { query ->
            if (query.isNotBlank()) {
                "?$query"
            } else {
                query
            }
        }

    /**
     * @return true if no filters are set, false otherwise
     */
    fun isEmpty() = name.isBlank() && tags.isBlank() && language.isBlank()

    /**
     * @return true if any filter is set, false otherwise
     */
    fun isNotEmpty() = !isEmpty()

    companion object {
        val empty = TestSuiteFilter(name = "", tags = "", language = "")
    }
}
