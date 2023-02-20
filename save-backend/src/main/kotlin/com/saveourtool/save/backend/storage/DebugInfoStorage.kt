package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.service.TestExecutionService
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.entities.TestExecution
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.AbstractSimpleStorage
import com.saveourtool.save.storage.AbstractSimpleStorageProjectReactor
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.deleteAsyncUnexpectedIds
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.switchIfEmptyToNotFound
import com.saveourtool.save.utils.upload

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * A storage for storing additional data associated with test results
 */
@Service
class DebugInfoStorage(
    configProperties: ConfigProperties,
    s3Operations: S3Operations,
    private val objectMapper: ObjectMapper,
    private val testExecutionService: TestExecutionService,
    private val testExecutionRepository: TestExecutionRepository,
) : AbstractSimpleStorage<Long>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "debugInfo"),
) {
    /**
     * Init method to delete unexpected ids which are not associated to [com.saveourtool.save.entities.TestExecution]
     *
     * @param storageProjectReactor
     */
    override fun doInitAsync(storageProjectReactor: AbstractSimpleStorageProjectReactor<Long>): Mono<Unit> = storageProjectReactor.deleteAsyncUnexpectedIds(testExecutionRepository,
        log)

    /**
     * Store provided [testResultDebugInfo] associated with [TestExecution.id]
     *
     * @param executionId
     * @param testResultDebugInfo
     * @return count of saved bytes
     */
    fun upload(
        executionId: Long,
        testResultDebugInfo: TestResultDebugInfo,
    ): Mono<Long> = blockingToMono { testExecutionService.getTestExecution(executionId, testResultDebugInfo.testResultLocation)?.requiredId() }
        .switchIfEmptyToNotFound {
            "Not found ${TestExecution::class.simpleName} by executionId $executionId and testResultLocation: ${testResultDebugInfo.testResultLocation}"
        }
        .flatMap { testExecutionId ->
            log.debug { "Writing debug info for $testExecutionId" }
            usingProjectReactor().upload(testExecutionId, objectMapper.writeValueAsBytes(testResultDebugInfo))
        }

    override fun buildKey(s3KeySuffix: String): Long = s3KeySuffix.toLong()
    override fun buildS3KeySuffix(key: Long): String = key.toString()
}
