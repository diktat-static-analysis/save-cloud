package com.saveourtool.common.execution

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property status
 * @property type
 * @property version
 * @property startTime
 * @property endTime
 * @property allTests
 * @property runningTests
 * @property passedTests
 * @property failedTests
 * @property skippedTests
 * @property unmatchedChecks
 * @property matchedChecks
 * @property expectedChecks
 * @property unexpectedChecks
 * @property testSuiteSourceName
 * @property score see [Execution.score]
 */
@Serializable
@Suppress("LongParameterList")
data class ExecutionDto(
    val id: Long,
    val status: ExecutionStatus,
    val type: TestingType,
    val version: String?,
    val startTime: Long,
    val endTime: Long?,
    val allTests: Long,
    val runningTests: Long,
    val passedTests: Long,
    val failedTests: Long,
    val skippedTests: Long,
    val unmatchedChecks: Long,
    val matchedChecks: Long,
    val expectedChecks: Long,
    val unexpectedChecks: Long,
    val testSuiteSourceName: String?,
    val score: Double?,
) {
    companion object {
        val empty = ExecutionDto(
            id = -1,
            status = ExecutionStatus.PENDING,
            type = TestingType.PUBLIC_TESTS,
            version = null,
            startTime = -1,
            endTime = null,
            allTests = 0,
            runningTests = 0,
            passedTests = 0,
            failedTests = 0,
            skippedTests = 0,
            unmatchedChecks = 0,
            matchedChecks = 0,
            expectedChecks = 0,
            unexpectedChecks = 0,
            testSuiteSourceName = "",
            score = null,
        )
    }
}
