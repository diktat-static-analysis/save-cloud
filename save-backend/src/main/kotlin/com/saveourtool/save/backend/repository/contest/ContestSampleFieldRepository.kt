package com.saveourtool.save.backend.repository.contest

import com.saveourtool.common.entities.ContestSampleField
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * The repository of contest sample field entities
 */
@Repository
interface ContestSampleFieldRepository : BaseEntityRepository<ContestSampleField> {
    /**
     * @param contestSampleId
     * @return [ContestSampleField] by [contestSampleId]
     */
    fun findByContestSampleId(contestSampleId: Long): List<ContestSampleField>
}
