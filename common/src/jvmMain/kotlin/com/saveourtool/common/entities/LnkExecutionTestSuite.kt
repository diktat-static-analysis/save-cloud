package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDto
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property execution execution that is connected to [testSuite]
 * @property testSuite manageable test suite
 */
@Entity
class LnkExecutionTestSuite(
    @ManyToOne
    @JoinColumn(name = "execution_id")
    var execution: Execution,

    @ManyToOne
    @JoinColumn(name = "test_suite_id")
    var testSuite: TestSuite,
) : BaseEntityWithDto<LnkExecutionTestSuiteDto>() {
    override fun toDto() = LnkExecutionTestSuiteDto(
        execution.toDto(),
        testSuite.toDto(),
    )
}
