package com.saveourtool.save.backend.service

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.entities.*
import com.saveourtool.common.service.ProjectService
import com.saveourtool.common.utils.debug
import com.saveourtool.common.utils.getLogger
import com.saveourtool.save.backend.repository.LnkContestProjectRepository

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service of [LnkContestProject]
 */
@Service
class LnkContestProjectService(
    private val lnkContestProjectRepository: LnkContestProjectRepository,
    private val lnkContestExecutionService: LnkContestExecutionService,
    private val projectService: ProjectService,
) {
    /**
     * @param contestName name of a [Contest]
     * @return list of links contest-project for a contest
     */
    fun getAllByContestName(contestName: String): List<LnkContestProject> = lnkContestProjectRepository.findByContestName(contestName)

    /**
     * @param projectName
     * @param organizationName
     * @param numberOfContests
     * @return list of best [numberOfContests] contests of a project
     */
    fun getByProjectNameAndOrganizationName(
        projectName: String,
        organizationName: String,
        numberOfContests: Int,
    ): List<LnkContestProject> = lnkContestProjectRepository.findByProjectNameAndProjectOrganizationName(
        projectName,
        organizationName,
        PageRequest.ofSize(numberOfContests),
    )

    /**
     * @param contestName name of a contest
     * @param projects list of projects
     * @return list of [Project]s that are participating in contest with name [contestName] and are in list [projects]
     */
    fun getProjectsFromListAndContest(contestName: String, projects: List<Project>) = projects.mapNotNull { it.id }
        .toSet()
        .let {
            lnkContestProjectRepository.findByContestNameAndProjectIdIn(contestName, it)
        }

    /**
     * @param project
     * @param contestName
     * @return best score of [project] under [Contest] with name [contestName]
     */
    fun getBestScoreOfProjectInContestWithName(project: Project, contestName: String) =
            lnkContestProjectRepository.findByProjectAndContestName(project, contestName)?.bestExecution?.score

    /**
     * @param project a [Project]
     * @param contest a [Contest]
     * @return true if record is saved, false if is already present
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun saveLnkContestProject(project: Project, contest: Contest): Boolean = if (lnkContestProjectRepository.findByContestAndProject(contest, project).isPresent) {
        false
    } else {
        lnkContestProjectRepository.save(LnkContestProject(project, contest, null))
        true
    }

    /**
     * @param newExecution
     */
    @Transactional
    fun updateBestExecution(newExecution: Execution) {
        val newScore = requireNotNull(newExecution.score) {
            "Cannot update best score, because no score has been provided for execution id=${newExecution.id}"
        }
        val contest = lnkContestExecutionService.findContestByExecution(newExecution)
            ?: error("Execution was performed not as a part of contest")
        val project = newExecution.project
        val lnkContestProject = lnkContestProjectRepository.findByContestAndProject(contest, project)
            .orElseThrow {
                IllegalStateException("Project ${project.shortToString()} is not bound to contest name=${contest.name}")
            }
        val oldBestScore = lnkContestProject.bestExecution?.score
        if (oldBestScore == null || oldBestScore <= newScore) {
            logger.debug {
                "For project ${project.shortToString()} updating best_score from execution " +
                        "[id=${lnkContestProject.bestExecution?.id},score=$oldBestScore] to " +
                        "[id=${newExecution.id},score=$newScore]"
            }
            lnkContestProject.bestExecution = newExecution
            lnkContestProjectRepository.save(lnkContestProject)

            updateProjectContestRating(project)
        }
    }

    private fun updateProjectContestRating(project: Project) {
        val projectContestRating = lnkContestProjectRepository.findByProject(project).mapNotNull {
            it.bestExecution?.score
        }.sum()

        projectService.updateProject(project.apply {
            this.contestRating = projectContestRating
        })
    }

    /**
     * @param projectCoordinates
     * @param contestName
     * @return whether project by [projectCoordinates] is enrolled into a contest by [contestName]
     */
    fun isEnrolled(projectCoordinates: ProjectCoordinates, contestName: String): Boolean = lnkContestProjectRepository.findByContestNameAndProjectOrganizationNameAndProjectName(
        contestName, projectCoordinates.organizationName, projectCoordinates.projectName
    ) != null

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<LnkContestProjectService>()
    }
}
