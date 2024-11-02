package com.saveourtool.save.frontend.components.basic

import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.execution.ExecutionStatus
import com.saveourtool.common.execution.TestingType
import kotlin.test.Test
import kotlin.test.assertEquals

class ExecutionStatisticsValuesTest {
    @Test
    fun nullExecution() {
        val executionStatisticsValues = ExecutionStatisticsValues(null)
        assertEquals("secondary", executionStatisticsValues.style)
        assertEquals("0", executionStatisticsValues.allTests)
        assertEquals("0", executionStatisticsValues.passedTests)
        assertEquals("0", executionStatisticsValues.failedTests)
        assertEquals("0", executionStatisticsValues.runningTests)
        assertEquals("0", executionStatisticsValues.passRate)
        assertEquals("0", executionStatisticsValues.precisionRate)
        assertEquals("0", executionStatisticsValues.recallRate)
    }

    @Test
    fun notNullExecution() {
        val executionDto = ExecutionDto(
            id = -1,
            status = ExecutionStatus.RUNNING,
            type = TestingType.PRIVATE_TESTS,
            version = "N/A",
            startTime = 0L,
            endTime = null,
            allTests = 10,
            runningTests = 5,
            passedTests = 3,
            failedTests = 1,
            skippedTests = 1,
            unmatchedChecks = 10,
            matchedChecks = 15,
            expectedChecks = 25,
            unexpectedChecks = 5,
            testSuiteSourceName = "",
            score = null,
        )
        val executionStatisticsValues = ExecutionStatisticsValues(executionDto)
        assertEquals("danger", executionStatisticsValues.style)
        assertEquals("10", executionStatisticsValues.allTests)
        assertEquals("3", executionStatisticsValues.passedTests)
        assertEquals("1", executionStatisticsValues.failedTests)
        assertEquals("5", executionStatisticsValues.runningTests)
        assertEquals("30", executionStatisticsValues.passRate)
        assertEquals("75", executionStatisticsValues.precisionRate)
        assertEquals("60", executionStatisticsValues.recallRate)
    }
}
