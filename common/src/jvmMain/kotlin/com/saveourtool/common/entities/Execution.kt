package com.saveourtool.common.entities

import com.saveourtool.common.domain.Sdk
import com.saveourtool.common.domain.toSdk
import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.common.execution.TestingType
import com.saveourtool.common.request.RunExecutionRequest
import com.saveourtool.common.spring.entity.BaseEntity
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property project
 * @property startTime
 * @property endTime If the state is RUNNING we are not considering it, so it can never be null
 * @property status
 * @property batchSize Maximum number of returning tests per execution
 * @property type
 * @property version
 * @property allTests
 * @property runningTests
 * @property passedTests
 * @property failedTests
 * @property skippedTests
 * @property unmatchedChecks
 * @property matchedChecks
 * @property expectedChecks
 * @property unexpectedChecks
 * @property sdk
 * @property saveCliVersion
 * @property user user that has started this execution
 * @property execCmd
 * @property batchSizeForAnalyzer
 * @property testSuiteSourceName
 * @property score a rating of this execution. Specific meaning may vary depending on [type]
 */
@Suppress("LongParameterList")
@Entity
class Execution(

    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project,

    var startTime: LocalDateTime,

    var endTime: LocalDateTime?,

    @Enumerated(EnumType.STRING)
    var status: ExecutionStatus,

    var batchSize: Int?,

    @Enumerated(EnumType.STRING)
    var type: TestingType,

    var version: String?,

    var allTests: Long,

    var runningTests: Long,

    var passedTests: Long,

    var failedTests: Long,

    var skippedTests: Long,

    var unmatchedChecks: Long,

    var matchedChecks: Long,

    var expectedChecks: Long,

    var unexpectedChecks: Long,

    var sdk: String,

    var saveCliVersion: String,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User?,

    var execCmd: String?,

    var batchSizeForAnalyzer: String?,

    var testSuiteSourceName: String?,

    var score: Double?,

) : BaseEntity() {
    /**
     * @return Execution dto
     */
    @Suppress("UnsafeCallOnNullableType")
    fun toDto() = ExecutionDto(
        id = id!!,
        status = status,
        type = type,
        version = version,
        startTime = startTime.toEpochSecond(ZoneOffset.UTC),
        endTime = endTime?.toEpochSecond(ZoneOffset.UTC),
        allTests = allTests,
        runningTests = runningTests,
        passedTests = passedTests,
        failedTests = failedTests,
        skippedTests = skippedTests,
        unmatchedChecks = unmatchedChecks,
        matchedChecks = matchedChecks,
        expectedChecks = expectedChecks,
        unexpectedChecks = unexpectedChecks,
        testSuiteSourceName = testSuiteSourceName,
        score = score,
    )

    /**
     * @param saveAgentUrl an url to download save-agent
     * @return [RunExecutionRequest] created from current entity
     */
    fun toRunRequest(
        saveAgentUrl: URL,
    ): RunExecutionRequest {
        require(status == ExecutionStatus.PENDING) {
            "${RunExecutionRequest::class.simpleName} can be created only for ${Execution::class.simpleName} with status = ${ExecutionStatus.PENDING}"
        }
        return RunExecutionRequest(
            executionId = requiredId(),
            sdk = sdk.toSdk(),
            saveAgentUrl = saveAgentUrl.toString(),
        )
    }

    companion object {
        /**
         * Create a stub for testing. Since all fields are mutable, only required ones can be set after calling this method.
         *
         * @param project project instance
         * @return a execution
         */
        fun stub(project: Project) = Execution(
            project = project,
            startTime = LocalDateTime.now(),
            endTime = null,
            status = ExecutionStatus.RUNNING,
            batchSize = 20,
            type = TestingType.PUBLIC_TESTS,
            version = null,
            allTests = 0,
            runningTests = 0,
            passedTests = 0,
            failedTests = 0,
            skippedTests = 0,
            unmatchedChecks = 0,
            matchedChecks = 0,
            expectedChecks = 0,
            unexpectedChecks = 0,
            sdk = Sdk.Default.toString(),
            saveCliVersion = "N/A",
            user = null,
            execCmd = null,
            batchSizeForAnalyzer = null,
            testSuiteSourceName = "",
            score = null,
        )
    }
}
