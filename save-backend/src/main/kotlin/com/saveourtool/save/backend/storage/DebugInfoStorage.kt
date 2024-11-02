package com.saveourtool.save.backend.storage

import com.saveourtool.common.domain.TestResultDebugInfo
import com.saveourtool.common.entities.TestExecution
import com.saveourtool.common.s3.S3Operations
import com.saveourtool.common.storage.AbstractSimpleReactiveStorage
import com.saveourtool.common.storage.DefaultStorageProjectReactor
import com.saveourtool.common.storage.concatS3Key
import com.saveourtool.common.storage.deleteUnexpectedKeys
import com.saveourtool.common.utils.*
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.service.TestExecutionService

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * A storage for storing additional data associated with test results
 */
@Service
class DebugInfoStorage(
    configProperties: ConfigProperties,
    private val s3Operations: S3Operations,
    private val objectMapper: ObjectMapper,
    private val testExecutionService: TestExecutionService,
    private val testExecutionRepository: TestExecutionRepository,
) : AbstractSimpleReactiveStorage<Long>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "debugInfo"),
) {
    /**
     * Init method to delete unexpected ids which are not associated to [com.saveourtool.save.entities.TestExecution]
     */
    override fun doInit(underlying: DefaultStorageProjectReactor<Long>): Mono<Unit> = Mono.fromFuture {
        s3Operations.deleteUnexpectedKeys(
            storageName = "${this::class.simpleName}",
            commonPrefix = s3KeyManager.commonPrefix,
        ) { s3Key ->
            testExecutionRepository.findById(s3Key.removePrefix(s3KeyManager.commonPrefix).toLong()).isEmpty
        }
    }.publishOn(s3Operations.scheduler)

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
            uploadAndReturnContentSize(testExecutionId, objectMapper.writeValueAsBytes(testResultDebugInfo))
        }

    override fun doBuildKeyFromSuffix(s3KeySuffix: String): Long = s3KeySuffix.toLong()
    override fun doBuildS3KeySuffix(key: Long): String = key.toString()

    companion object {
        private val log: Logger = getLogger<DebugInfoStorage>()
    }
}
