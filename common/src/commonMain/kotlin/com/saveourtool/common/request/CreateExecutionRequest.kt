package com.saveourtool.common.request

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.domain.Sdk
import com.saveourtool.common.execution.TestingType
import kotlinx.serialization.Serializable

/**
 * @property projectCoordinates project coordinates for evaluated tool
 * @property testSuiteIds selected test suites for running
 * @property fileIds selected files of evaluated tool
 * @property sdk
 * @property execCmd
 * @property batchSizeForAnalyzer
 * @property testingType a [TestingType] for this execution
 * @property contestName if [testingType] is [TestingType.CONTEST_MODE], then this property contains name of the associated contest
 * @property testsVersion version of selected test suites (if it's missed, it will be calculated as commitId)
 */
@Serializable
data class CreateExecutionRequest(
    val projectCoordinates: ProjectCoordinates,

    val testSuiteIds: List<Long>,
    val fileIds: List<Long>,

    val sdk: Sdk,
    val execCmd: String? = null,
    val batchSizeForAnalyzer: String? = null,

    val testingType: TestingType,
    val contestName: String? = null,

    val testsVersion: String? = null,
) {
    init {
        require((testingType == TestingType.CONTEST_MODE) xor (contestName == null)) {
            "RunExecutionRequest.contestName shouldn't be set unless testingType is ${TestingType.CONTEST_MODE}"
        }
    }
}
