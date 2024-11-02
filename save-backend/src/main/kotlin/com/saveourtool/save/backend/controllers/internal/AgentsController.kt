package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.common.agent.*
import com.saveourtool.common.entities.*
import com.saveourtool.common.storage.impl.InternalFileKey
import com.saveourtool.common.test.TestDto
import com.saveourtool.common.utils.*
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.backend.service.AgentService
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.service.TestExecutionService
import com.saveourtool.save.backend.service.TestService
import com.saveourtool.save.backend.storage.BackendInternalFileStorage
import com.saveourtool.save.backend.storage.FileStorage
import com.saveourtool.save.backend.storage.TestsSourceSnapshotStorage

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import kotlinx.datetime.toJavaLocalDateTime

/**
 * Controller to manipulate with Agent related data
 */
@RestController
@RequestMapping("/internal")
@Suppress(
    "LongParameterList",
)
class AgentsController(
    private val agentStatusRepository: AgentStatusRepository,
    private val agentService: AgentService,
    private val configProperties: ConfigProperties,
    private val executionService: ExecutionService,
    private val testService: TestService,
    private val testExecutionService: TestExecutionService,
    private val fileStorage: FileStorage,
    private val internalFileStorage: BackendInternalFileStorage,
    private val testsSourceSnapshotStorage: TestsSourceSnapshotStorage,
) {
    /**
     * @param containerId [Agent.containerId]
     * @return [Mono] with [AgentInitConfig]
     */
    @GetMapping("/agents/get-init-config")
    fun getInitConfig(
        @RequestParam containerId: String,
    ): Mono<com.saveourtool.common.agent.AgentInitConfig> = getAgentByContainerIdAsMono(containerId)
        .map {
            agentService.getExecution(it)
        }
        .map { execution ->
            AgentInitConfig(
                saveCliUrl = internalFileStorage.generateRequiredUrlToDownloadFromContainer(InternalFileKey.saveCliKey(execution.saveCliVersion))
                    .toString(),
                testSuitesSourceSnapshotUrl = executionService.getRelatedTestsSourceSnapshot(execution.requiredId())
                    .let { testsSourceSnapshot ->
                        testsSourceSnapshotStorage.generateRequiredRequestToDownload(testsSourceSnapshot).urlFromContainer.toString()
                    },
                additionalFileNameToUrl = executionService.getAssignedFiles(execution)
                    .associate { file ->
                        file.name to fileStorage.generateRequiredRequestToDownload(file).urlFromContainer.toString()
                    },
                saveCliOverrides = SaveCliOverrides(
                    overrideExecCmd = execution.execCmd,
                    overrideExecFlags = null,
                    batchSize = execution.batchSizeForAnalyzer?.takeIf { it.isNotBlank() }?.toInt(),
                    batchSeparator = null,
                ),
            )
        }

    /**
     * @param containerId [Agent.containerId]
     * @return [Mono] with [AgentRunConfig]
     */
    @GetMapping("/agents/get-next-run-config")
    @Transactional
    fun getNextRunConfig(
        @RequestParam containerId: String,
    ): Mono<com.saveourtool.common.agent.AgentRunConfig> = getAgentByContainerIdAsMono(containerId)
        .map {
            agentService.getExecution(it)
        }
        .zipWhen { execution ->
            testService.getTestBatches(execution)
        }
        .filter { (_, testBatch) -> testBatch.isNotEmpty() }
        .map { (execution, testBatch) ->
            val backendUrl = configProperties.agentSettings.backendUrl

            testBatch to com.saveourtool.common.agent.AgentRunConfig(
                cliArgs = testBatch.constructCliCommand(),
                executionDataUploadUrl = "$backendUrl/internal/saveTestResult",
                debugInfoUploadUrl = "$backendUrl/internal/files/debug-info?executionId=${execution.requiredId()}"
            )
        }
        .asyncEffect { (testBatch, _) ->
            blockingToMono { testExecutionService.assignAgentByTest(containerId, testBatch) }
                .doOnSuccess {
                    log.trace { "Agent $containerId has been set as executor for tests $testBatch and its status has been set to BUSY" }
                }
        }
        .map { (_, runConfig) -> runConfig }

    /**
     * @param executionId ID of [Execution]
     * @param agent [AgentDto] to save into the DB
     * @return an ID, assigned to the agent
     */
    @PostMapping("/agents/insert")
    fun addAgent(
        @RequestParam executionId: Long,
        @RequestBody agent: AgentDto,
    ): Long {
        log.debug { "Saving agent $agent" }
        return agentService.save(executionId, agent.toEntity())
            .requiredId()
    }

    /**
     * @param agentStatus [AgentStatus] to update in the DB
     * @throws ResponseStatusException code 409 if agent has already its final state that shouldn't be updated
     */
    @PostMapping("/updateAgentStatus")
    @Transactional
    fun updateAgentStatus(@RequestBody agentStatus: AgentStatusDto) {
        val latestAgentStatus = agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(agentStatus.containerId)
        when (val latestState = latestAgentStatus?.state) {
            com.saveourtool.common.agent.AgentState.TERMINATED ->
                throw ResponseStatusException(HttpStatus.CONFLICT, "Agent ${agentStatus.containerId} has state $latestState and shouldn't be updated")
            agentStatus.state -> {
                // updating time
                latestAgentStatus.endTime = agentStatus.time.toJavaLocalDateTime()
                agentStatusRepository.save(latestAgentStatus)
            }
            else -> {
                // insert new agent status
                agentStatusRepository.save(agentStatus.toEntity { getAgentByContainerId(it) })
            }
        }
    }

    /**
     * Get statuses of all agents assigned to execution with provided ID.
     *
     * @param executionId ID of an execution.
     * @return list of agent statuses
     * @throws IllegalStateException if provided [executionId] is invalid.
     */
    @GetMapping("/getAgentStatusesByExecutionId")
    @Transactional
    fun findAllAgentStatusesByExecutionId(@RequestParam executionId: Long): List<AgentStatusDto> =
            agentService.getAgentsByExecutionId(executionId).map { agent ->
                val latestStatus = requireNotNull(
                    agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(agent.containerId)
                ) {
                    "AgentStatus not found for agent with containerId=${agent.containerId}"
                }
                latestStatus.toDto()
            }

    /**
     * Get statuses of agents identified by [containerIds].
     *
     * @param containerIds a list of containerIds agents.
     * @return list of agent statuses
     */
    @GetMapping("/agents/statuses")
    @Transactional
    fun findAgentStatuses(@RequestParam(name = "ids") containerIds: List<String>): List<AgentStatusDto> {
        val agents = containerIds.map {
            agentService.getAgentByContainerId(it)
        }
        val executionIds = agents.map { agentService.getExecution(it).requiredId() }.distinct()
        check(executionIds.size == 1) {
            "Statuses are requested for agents from different executions: containerIds=$containerIds, execution IDs are $executionIds"
        }
        return agents.map { agent ->
            val latestStatus = requireNotNull(
                agentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(agent.containerId)
            ) {
                "AgentStatus not found for agent with containerId=${agent.containerId}"
            }
            latestStatus.toDto()
        }
    }

    /**
     * Get agent by containerId.
     *
     * @param containerId containerId of an agent.
     * @return [Agent]
     */
    private fun getAgentByContainerId(containerId: String): Agent = agentService.getAgentByContainerId(containerId)

    private fun getAgentByContainerIdAsMono(containerId: String): Mono<Agent> = blockingToMono {
        getAgentByContainerId(containerId)
    }

    private fun List<TestDto>.constructCliCommand() = joinToString(" ") { it.filePath }
        .also {
            log.debug("Constructed cli args for SAVE-cli: $it")
        }

    companion object {
        private val log = LoggerFactory.getLogger(AgentsController::class.java)
    }
}
