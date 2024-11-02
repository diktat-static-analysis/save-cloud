package com.saveourtool.save.backend.repository.contest

import com.saveourtool.common.entities.ContestSample
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * The repository of contest sample entities
 */
@Repository
interface ContestSampleRepository : BaseEntityRepository<ContestSample>
