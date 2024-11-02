package com.saveourtool.save.backend.service

import com.saveourtool.common.entities.*
import com.saveourtool.common.utils.orNotFound
import com.saveourtool.save.backend.repository.LnkContestExecutionRepository
import com.saveourtool.save.backend.repository.contest.ContestRepository

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Service of [LnkContestExecution]
 */
@Service
class LnkContestExecutionService(
    private val contestRepository: ContestRepository,
    private val lnkContestExecutionRepository: LnkContestExecutionRepository,
) {
    /**
     * @param contest
     * @param project
     * @param pageRequest
     * @return list of [LnkContestExecution]s by [Contest] and [Project]
     */
    fun getPageExecutionsByContestAndProject(
        contest: Contest,
        project: Project,
        pageRequest: PageRequest,
    ): List<LnkContestExecution> = lnkContestExecutionRepository
        .findByExecutionProjectAndContestOrderByExecutionStartTimeDesc(project, contest, pageRequest)
        .content

    /**
     * @param contest
     * @param project
     * @return latest [LnkContestExecution] by [Contest] and [Project]
     */
    fun getLatestExecutionByContestAndProject(contest: Contest, project: Project) = lnkContestExecutionRepository
        .findFirstByContestAndExecutionProjectOrderByExecutionStartTimeDesc(contest, project)

    /**
     * @param contest
     * @param projectIds
     * @return list of latest [LnkContestExecution] of [Project]s with [Project.id] in [projectIds] in [Contest]
     */
    fun getLatestExecutionByContestAndProjectIds(contest: Contest, projectIds: List<Long>) = lnkContestExecutionRepository
        .findByContestAndExecutionProjectIdInOrderByExecutionStartTimeDesc(contest, projectIds)
        .distinctBy { it.execution.project }

    /**
     * @param execution
     * @return a [Contest] under which [execution] has been performed, or `null` if [execution] is not associated
     * with any [Contest]
     */
    fun findContestByExecution(execution: Execution) = lnkContestExecutionRepository.findByExecution(execution)?.contest

    /**
     * @param execution
     * @param contestName
     * @return a created [LnkContestExecution] or exception with code 404
     */
    fun createLink(execution: Execution, contestName: String): LnkContestExecution =
            lnkContestExecutionRepository.save(
                LnkContestExecution(execution = execution, contest = contestRepository.findByName(contestName).orNotFound())
            )
}
