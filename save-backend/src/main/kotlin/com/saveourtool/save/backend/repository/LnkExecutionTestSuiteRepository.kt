package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.LnkExecutionTestSuite
import com.saveourtool.common.entities.TestSuite
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository of [LnkExecutionTestSuite]
 */
@Repository
interface LnkExecutionTestSuiteRepository : BaseEntityRepository<LnkExecutionTestSuite> {
    /**
     * @param execution execution that is connected to testSuite
     * @return [LnkExecutionTestSuite] by [execution]
     */
    fun findByExecution(execution: Execution): List<LnkExecutionTestSuite>

    /**
     * @param executionId execution id execution that is connected to testSuite
     * @return [LnkExecutionTestSuite] by [executionId]
     */
    fun findByExecutionId(executionId: Long): List<LnkExecutionTestSuite>

    /**
     * @param executionIds list of ID of execution that is connected to testSuite
     * @return all [LnkExecutionTestSuite] by [Execution.id]
     */
    fun findByExecutionIdIn(executionIds: Collection<Long>): List<LnkExecutionTestSuite>

    /**
     * @param testSuiteId manageable test suite
     * @return [LnkExecutionTestSuite] by [testSuiteId]
     */
    fun findByTestSuiteId(testSuiteId: Long): List<LnkExecutionTestSuite>

    /**
     * @param testSuites list of manageable test suites
     * @return [LnkExecutionTestSuite] by [testSuites]
     */
    fun findAllByTestSuiteIn(testSuites: List<TestSuite>): List<LnkExecutionTestSuite>
}
