package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.common.test.TestsSourceVersionDto
import com.saveourtool.common.test.TestsSourceVersionInfo
import com.saveourtool.common.testsuite.TestSuitesSourceFetchMode

import java.time.LocalDateTime
import javax.persistence.*

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime

/**
 * @property snapshot [TestsSourceSnapshot]
 * @property name human-readable version
 * @property createdByUser [User] created this version
 * @property creationTime time of creation this version
 * @property type
 */
@Entity
class TestsSourceVersion(
    @ManyToOne
    @JoinColumn(name = "snapshot_id")
    var snapshot: TestsSourceSnapshot,

    var name: String,
    @Enumerated(EnumType.STRING)
    var type: TestSuitesSourceFetchMode,

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    val createdByUser: User,
    val creationTime: LocalDateTime,
) : BaseEntityWithDtoWithId<TestsSourceVersionDto>() {
    override fun toDto(): TestsSourceVersionDto = TestsSourceVersionDto(
        snapshotId = snapshot.requiredId(),
        name = name,
        type = type,
        createdByUserId = createdByUser.requiredId(),
        creationTime = creationTime.toKotlinLocalDateTime(),
        id = id,
    )

    /**
     * @return [TestsSourceVersionInfo] created from [TestsSourceVersion]
     */
    fun toInfo(): TestsSourceVersionInfo = TestsSourceVersionInfo(
        organizationName = snapshot.source.organization.name,
        sourceName = snapshot.source.name,
        commitId = snapshot.commitId,
        commitTime = snapshot.commitTime.toKotlinLocalDateTime(),
        version = name,
        type = type,
        creationTime = creationTime.toKotlinLocalDateTime(),
        createdByUserName = createdByUser.name,
    )

    companion object {
        /**
         * @param snapshotResolver returns [TestsSourceSnapshot] by ID
         * @param userResolver returns [User] by ID
         * @return [TestsSourceVersion] created from [TestsSourceVersionDto]
         */
        fun TestsSourceVersionDto.toEntity(
            snapshotResolver: (Long) -> TestsSourceSnapshot,
            userResolver: (Long) -> User,
        ): TestsSourceVersion = TestsSourceVersion(
            snapshot = snapshotResolver(snapshotId),
            name = name,
            type = type,
            createdByUser = userResolver(createdByUserId),
            creationTime = creationTime.toJavaLocalDateTime(),
        ).apply {
            id = this@toEntity.id
        }
    }
}
