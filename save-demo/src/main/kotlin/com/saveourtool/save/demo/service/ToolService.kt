package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.GithubRepo
import com.saveourtool.save.demo.entity.Snapshot
import com.saveourtool.save.demo.entity.Tool
import com.saveourtool.save.demo.repository.ToolRepository
import com.saveourtool.save.domain.ProjectCoordinates
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [Service] for [ToolService] entity
 */
@Service
class ToolService(
    private val toolRepository: ToolRepository,
    private val githubRepoService: GithubRepoService,
    private val snapshotService: SnapshotService,
) {
    private fun save(githubRepo: GithubRepo, snapshot: Snapshot) = toolRepository.save(Tool(githubRepo, snapshot))

    /**
     * @return list of tools that are stored in database
     */
    fun getSupportedTools(): List<Tool> = toolRepository.findAll()

    /**
     * @param githubRepo
     * @param snapshot
     * @return [Tool] entity saved to database
     * @throws IllegalStateException if tool is already present in DB
     */
    @Transactional
    fun saveIfNotPresent(githubRepo: GithubRepo, snapshot: Snapshot): Tool {
        val githubRepoFromDb = githubRepoService.saveIfNotPresent(githubRepo)
        val snapshotFromDb = snapshotService.saveIfNotPresent(snapshot)
        return toolRepository.findByGithubRepoAndSnapshot(githubRepoFromDb, snapshotFromDb)?.let {
            throw IllegalStateException(
                "Tool ${githubRepo.organizationName}/${githubRepo.toolName} of version ${snapshot.version} is already present in DB."
            )
        } ?: save(githubRepoFromDb, snapshotFromDb)
    }

    /**
     * @param githubRepo
     * @param version
     * @return [Tool] fetched from [githubRepo] that matches requested [version]
     */
    fun findByGithubRepoAndVersion(githubRepo: GithubRepo, version: String) = toolRepository.findByGithubRepoAndSnapshotVersion(githubRepo, version)

    /**
     * @param githubRepo GitHub credentials
     * @return currently used version
     * todo: allow to use multiple versions
     */
    fun findCurrentVersion(githubRepo: GithubRepo): String? = toolRepository.findByGithubRepo(githubRepo)
        .maxOfOrNull { it.snapshot.version }

    /**
     * @param githubCoordinates GitHub project coordinates
     * @return currently used version
     * todo: allow to use multiple versions
     */
    fun findCurrentVersion(githubCoordinates: ProjectCoordinates): String? = with(githubCoordinates) {
        githubRepoService.find(organizationName, projectName)
    }
        ?.let {
            toolRepository.findByGithubRepo(it)
        }
        ?.maxOfOrNull { it.snapshot.version }
}
