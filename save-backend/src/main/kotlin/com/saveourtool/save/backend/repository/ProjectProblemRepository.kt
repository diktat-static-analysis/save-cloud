package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.ProjectProblem
import com.saveourtool.common.entitiescosv.VulnerabilityMetadata
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository to access data about project problems
 */
@Repository
interface ProjectProblemRepository : BaseEntityRepository<ProjectProblem> {
    /**
     * @param projectName name of project
     * @param organizationName name of organization
     * @return list of project problems
     */
    fun getAllProblemsByProjectNameAndProjectOrganizationName(projectName: String, organizationName: String): List<ProjectProblem>

    /**
     * @param projectName name of project
     * @param organizationName name of organization
     * @param isClosed flag is project problem closed or not
     * @return list of project problems
     */
    fun getAllProblemsByProjectNameAndProjectOrganizationNameAndIsClosed(projectName: String, organizationName: String, isClosed: Boolean): List<ProjectProblem>

    /**
     * @param identifier [VulnerabilityMetadata.identifier]
     * @return found [VulnerabilityMetadata] or null
     */
    @Query(
        value = "select * from cosv.vulnerability_metadata vm where vm.identifier = :identifier",
        nativeQuery = true,
    )
    fun findVulnerabilityByIdentifier(@Param("identifier") identifier: String): VulnerabilityMetadata?
}
