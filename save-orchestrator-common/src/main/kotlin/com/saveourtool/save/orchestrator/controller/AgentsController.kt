package com.saveourtool.save.orchestrator.controller

import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.common.request.RunExecutionRequest
import com.saveourtool.common.utils.EmptyResponse
import com.saveourtool.common.utils.info
import com.saveourtool.save.orchestrator.runner.ContainerRunnerException
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.ContainerService

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

/**
 * Controller used to start agents with needed information
 */
@RestController
class AgentsController(
    private val agentService: AgentService,
    private val containerService: ContainerService,
) {
    /**
     * Schedules tasks to build base images, create a number of containers and put their data into the database.
     *
     * @param request a request to run execution
     * @return OK if everything went fine.
     * @throws ResponseStatusException
     */
    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "UnsafeCallOnNullableType")
    @PostMapping("/initializeAgents")
    fun initialize(@RequestBody request: RunExecutionRequest): Mono<EmptyResponse> {
        val response = agentService.updateExecution(request.executionId, ExecutionStatus.INITIALIZATION)
            .thenReturn(ResponseEntity<Void>(HttpStatus.ACCEPTED))
            .subscribeOn(agentService.scheduler)
        return response.doOnSuccess {
            log.info {
                "Starting preparations for launching execution id=${request.executionId}"
            }
            Mono.fromCallable {
                containerService.prepareConfiguration(request)
            }
                .subscribeOn(agentService.scheduler)
                .map { configuration ->
                    containerService.createAndStartContainers(request.executionId, configuration)
                }
                .onErrorResume(ContainerRunnerException::class.java) { ex ->
                    reportExecutionError(request.executionId, ex)
                }
                .flatMap {
                    log.info("Sending request to make execution.id=${request.executionId} RUNNING")
                    agentService.updateExecution(request.executionId, ExecutionStatus.RUNNING)
                }
                .flatMap {
                    containerService.validateContainersAreStarted(request.executionId)
                }
                .subscribe()
        }
    }

    private fun <T> reportExecutionError(
        executionId: Long,
        ex: ContainerRunnerException,
    ): Mono<T> {
        log.error("${ex.message} for executionId=$executionId, will mark it as ERROR", ex)
        return agentService.updateExecution(executionId, ExecutionStatus.ERROR, ex.message)
            .then(Mono.empty())
    }

    /**
     * Delete containers and images associated with execution [executionId]
     *
     * @param executionId id of execution
     * @return empty response
     */
    @PostMapping("/cleanup")
    fun cleanup(@RequestParam executionId: Long): Mono<EmptyResponse> = Mono.fromCallable {
        containerService.cleanupAllByExecution(executionId)
    }
        .flatMap {
            Mono.just(ResponseEntity.ok().build())
        }

    companion object {
        private val log = LoggerFactory.getLogger(AgentsController::class.java)
    }
}
