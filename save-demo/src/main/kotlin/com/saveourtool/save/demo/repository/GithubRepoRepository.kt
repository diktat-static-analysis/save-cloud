package com.saveourtool.save.demo.repository

import com.saveourtool.common.spring.repository.BaseEntityRepository
import com.saveourtool.save.demo.entity.GithubRepo

import org.springframework.stereotype.Repository

/**
 * JPA repository for [GithubRepo] entity.
 */
@Repository
interface GithubRepoRepository : BaseEntityRepository<GithubRepo> {
    /**
     * @param projectName
     * @param organizationName
     * @return [GithubRepo] if an entity with such [projectName] and [organizationName] was found in database, null otherwise
     */
    fun findByProjectNameAndOrganizationName(projectName: String, organizationName: String): GithubRepo?
}
