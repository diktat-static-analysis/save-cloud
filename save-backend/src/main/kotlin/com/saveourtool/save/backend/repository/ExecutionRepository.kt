package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.Organization
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional
import javax.persistence.LockModeType
import javax.persistence.QueryHint

/**
 * Repository of execution
 */
@Repository
interface ExecutionRepository : BaseEntityRepository<Execution> {
    /**
     * @param name name of project
     * @param organization organization of project
     * @return list of executions
     */
    fun getAllByProjectNameAndProjectOrganization(name: String, organization: Organization): List<Execution>

    /**
     * @param name name of project
     * @param organization organization of project
     * @param start start date
     * @param end end date
     * @return list of executions
     */
    fun findByProjectNameAndProjectOrganizationAndStartTimeBetween(
        name: String,
        organization: Organization,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Execution>

    /**
     * Get latest (by start time an) execution by project name and organization
     *
     * @param name name of project
     * @param organizationName name of organization of project
     * @return execution or null if it was not found
     */
    @Suppress("IDENTIFIER_LENGTH")
    fun findTopByProjectNameAndProjectOrganizationNameOrderByStartTimeDesc(name: String, organizationName: String): Optional<Execution>

    /**
     * @param id if the execution
     * @return execution
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "javax.persistence.lock.timeout", value = "10000"))
    fun findWithLockingById(id: Long): Execution?
}
