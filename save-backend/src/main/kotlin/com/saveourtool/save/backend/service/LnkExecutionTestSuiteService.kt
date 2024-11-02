package com.saveourtool.save.backend.service

import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.LnkExecutionTestSuite
import com.saveourtool.common.entities.TestSuite
import com.saveourtool.save.backend.repository.LnkExecutionTestSuiteRepository

import org.springframework.stereotype.Service

/**
 * Service of [LnkExecutionTestSuite]
 */
@Service
class LnkExecutionTestSuiteService(
    private val lnkExecutionTestSuiteRepository: LnkExecutionTestSuiteRepository,
) {
    /**
     * @param execution execution that is connected to testSuite
     * @return all [TestSuite]s with [execution]
     */
    fun getAllTestSuitesByExecution(execution: Execution) =
            lnkExecutionTestSuiteRepository.findByExecution(execution)
                .map {
                    it.testSuite
                }

    /**
     * @param executionId execution id that is connected to testSuite
     * @return all [TestSuite]s with [executionId]
     */
    fun getAllTestSuiteIdsByExecutionId(executionId: Long) =
            lnkExecutionTestSuiteRepository.findByExecutionId(executionId)
                .map {
                    it.testSuite.requiredId()
                }

    /**
     * @param executionIds IDs of manageable executions
     */
    fun deleteByExecutionIds(executionIds: Collection<Long>) {
        lnkExecutionTestSuiteRepository.deleteAll(lnkExecutionTestSuiteRepository.findByExecutionIdIn(executionIds))
    }

    /**
     * @param lnkExecutionTestSuites link execution to testSuites
     */
    fun saveAll(lnkExecutionTestSuites: List<LnkExecutionTestSuite>): List<LnkExecutionTestSuite> = lnkExecutionTestSuiteRepository.saveAll(lnkExecutionTestSuites)
}
