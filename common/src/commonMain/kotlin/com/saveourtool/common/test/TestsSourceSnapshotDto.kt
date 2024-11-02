package com.saveourtool.common.test

import com.saveourtool.common.entities.DtoWithId
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property sourceId ID of [com.saveourtool.save.testsuite.TestSuitesSourceDto]
 * @property commitId hash-code
 * @property commitTime time of commit
 * @property id ID of saved entity
 */
@Serializable
data class TestsSourceSnapshotDto(
    val sourceId: Long,
    val commitId: String,
    val commitTime: LocalDateTime,
    override val id: Long? = null,
) : DtoWithId()
