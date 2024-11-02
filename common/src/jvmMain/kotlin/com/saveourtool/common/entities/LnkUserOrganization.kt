package com.saveourtool.common.entities

import com.saveourtool.common.domain.Role
import com.saveourtool.common.spring.entity.BaseEntity
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property organization
 * @property user in [organization]
 * @property role of [user] in [organization]
 */
@Entity
@Table(schema = "save_cloud", name = "lnk_user_organization")
class LnkUserOrganization(
    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

    @Enumerated(EnumType.STRING)
    var role: Role,
) : BaseEntity()
