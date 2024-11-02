package com.saveourtool.common.entities

import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.testsuite.TestSuiteDto
import kotlinx.serialization.Serializable

/**
 * @property execution
 * @property testSuite
 */
@Serializable
data class LnkExecutionTestSuiteDto(
    val execution: ExecutionDto,
    val testSuite: TestSuiteDto,
)
