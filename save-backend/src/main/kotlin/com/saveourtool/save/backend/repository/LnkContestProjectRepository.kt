package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.Contest
import com.saveourtool.common.entities.LnkContestProject
import com.saveourtool.common.entities.Project
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Repository of [LnkContestProject]
 */
@Repository
interface LnkContestProjectRepository : BaseEntityRepository<LnkContestProject> {
    /**
     * @param project
     * @return [LnkContestProject] by [project]
     */
    fun findByProject(project: Project): List<LnkContestProject>

    /**
     * @param contest
     * @param project
     * @return [LnkContestProject] by [contest] and [project]
     */
    fun findByContestAndProject(contest: Contest, project: Project): Optional<LnkContestProject>

    /**
     * @param projectName
     * @param organizationName
     * @param pageable
     * @return link contest-project for project with name [projectName] and contest with name [contestName]
     */
    fun findByProjectNameAndProjectOrganizationName(
        projectName: String,
        organizationName: String,
        pageable: Pageable,
    ): List<LnkContestProject>

    /**
     * @param contestName
     * @param projectIds
     * @return list or [LnkContestProject] of projects with ids from [projectIds] that participate in contest with name [contestName]
     */
    fun findByContestNameAndProjectIdIn(
        contestName: String,
        projectIds: Set<Long>,
    ): List<LnkContestProject>

    /**
     * @param contestName
     * @param organizationName
     * @param projectName
     * @return a [LnkContestProject] if any has been found
     */
    fun findByContestNameAndProjectOrganizationNameAndProjectName(
        contestName: String,
        organizationName: String,
        projectName: String,
    ): LnkContestProject?

    /**
     * @param contestName
     * @return list of [LnkContestProject] linked to contest with name [contestName]
     */
    fun findByContestName(contestName: String): List<LnkContestProject>

    /**
     * @param project
     * @param contestName
     * @return a [LnkContestProject], if any, associated with [project] and [Contest] named [contestName]
     */
    fun findByProjectAndContestName(project: Project, contestName: String): LnkContestProject?
}
