package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.*
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository of [LnkProjectGithub]
 */
@Repository
interface LnkProjectGithubRepository : BaseEntityRepository<LnkProjectGithub> {
    /**
     * @param project
     * @return [LnkProjectGithub] by [project]
     */
    fun findByProject(project: Project): LnkProjectGithub?

    /**
     * @param organizationName
     * @param projectName
     * @return [LnkProjectGithub] by [projectName] and [organizationName]
     */
    fun findByProjectOrganizationNameAndProjectName(organizationName: String, projectName: String): LnkProjectGithub?

    /**
     * @param githubOwner
     * @param githubRepoName
     * @return [LnkProjectGithub] by [githubOwner] and [githubRepoName]
     */
    fun findByGithubOwnerAndGithubRepoName(githubOwner: String, githubRepoName: String): LnkProjectGithub?
}
