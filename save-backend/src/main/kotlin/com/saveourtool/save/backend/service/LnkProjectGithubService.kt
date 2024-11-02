package com.saveourtool.save.backend.service

import com.saveourtool.common.entities.*
import com.saveourtool.save.backend.repository.LnkProjectGithubRepository

import org.springframework.stereotype.Service

/**
 * Service of [LnkProjectGithub]
 */
@Service
class LnkProjectGithubService(
    private val lnkProjectGithubRepository: LnkProjectGithubRepository,
) {
    /**
     * @param organizationName organization name from saveourtool
     * @param projectName project name from saveourtool
     * @return [LnkProjectGithub] by saveourtool project and organization name
     */
    fun findBySaveCredentials(organizationName: String, projectName: String) = lnkProjectGithubRepository
        .findByProjectOrganizationNameAndProjectName(organizationName, projectName)

    /**
     * @param ownerName user/organization name from GitHub
     * @param repoName project name from GitHub
     * @return [LnkProjectGithub] by GitHub project and user/organization name
     */
    fun findByGithubCredentials(ownerName: String, repoName: String) = lnkProjectGithubRepository
        .findByGithubOwnerAndGithubRepoName(ownerName, repoName)

    /**
     * @param project
     * @return [LnkProjectGithub] by project entity
     */
    fun findByProject(project: Project) = lnkProjectGithubRepository.findByProject(project)

    /**
     * @param project project from database
     * @param ownerName user/organization name from GitHub
     * @param repoName project name from GitHub
     * @return [LnkProjectGithub] saved to database
     */
    fun saveIfNotPresent(project: Project, ownerName: String, repoName: String): LnkProjectGithub = lnkProjectGithubRepository.findByProject(project)
        ?: lnkProjectGithubRepository.save(
            LnkProjectGithub(
                project,
                ownerName,
                repoName,
            )
        )

    /**
     * @param lnkProjectGithub entity to be deleted
     */
    fun delete(lnkProjectGithub: LnkProjectGithub) = lnkProjectGithubRepository.delete(lnkProjectGithub)
}
