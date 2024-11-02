package com.saveourtool.save.demo.repository

import com.saveourtool.common.spring.repository.BaseEntityRepository
import com.saveourtool.save.demo.entity.Demo

import org.springframework.stereotype.Repository

/**
 * JPA repository for [Demo] entity.
 */
@Repository
interface DemoRepository : BaseEntityRepository<Demo> {
    /**
     * @param organizationName
     * @param projectName
     * @return [Demo] of [organizationName]/[projectName] saveourtool project
     */
    fun findByOrganizationNameAndProjectName(organizationName: String, projectName: String): Demo?
}
