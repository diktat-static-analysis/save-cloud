package com.saveourtool.common.entities

import com.saveourtool.common.domain.Role
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.info.UserStatus
import com.saveourtool.common.spring.entity.BaseEntityWithDate

import com.fasterxml.jackson.annotation.JsonIgnore

import javax.persistence.*

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * @property name
 * @property password *in plain text*
 * @property role role of this user
 * @property email email of user
 * @property avatar avatar of user
 * @property company
 * @property location
 * @property linkedin
 * @property gitHub
 * @property twitter
 * @property status
 * @property originalLogins
 * @property rating rating of user
 * @property website
 * @property freeText
 * @property realName
 */
@Entity
@Suppress("LongParameterList")
@Table(schema = "save_cloud", name = "user")
class User(
    var name: String,
    var password: String?,
    var role: String?,
    var email: String? = null,
    var avatar: String? = null,
    var company: String? = null,
    var location: String? = null,
    var linkedin: String? = null,
    @Column(name = "git_hub")
    var gitHub: String? = null,
    var twitter: String? = null,
    @Enumerated(EnumType.STRING)
    var status: UserStatus = UserStatus.CREATED,
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "user",
        targetEntity = OriginalLogin::class
    )
    @JsonIgnore
    var originalLogins: List<OriginalLogin> = emptyList(),
    var rating: Long = 0,
    var website: String? = null,
    @Column(name = "free_text")
    var freeText: String? = null,
    @Column(name = "real_name")
    var realName: String? = null,
) : BaseEntityWithDate() {
    /**
     * @param projects roles in projects
     * @param organizations roles in organizations
     * @return [UserInfo] object
     */
    fun toUserInfo(projects: Map<String, Role> = emptyMap(), organizations: Map<String, Role> = emptyMap()) = UserInfo(
        id = id,
        name = name,
        originalLogins = originalLogins.associate { it.source to it.name },
        projects = projects,
        organizations = organizations,
        email = email,
        avatar = avatar,
        company = company,
        linkedin = linkedin,
        gitHub = gitHub,
        twitter = twitter,
        globalRole = role?.let { Role.fromSpringSecurityRole(it) } ?: Role.VIEWER,
        location = location,
        status = status,
        rating = rating,
        website = website,
        freeText = freeText,
        realName = realName,
        createDate = createDate?.toKotlinLocalDateTime(),
    )
}
