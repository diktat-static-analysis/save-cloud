package com.saveourtool.common.entities

import com.saveourtool.common.entities.contest.ContestResult
import com.saveourtool.common.spring.entity.BaseEntity

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * Entity that represents a link between [Contest] and [Execution]
 *
 * @property contest
 * @property execution
 */
@Entity
class LnkContestExecution(
    @OneToOne
    @JoinColumn(name = "execution_id")
    var execution: Execution,

    @ManyToOne
    @JoinColumn(name = "contest_id")
    var contest: Contest,

) : BaseEntity() {
    /**
     * @return [ContestResult] from [LnkContestExecution]
     */
    fun toContestResult() = ContestResult(
        execution.project.name,
        execution.project.organization.name,
        contest.name,
        execution.score,
        execution.startTime.toKotlinLocalDateTime(),
        execution.status,
        execution.sdk,
        execution.failedTests != 0L,
    )
}
