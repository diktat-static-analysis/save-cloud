package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.*
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * Repository of [LnkContestExecution]
 */
@Repository
interface LnkContestExecutionRepository : BaseEntityRepository<LnkContestExecution> {
    /**
     * Get N best scores of a [Project] in contest with name [contestName]
     *
     * @param project
     * @param contestName
     * @param pageable
     * @return record with a page of N best scores of a contests of the project
     */
    fun findByExecutionProjectAndContestNameOrderByExecutionScoreDesc(project: Project, contestName: String, pageable: Pageable): Page<LnkContestExecution>

    /**
     * Get N executions of a [Project] in [Contest]
     *
     * @param project
     * @param contest
     * @param pageable
     * @return page of N records with executions sorted by starting time
     */
    @Suppress("IDENTIFIER_LENGTH")
    fun findByExecutionProjectAndContestOrderByExecutionStartTimeDesc(project: Project, contest: Contest, pageable: Pageable): Page<LnkContestExecution>

    /**
     * @param contest
     * @param project
     * @return [LnkContestExecution] if found, null otherwise
     */
    @Suppress("IDENTIFIER_LENGTH")
    fun findFirstByContestAndExecutionProjectOrderByExecutionStartTimeDesc(contest: Contest, project: Project): LnkContestExecution?

    /**
     * @param contest
     * @param projectIds
     * @return list of [LnkContestExecution]s
     */
    @Suppress("IDENTIFIER_LENGTH")
    fun findByContestAndExecutionProjectIdInOrderByExecutionStartTimeDesc(contest: Contest, projectIds: List<Long>): List<LnkContestExecution>

    /**
     * @param project
     * @param contestName
     * @return [LnkContestExecution] associated with [project] and [Contest] with name [contestName]
     */
    fun findByExecutionProjectAndContestName(project: Project, contestName: String): LnkContestExecution?

    /**
     * @param execution
     * @return [LnkContestExecution] if any is associated with [execution]
     */
    fun findByExecution(execution: Execution): LnkContestExecution?
}
