package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository

/**
 * The repository of organization entities
 */
@Repository
interface OrganizationRepository : JpaRepository<Organization, Long>,
QueryByExampleExecutor<Organization>,
JpaSpecificationExecutor<Organization>,
ValidateRepository {
    /**
     * @param name
     * @return organization by name
     */
    fun findByName(name: String): Organization?

    /**
     * @param id
     * @return organization by id
     */
    // The getById method from JpaRepository can lead to LazyInitializationException
    fun getOrganizationById(id: Long): Organization

    /**
     * @param prefix prefix of organization name
     * @param status is status to be found
     * @return list of organizations with names that start with [prefix]
     */
    fun findByNameStartingWithAndStatus(prefix: String, status: OrganizationStatus): List<Organization>

    /**
     * @param prefix prefix of organization name
     * @param statuses is set of statuses, one of which an organization can have
     * @return list of organizations with names that start with [prefix]
     */
    fun findByNameStartingWithAndStatusIn(prefix: String, statuses: Set<OrganizationStatus>): List<Organization>

    /**
     * @param status
     * @return list of organizations with required status
     */
    fun findByStatus(status: OrganizationStatus): List<Organization>

    /**
     * @param statuses is set of statuses, one of which an organization can have
     * @return list of organizations with required status
     */
    fun findByStatusIn(statuses: Set<OrganizationStatus>): List<Organization>
}
