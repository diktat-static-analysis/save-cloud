package com.saveourtool.common.entities.contest

import com.saveourtool.common.execution.ExecutionStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Represents all data related to contest result
 * @property projectName name of a project
 * @property organizationName name of an organization in which given project is
 * @property score that project got in contest
 * @property contestName
 * @property submissionTime
 * @property submissionStatus
 * @property sdk
 * @property hasFailedTest
 */
@Serializable
data class ContestResult(
    val projectName: String,
    val organizationName: String,
    val contestName: String,
    val score: Double? = null,
    @Contextual
    val submissionTime: LocalDateTime? = null,
    val submissionStatus: ExecutionStatus = ExecutionStatus.PENDING,
    val sdk: String = "",
    val hasFailedTest: Boolean = false,
)
