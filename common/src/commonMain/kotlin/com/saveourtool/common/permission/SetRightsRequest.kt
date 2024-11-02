package com.saveourtool.common.permission

import kotlinx.serialization.Serializable

/**
 * @property organizationName organization that will gain or loose rights
 * @property rights new rights
 * @property testSuiteIds list of test suite ids to perform mass operations
 */
@Serializable
data class SetRightsRequest(
    val organizationName: String,
    val rights: Rights,
    val testSuiteIds: List<Long> = emptyList()
)
