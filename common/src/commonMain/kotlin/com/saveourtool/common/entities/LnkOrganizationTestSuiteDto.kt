package com.saveourtool.common.entities

import com.saveourtool.common.permission.Rights
import com.saveourtool.common.testsuite.TestSuiteDto
import kotlinx.serialization.Serializable

/**
 * @property organization organization that is connected to [testSuite]
 * @property testSuite manageable test suite
 * @property rights [Rights] that [organization] has over [testSuite]
 */
@Serializable
data class LnkOrganizationTestSuiteDto(
    val organization: OrganizationDto,
    val testSuite: TestSuiteDto,
    val rights: Rights,
)
