package com.saveourtool.common.entities.contest

import com.saveourtool.common.testsuite.TestSuiteVersioned
import com.saveourtool.common.validation.Validatable
import com.saveourtool.common.validation.isValidName

import kotlin.js.JsExport
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Data class of contest information
 *
 * @property name contest name
 * @property description info about contest
 * @property startTime start time of a contest
 * @property endTime end time of a contest
 * @property organizationName
 * @property testSuites
 * @property creationTime
 * @property status
 */
@Serializable
@JsExport
data class ContestDto(
    val name: String,
    val status: ContestStatus,
    @Contextual
    val startTime: LocalDateTime?,
    @Contextual
    val endTime: LocalDateTime?,
    val description: String?,
    val organizationName: String,
    val testSuites: List<TestSuiteVersioned>,
    @Contextual
    val creationTime: LocalDateTime?,
) : Validatable {
    override fun validate(): Boolean = name.isValidName()

    companion object {
        val empty = ContestDto(
            "",
            ContestStatus.CREATED,
            null,
            null,
            null,
            "",
            emptyList(),
            null,
        )
    }
}
