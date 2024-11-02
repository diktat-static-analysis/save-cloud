package com.saveourtool.common.test

import com.saveourtool.common.entities.DtoWithId
import com.saveourtool.common.testsuite.TestSuitesSourceFetchMode
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property snapshotId ID of [TestsSourceSnapshotDto]
 * @property name human-readable version
 * @property type type of fetch mode
 * @property createdByUserId ID of [com.saveourtool.save.info.UserInfo] created this version
 * @property creationTime time of creation this version
 * @property id ID of saved entity
 */
@Serializable
data class TestsSourceVersionDto(
    val snapshotId: Long,
    val name: String,
    val type: TestSuitesSourceFetchMode,
    val createdByUserId: Long,
    val creationTime: LocalDateTime,
    override val id: Long? = null,
) : DtoWithId()
