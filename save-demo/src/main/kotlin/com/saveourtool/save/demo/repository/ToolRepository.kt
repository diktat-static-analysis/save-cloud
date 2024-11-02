package com.saveourtool.save.demo.repository

import com.saveourtool.common.spring.repository.BaseEntityRepository
import com.saveourtool.save.demo.entity.GithubRepo
import com.saveourtool.save.demo.entity.Snapshot
import com.saveourtool.save.demo.entity.Tool

import org.springframework.stereotype.Repository

/**
 * JPA repository for [Tool] entity.
 */
@Repository
interface ToolRepository : BaseEntityRepository<Tool> {
    /**
     * @param githubRepo
     * @return list of [Tool]s corresponding to [githubRepo]
     */
    fun findByGithubRepo(githubRepo: GithubRepo): List<Tool>

    /**
     * @param githubRepo
     * @param snapshot
     * @return [Tool] from [githubRepo] repository corresponding [snapshot].
     */
    fun findByGithubRepoAndSnapshot(githubRepo: GithubRepo, snapshot: Snapshot): Tool?
}
