package com.saveourtool.save.demo.entity

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.spring.entity.BaseEntity
import com.saveourtool.common.utils.github.GitHubRepoInfo
import javax.persistence.Entity
import javax.persistence.Table

/**
 * @property organizationName
 * @property projectName
 */
@Entity
@Table(name = "git_repo")
class GithubRepo(
    override var organizationName: String,
    override var projectName: String,
) : BaseEntity(), GitHubRepoInfo {
    /**
     * @return pretty string that defines [GithubRepo]
     */
    fun toPrettyString() = "$organizationName/$projectName"
}

/**
 * @return [GithubRepo] from [ProjectCoordinates]
 */
fun ProjectCoordinates.toGithubRepo() = GithubRepo(
    organizationName,
    projectName,
)
