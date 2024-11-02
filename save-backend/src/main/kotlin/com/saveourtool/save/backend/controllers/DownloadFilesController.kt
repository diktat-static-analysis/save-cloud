package com.saveourtool.save.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.domain.*
import com.saveourtool.common.entities.TestExecution
import com.saveourtool.common.utils.*
import com.saveourtool.common.v1
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.*

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer

/**
 * A Spring controller for file downloading
 */
@RestController
@ApiSwaggerSupport
@Tags(
    Tag(name = "files"),
)
@Suppress("LongParameterList")
class DownloadFilesController(
    private val debugInfoStorage: DebugInfoStorage,
    private val executionInfoStorage: ExecutionInfoStorage,
) {
    /**
     * @param testExecutionId [com.saveourtool.save.entities.TestExecution.id]
     * @return [Mono] with content of DebugInfo
     * @throws ResponseStatusException if request is invalid or result cannot be returned
     */
    @Suppress("ThrowsCount", "UnsafeCallOnNullableType")
    @GetMapping(path = ["/api/$v1/files/get-debug-info"])
    fun getDebugInfo(
        @RequestParam testExecutionId: Long,
    ): Flux<ByteBuffer> = debugInfoStorage.download(testExecutionId)
        .switchIfEmptyToNotFound {
            logger.warn("Additional file for ${TestExecution::class.simpleName} with id $testExecutionId not found")
            "File not found"
        }

    /**
     * @param executionId [com.saveourtool.save.entities.Execution.id]
     * @return [Mono] with response
     * @throws ResponseStatusException if request is invalid or result cannot be returned
     */
    @Suppress("ThrowsCount", "UnsafeCallOnNullableType")
    @GetMapping(path = ["/api/${com.saveourtool.common.v1}/files/get-execution-info"])
    fun getExecutionInfo(
        @RequestParam executionId: Long,
    ): Flux<ByteBuffer> = executionInfoStorage.download(executionId)
        .switchIfEmptyToNotFound {
            logger.debug("ExecutionInfo for $executionId not found")
            "File not found"
        }

    companion object {
        private val logger = LoggerFactory.getLogger(DownloadFilesController::class.java)
    }
}
