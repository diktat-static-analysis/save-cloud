package com.saveourtool.common.entities

import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.common.testsuite.TestSuiteDto
import kotlinx.serialization.Serializable

/**
 * @property contest
 * @property testSuite
 */
@Serializable
data class LnkContestTestSuiteDto(
    val contest: ContestDto,
    val testSuite: TestSuiteDto,
)
