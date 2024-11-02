package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.Contest
import com.saveourtool.common.entities.LnkContestTestSuite
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository of [LnkContestTestSuite]
 */
@Repository
interface LnkContestTestSuiteRepository : BaseEntityRepository<LnkContestTestSuite> {
    /**
     * @param contest
     * @return [LnkContestTestSuite] by [contest]
     */
    fun findByContest(contest: Contest): List<LnkContestTestSuite>
}
