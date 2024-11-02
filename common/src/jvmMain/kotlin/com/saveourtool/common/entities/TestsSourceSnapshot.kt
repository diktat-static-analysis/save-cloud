package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.common.test.TestsSourceSnapshotDto

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime

/**
 * @property source [TestSuitesSource]
 * @property commitId hash-code
 * @property commitTime time of commit
 */
@Entity
class TestsSourceSnapshot(
    @ManyToOne
    @JoinColumn(name = "source_id")
    var source: TestSuitesSource,

    var commitId: String,
    var commitTime: LocalDateTime,
) : BaseEntityWithDtoWithId<TestsSourceSnapshotDto>() {
    override fun toDto(): TestsSourceSnapshotDto = TestsSourceSnapshotDto(
        sourceId = source.requiredId(),
        commitId = commitId,
        commitTime = commitTime.toKotlinLocalDateTime(),
        id = id,
    )

    companion object {
        val empty = TestsSourceSnapshot(
            source = TestSuitesSource.empty,
            commitId = "",
            commitTime = LocalDateTime.MIN,
        )

        /**
         * @param sourceResolver returns [TestSuitesSource] by ID
         * @return [TestsSourceSnapshot] created from [TestsSourceSnapshotDto]
         */
        fun TestsSourceSnapshotDto.toEntity(sourceResolver: (Long) -> TestSuitesSource): TestsSourceSnapshot = TestsSourceSnapshot(
            source = sourceResolver(sourceId),
            commitId = commitId,
            commitTime = commitTime.toJavaLocalDateTime(),
        ).apply {
            this.id = this@toEntity.id
        }
    }
}
