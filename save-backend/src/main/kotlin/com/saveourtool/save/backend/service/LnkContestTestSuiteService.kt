package com.saveourtool.save.backend.service

import com.saveourtool.common.entities.*
import com.saveourtool.save.backend.repository.LnkContestTestSuiteRepository

import org.springframework.stereotype.Service

/**
 * Service of [LnkContestTestSuite]
 */
@Service
class LnkContestTestSuiteService(
    private val lnkContestTestSuiteRepository: LnkContestTestSuiteRepository,
) {
    /**
     * @param contest
     * @return all [TestSuite]s with [contest]
     */
    fun getAllTestSuitesByContest(contest: Contest) = lnkContestTestSuiteRepository.findByContest(contest)
        .map {
            it.testSuite
        }

    /**
     * @param contest
     * @return list of test suite ids
     */
    fun getAllTestSuiteIdsByContest(contest: Contest) = getAllTestSuitesByContest(contest).map { it.requiredId() }

    /**
     * @param lnkContestTestSuite
     */
    fun save(lnkContestTestSuite: LnkContestTestSuite): LnkContestTestSuite = lnkContestTestSuiteRepository.save(lnkContestTestSuite)

    /**
     * @param lnkContestTestSuites collection of [LnkContestTestSuite]
     */
    fun saveAll(lnkContestTestSuites: Collection<LnkContestTestSuite>): Collection<LnkContestTestSuite> = lnkContestTestSuiteRepository.saveAll(lnkContestTestSuites)
}
