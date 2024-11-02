package com.saveourtool.common.test

import com.saveourtool.common.testsuite.TestSuitesSourceFetchMode
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

typealias TestsSourceVersionInfoList = List<TestsSourceVersionInfo>

/**
 * Info about tests snapshot with commit technical info and version provided by user

 * @property organizationName
 * @property sourceName
 * @property commitId commit hash, unique ID of tests snapshot
 * @property commitTime commit time according to git history
 * @property version human-readable version provided by user
 * @property type version type
 * @property createdByUserName username who created this version
 * @property creationTime time when this version is created
 */
@Serializable
data class TestsSourceVersionInfo(
    val organizationName: String,
    val sourceName: String,
    val commitId: String,
    val commitTime: LocalDateTime,
    val version: String,
    val type: TestSuitesSourceFetchMode,
    val createdByUserName: String,
    val creationTime: LocalDateTime,
)
