package com.saveourtool.common.entities

import kotlinx.datetime.LocalDateTime

/**
 * @property organizationName
 * @property testSuitesSourceName
 * @property version
 * @property creationTime
 * @property location
 */
data class TestSuitesSourceSnapshotDto(
    val organizationName: String,
    val testSuitesSourceName: String,
    val version: String,
    val creationTime: LocalDateTime,

    val location: String,
)
