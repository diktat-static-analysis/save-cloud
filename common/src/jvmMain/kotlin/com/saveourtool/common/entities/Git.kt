package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDto
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * Data class with repository information
 * fixme should operate not with password, but with some sort of token (github integration)
 *
 * @property url url of repo
 * @property username username to credential
 * @property password password to credential
 * @property organization
 */
@Entity
@Table(schema = "save_cloud", name = "git")
class Git(
    var url: String,
    var username: String? = null,
    var password: String? = null,

    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization,
) : BaseEntityWithDto<GitDto>() {
    /**
     * @return git dto
     */
    override fun toDto() = GitDto(
        url = url,
        username = username,
        password = password,
    )

    companion object {
        val empty = Git(
            url = "",
            username = null,
            password = null,
            organization = Organization.stub(-1)
        )
    }
}
