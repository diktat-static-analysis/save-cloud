package com.saveourtool.common.repository

import com.saveourtool.common.entities.OriginalLogin
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository to access data about original user logins and sources
 */
@Repository
interface OriginalLoginRepository : BaseEntityRepository<OriginalLogin> {
    /**
     * @param name
     * @param source
     * @return user or null if no results have been found
     */
    fun findByNameAndSource(name: String, source: String): OriginalLogin?

    /**
     * @param id id of user
     */
    fun deleteByUserId(id: Long)
}
