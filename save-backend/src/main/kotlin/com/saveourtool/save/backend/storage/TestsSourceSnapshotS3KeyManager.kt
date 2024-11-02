package com.saveourtool.save.backend.storage

import com.saveourtool.common.entities.TestSuitesSource
import com.saveourtool.common.entities.TestsSourceSnapshot
import com.saveourtool.common.entities.TestsSourceSnapshot.Companion.toEntity
import com.saveourtool.common.storage.concatS3Key
import com.saveourtool.common.storage.key.AbstractS3KeyDtoManager
import com.saveourtool.common.test.TestsSourceSnapshotDto
import com.saveourtool.common.utils.BlockingBridge
import com.saveourtool.common.utils.getByIdOrNotFound
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.backend.repository.TestsSourceSnapshotRepository
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.service.TestSuitesService

import org.springframework.stereotype.Component

/**
 * [com.saveourtool.save.storage.key.S3KeyManager] for [TestsSourceSnapshotStorage]
 */
@Component
class TestsSourceSnapshotS3KeyManager(
    configProperties: ConfigProperties,
    testsSourceSnapshotRepository: TestsSourceSnapshotRepository,
    blockingBridge: BlockingBridge,
    private val testSuitesSourceRepository: TestSuitesSourceRepository,
    private val testSuitesService: TestSuitesService,
    private val executionService: ExecutionService,
) : AbstractS3KeyDtoManager<TestsSourceSnapshotDto, TestsSourceSnapshot, TestsSourceSnapshotRepository>(
    concatS3Key(configProperties.s3Storage.prefix, "tests-source-snapshot"),
    testsSourceSnapshotRepository,
    blockingBridge
) {
    override fun createNewEntityFromDto(dto: TestsSourceSnapshotDto): TestsSourceSnapshot = dto.toEntity { testSuitesSourceRepository.getByIdOrNotFound(it) }

    override fun findByDto(
        dto: TestsSourceSnapshotDto
    ): TestsSourceSnapshot? = repository.findBySourceIdAndCommitId(
        sourceId = dto.sourceId,
        commitId = dto.commitId,
    )

    override fun beforeDelete(entity: TestsSourceSnapshot) {
        executionService.unlinkTestSuitesFromAllExecution(testSuitesService.getBySourceSnapshot(entity))
    }

    /**
     * @param testSuitesSource
     * @return list of [TestsSourceSnapshotDto] found by [testSuitesSource]
     */
    fun findAll(testSuitesSource: TestSuitesSource): List<TestsSourceSnapshotDto> = repository.findAllBySource(testSuitesSource).map { it.toDto() }
}
