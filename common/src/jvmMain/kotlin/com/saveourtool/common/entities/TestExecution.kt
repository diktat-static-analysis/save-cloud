package com.saveourtool.common.entities

import com.saveourtool.common.agent.TestExecutionDto
import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.spring.entity.BaseEntityWithDtoWithId

import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property test test
 * @property execution during which this TestExecution should take place.
 * @property agent which will execute this TestExecution. Agent id is nullable because agent isn't created at the time when test execution is created
 * @property status status of test execution
 * @property startTime start time
 * @property endTime finish time
 * @property unmatched number of unmatched checks/validations in test (false negative results)
 * @property matched number of matched checks/validations in test (true positive results)
 * @property expected number of all checks/validations in test (unmatched + matched)
 * @property unexpected number of matched,but not expected checks/validations in test (false positive results)
 */
@Entity
@Suppress("LongParameterList")
class TestExecution(

    @ManyToOne
    @JoinColumn(name = "test_id")
    var test: Test,

    @ManyToOne
    @JoinColumn(name = "execution_id")
    var execution: Execution,

    @ManyToOne
    @JoinColumn(name = "agent_id")
    var agent: Agent?,

    @Enumerated(EnumType.STRING)
    var status: TestResultStatus,

    var startTime: LocalDateTime?,

    var endTime: LocalDateTime?,

    var unmatched: Long?,

    var matched: Long?,

    var expected: Long?,

    var unexpected: Long?,

) : BaseEntityWithDtoWithId<com.saveourtool.common.agent.TestExecutionDto>() {
    /**
     * Converts `this` to [TestExecutionDto]
     *
     * @return a new [TestExecutionDto]
     */
    override fun toDto() = com.saveourtool.common.agent.TestExecutionDto(
        filePath = test.filePath,
        pluginName = test.pluginName,
        agentContainerId = agent?.containerId,
        agentContainerName = agent?.containerName,
        status = status,
        startTimeSeconds = startTime?.toEpochSecond(ZoneOffset.UTC),
        endTimeSeconds = endTime?.toEpochSecond(ZoneOffset.UTC),
        testSuiteName = test.testSuite.name,
        tags = test.testSuite.tagsAsList(),
        unmatched = unmatched,
        matched = matched,
        expected = expected,
        unexpected = unexpected,
        executionId = execution.requiredId(),
        id = requiredId(),
    )
}
