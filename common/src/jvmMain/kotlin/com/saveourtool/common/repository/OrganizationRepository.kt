package com.saveourtool.common.repository

import com.saveourtool.common.entities.Organization
import com.saveourtool.common.entities.OrganizationStatus
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * The repository of organization entities
 */
@Repository
interface OrganizationRepository : BaseEntityRepository<Organization>,
ValidateRepository {
    /**
     * @param name
     * @return organization by [name]
     */
    fun findByName(name: String): Organization?

    /**
     * @param name
     * @param statuses
     * @return organization by [name] and [statuses]
     */
    fun findByNameAndStatusIn(name: String, statuses: Set<OrganizationStatus>): Organization?

    /**
     * @param id
     * @return organization by id
     */
    // The getById method from JpaRepository can lead to LazyInitializationException
    fun getOrganizationById(id: Long): Organization

    /**
     * @param prefix prefix of organization name
     * @param statuses is set of statuses, one of which an organization can have
     * @param pageable [Pageable]
     * @return list of organizations with names that start with [prefix]
     */
    fun findByNameStartingWithAndStatusIn(prefix: String, statuses: Set<OrganizationStatus>, pageable: Pageable): List<Organization>
}
