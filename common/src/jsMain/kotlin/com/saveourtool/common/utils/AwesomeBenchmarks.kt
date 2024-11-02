package com.saveourtool.common.utils

import com.saveourtool.common.entities.benchmarks.BenchmarkCategoryEnum

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property name
 * @property category
 * @property tags
 * @property language
 * @property license
 * @property scenarios_num
 * @property description
 * @property homepage
 * @property sources
 * @property documentation
 */
@Serializable
data class AwesomeBenchmarks(
    val id: Long,
    val name: String,
    val category: BenchmarkCategoryEnum,
    val tags: String,
    val language: String,
    val license: String,
    @Suppress("VARIABLE_NAME_INCORRECT_FORMAT", "ConstructorParameterNaming")
    val scenarios_num: Long,
    val description: String,
    val homepage: String,
    val sources: String,
    val documentation: String,
)
