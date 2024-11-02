package com.saveourtool.save.backend.service

import com.saveourtool.common.domain.*
import com.saveourtool.common.entities.*
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.common.execution.TestingType
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.service.ProjectService
import com.saveourtool.common.test.TestsSourceSnapshotDto
import com.saveourtool.common.utils.*
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.utils.ErrorMessage
import com.saveourtool.save.backend.utils.getOrThrowBadRequest

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import generated.SAVE_CLI_VERSION
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

import java.time.LocalDateTime
import java.util.Optional

/**
 * Service that is used to manipulate executions
 */
@Suppress("LongParameterList")
@Service
class ExecutionService(
    private val executionRepository: ExecutionRepository,
    private val projectService: ProjectService,
    private val userRepository: UserRepository,
    private val testRepository: TestRepository,
    private val testExecutionRepository: TestExecutionRepository,
    @Lazy private val testSuitesService: TestSuitesService,
    private val configProperties: ConfigProperties,
    private val lnkContestProjectService: LnkContestProjectService,
    private val lnkContestExecutionService: LnkContestExecutionService,
    private val lnkExecutionTestSuiteService: LnkExecutionTestSuiteService,
    private val fileRepository: FileRepository,
    private val lnkExecutionFileRepository: LnkExecutionFileRepository,
    private val lnkExecutionTestSuiteRepository: LnkExecutionTestSuiteRepository,
    @Lazy private val agentService: AgentService,
    private val agentStatusService: AgentStatusService,
    private val testAnalysisService: TestAnalysisService,
    @Lazy private val testsSourceVersionService: TestsSourceVersionService,
) {
    private val log = LoggerFactory.getLogger(ExecutionService::class.java)

    /**
     * Find execution by id
     *
     * @param id id of execution
     * @return execution if it has been found
     */
    fun findExecution(id: Long): Execution? = executionRepository.findByIdOrNull(id)

    /**
     * Get execution by id
     *
     * @param id id of [Execution]
     * @return [Execution] or exception
     */
    fun getExecution(id: Long): Execution = findExecution(id).orNotFound {
        "Not found execution with id $id"
    }

    /**
     * @param execution execution that is connected to testSuite
     * @param testSuites list of manageable [TestSuite]
     * @param files list of manageable [File]
     */
    @Transactional
    fun save(execution: Execution, testSuites: Collection<TestSuite>, files: Collection<File> = emptyList()): Execution {
        val newExecution = executionRepository.save(execution)
        testSuites.map { LnkExecutionTestSuite(newExecution, it) }.let { lnkExecutionTestSuiteService.saveAll(it) }
        files.map { LnkExecutionFile(newExecution, it) }.let { lnkExecutionFileRepository.saveAll(it) }
        return newExecution
    }

    /**
     * @param srcExecution [Execution]
     * @param newStatus [Execution.status]
     * @throws ResponseStatusException
     */
    @Transactional
    fun updateExecutionStatus(srcExecution: Execution, newStatus: ExecutionStatus) {
        val executionId = srcExecution.requiredId()
        log.debug("Updating status to $newStatus on execution id = $executionId")
        if (newStatus == ExecutionStatus.OBSOLETE) {
            log.info {
                "Marking execution with id $executionId as obsolete. Additionally cleanup dependencies to this execution"
            }
            doDeleteDependencies(listOf(executionId))
        }
        val execution = executionRepository.findWithLockingById(executionId).orNotFound()
        val updatedExecution = execution.apply {
            status = newStatus
        }
        if (updatedExecution.status == ExecutionStatus.FINISHED || updatedExecution.status == ExecutionStatus.ERROR) {
            // execution is completed, we can update end time
            updatedExecution.endTime = LocalDateTime.now()

            if (execution.type == TestingType.CONTEST_MODE && updatedExecution.status == ExecutionStatus.FINISHED) {
                // maybe this execution is the new best execution under a certain contest
                lnkContestProjectService.updateBestExecution(execution)
            }

            // if the tests are stuck in the READY_FOR_TESTING or RUNNING status
            testExecutionRepository.findByStatusListAndExecutionId(listOf(TestResultStatus.READY_FOR_TESTING, TestResultStatus.RUNNING), execution.requiredId()).map { testExec ->
                log.debug {
                    "Test execution id=${testExec.id} has status ${testExec.status} while execution id=${updatedExecution.id} has status ${updatedExecution.status}. " +
                            "Will mark it ${TestResultStatus.INTERNAL_ERROR}"
                }
                testExec.status = TestResultStatus.INTERNAL_ERROR
                testExecutionRepository.save(testExec)
            }
        }
        executionRepository.save(updatedExecution)

        /*
         * Test analysis: update the in-memory statistics.
         */
        if (updatedExecution.status == ExecutionStatus.FINISHED) {
            testAnalysisService.updateStatistics(updatedExecution)
        }
    }

    /**
     * @param name name of project
     * @param organization organization of project
     * @return list of execution
     */
    fun getExecutionByNameAndOrganization(name: String, organization: Organization) =
            executionRepository.getAllByProjectNameAndProjectOrganization(name, organization)

    /**
     * @param name name of project
     * @param organization organization of project
     * @param start start date
     * @param end end date
     * @return list of executions
     */
    fun getExecutionByNameAndOrganizationAndStartTimeBetween(
        name: String,
        organization: Organization,
        start: LocalDateTime,
        end: LocalDateTime
    ) =
            executionRepository.findByProjectNameAndProjectOrganizationAndStartTimeBetween(name, organization, start, end)

    /**
     * Get latest (by start time an) execution by project name and organization
     *
     * @param name name of project
     * @param organizationName name of organization of project
     * @return execution or null if it was not found
     */
    fun getLatestExecutionByProjectNameAndProjectOrganizationName(name: String, organizationName: String): Optional<Execution> =
            executionRepository.findTopByProjectNameAndProjectOrganizationNameOrderByStartTimeDesc(name, organizationName)

    /**
     * Delete executions, except participating in contests, by project name and organization
     *
     * @param name name of project
     * @param organization organization of project
     * @return Unit
     */
    @Suppress("IDENTIFIER_LENGTH")
    @Transactional
    fun deleteExecutionExceptParticipatingInContestsByProjectNameAndProjectOrganization(name: String, organization: Organization) {
        executionRepository.getAllByProjectNameAndProjectOrganization(name, organization)
            .filter {
                lnkContestExecutionService.findContestByExecution(it) == null
            }
            .map { it.requiredId() }
            .let { deleteByIds(it) }
    }

    /**
     * @param projectCoordinates
     * @param testSuiteIds
     * @param testsVersion
     * @param fileIds
     * @param username
     * @param sdk
     * @param execCmd
     * @param batchSizeForAnalyzer
     * @param testingType
     * @param contestName
     * @return new [Execution] with provided values
     */
    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
    @Transactional
    fun createNew(
        projectCoordinates: ProjectCoordinates,
        testSuiteIds: List<Long>,
        testsVersion: String?,
        fileIds: List<Long>,
        username: String,
        sdk: Sdk,
        execCmd: String?,
        batchSizeForAnalyzer: String?,
        testingType: TestingType,
        contestName: String?,
    ): Execution {
        val project = with(projectCoordinates) {
            projectService.findByNameAndOrganizationNameAndCreatedStatus(projectName, organizationName).orNotFound {
                "Not found project $projectName in $organizationName"
            }
        }
        val testSuites = testSuiteIds.map { testSuitesService.getById(it) }
        val snapshot = testSuites.singleSnapshot().getOrThrowBadRequest()
        return doCreateNew(
            project = project,
            testSuites = testSuites,
            testsVersion = testsVersion?.validateTestsVersion(snapshot) ?: snapshot.commitId,
            testSuiteSourceName = snapshot.source.name,
            allTests = testSuiteIds.flatMap { testRepository.findAllByTestSuiteId(it) }
                .count()
                .toLong(),
            files = fileIds.map { fileRepository.getByIdOrNotFound(it) },
            username = username,
            sdk = sdk.toString(),
            execCmd = execCmd,
            batchSizeForAnalyzer = batchSizeForAnalyzer,
            testingType = testingType,
            contestName,
        )
    }

    /**
     * @param execution
     * @param username
     * @return new [Execution] with values taken from [execution]
     */
    @Transactional
    fun createNewCopy(
        execution: Execution,
        username: String,
    ): Execution {
        val testSuites = lnkExecutionTestSuiteService.getAllTestSuitesByExecution(execution)
        val files = lnkExecutionFileRepository.findAllByExecution(execution).map { it.file }
        return doCreateNew(
            project = execution.project,
            testSuites = testSuites,
            testsVersion = execution.version,
            testSuiteSourceName = execution.testSuiteSourceName,
            allTests = execution.allTests,
            files = files,
            username = username,
            sdk = execution.sdk,
            execCmd = execution.execCmd,
            batchSizeForAnalyzer = execution.batchSizeForAnalyzer,
            testingType = execution.type,
            contestName = lnkContestExecutionService.takeIf { execution.type == TestingType.CONTEST_MODE }
                ?.findContestByExecution(execution)?.name,
        )
    }

    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS", "UnsafeCallOnNullableType")
    private fun doCreateNew(
        project: Project,
        testSuites: List<TestSuite>,
        testsVersion: String?,
        testSuiteSourceName: String?,
        allTests: Long,
        files: List<File>,
        username: String,
        sdk: String,
        execCmd: String?,
        batchSizeForAnalyzer: String?,
        testingType: TestingType,
        contestName: String?,
    ): Execution {
        val user = userRepository.findByName(username).orNotFound {
            "Not found user $username"
        }
        val execution = Execution(
            project = project,
            startTime = LocalDateTime.now(),
            endTime = null,
            status = ExecutionStatus.PENDING,
            batchSize = configProperties.initialBatchSize,
            type = testingType,
            version = testsVersion,
            allTests = allTests,
            runningTests = 0,
            passedTests = 0,
            failedTests = 0,
            skippedTests = 0,
            unmatchedChecks = 0,
            matchedChecks = 0,
            expectedChecks = 0,
            unexpectedChecks = 0,
            sdk = sdk,
            saveCliVersion = SAVE_CLI_VERSION,
            user = user,
            execCmd = execCmd,
            batchSizeForAnalyzer = batchSizeForAnalyzer,
            testSuiteSourceName = testSuiteSourceName,
            score = null,
        )

        val savedExecution = executionRepository.save(execution)
        testSuites.map { LnkExecutionTestSuite(savedExecution, it) }.let { lnkExecutionTestSuiteService.saveAll(it) }
        files.map { LnkExecutionFile(savedExecution, it) }.let { lnkExecutionFileRepository.saveAll(it) }
        if (testingType == TestingType.CONTEST_MODE) {
            lnkContestExecutionService.createLink(
                savedExecution, requireNotNull(contestName) {
                    "Requested execution type is ${TestingType.CONTEST_MODE} but no contest name has been specified"
                }
            )
        }
        log.info("Created a new execution id=${savedExecution.id} for project id=${project.id}")
        return savedExecution
    }

    private fun String.validateTestsVersion(snapshot: TestsSourceSnapshot): String {
        testsSourceVersionService.doesVersionExist(snapshot.source.requiredId(), this)
            .takeIf { it }
            .orResponseStatusException(HttpStatus.BAD_REQUEST) {
                "Provided tests version $this doesn't belong to tests snapshot $snapshot"
            }
        return this
    }

    /**
     * Unlink provided [File] from all [Execution]s
     *
     * @param file
     */
    @Transactional
    fun unlinkFileFromAllExecution(file: File) {
        lnkExecutionFileRepository.findAllByFile(file)
            .also {
                lnkExecutionFileRepository.deleteAll(it)
            }
            .map { it.execution }
            .forEach {
                updateExecutionStatus(it, ExecutionStatus.OBSOLETE)
            }
    }

    /**
     * Unlink provided list of [TestSuite]s from all [Execution]s
     *
     * @param testSuites
     */
    @Transactional
    fun unlinkTestSuitesFromAllExecution(testSuites: List<TestSuite>) {
        lnkExecutionTestSuiteRepository.findAllByTestSuiteIn(testSuites)
            .also { links ->
                val linksByExecutions = lnkExecutionTestSuiteRepository.findByExecutionIdIn(links.map { it.execution.requiredId() })
                require(
                    linksByExecutions.isEmpty() || linksByExecutions.size == links.size
                ) {
                    "Expected that we remove all test suites related to a single execution at once"
                }
                lnkExecutionTestSuiteRepository.deleteAll(links)
            }
            .map { it.execution }
            .forEach {
                updateExecutionStatus(it, ExecutionStatus.OBSOLETE)
            }
    }

    /**
     * @param execution
     * @return all [FileDto]s are assigned to provided [Execution]
     */
    fun getAssignedFiles(execution: Execution): List<FileDto> = lnkExecutionFileRepository.findAllByExecution(execution)
        .map { it.file.toDto() }

    /**
     * @param executionId
     * @return a single [TestsSourceSnapshotDto] (with validation) from [TestSuite]s which are assigned to [Execution] (by provided [executionId])
     */
    fun getRelatedTestsSourceSnapshot(executionId: Long): TestsSourceSnapshotDto = lnkExecutionTestSuiteRepository.findByExecutionId(executionId)
        .map { it.testSuite }
        .singleSnapshot()
        .getOrThrowBadRequest()
        .toDto()

    /**
     * Delete [Execution] and links to TestSuite, TestExecution and related Agent with AgentStatus
     *
     * @param executionIds
     */
    @Transactional
    fun deleteByIds(executionIds: List<Long>) {
        log.info {
            "Deleting executions with id in $executionIds. Additionally cleanup dependencies to these executions"
        }
        // dependencies will be cleanup by cascade constrains
        executionRepository.deleteAllById(executionIds)
    }

    private fun doDeleteDependencies(executionIds: List<Long>) {
        log.info {
            "Delete dependencies to executions ($executionIds): links to test suites and files, agents with their statuses and test executions"
        }
        lnkExecutionTestSuiteService.deleteByExecutionIds(executionIds)
        lnkExecutionFileRepository.deleteAllByExecutionIdIn(executionIds)
        testExecutionRepository.deleteByExecutionIdIn(executionIds)
        agentStatusService.deleteAgentStatusWithExecutionIds(executionIds)
        agentService.deleteAgentByExecutionIds(executionIds)
    }

    companion object {
        private fun Collection<TestSuite>.singleSnapshot(): Either<ErrorMessage, TestsSourceSnapshot> = map { it.sourceSnapshot }
            .distinct()
            .let { snapshots ->
                when (snapshots.size) {
                    1 -> snapshots[0]
                        .right()

                    else -> ErrorMessage("Only a single tests snapshot is supported, but got: $snapshots")
                        .left()
                }
            }
    }
}
