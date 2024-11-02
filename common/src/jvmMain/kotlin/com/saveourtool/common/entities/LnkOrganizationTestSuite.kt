package com.saveourtool.common.entities

import com.saveourtool.common.permission.Rights
import com.saveourtool.common.spring.entity.BaseEntityWithDto
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property organization organization that is connected to [testSuite]
 * @property testSuite manageable test suite
 * @property rights [Rights] that [organization] has over [testSuite]
 */
@Entity
class LnkOrganizationTestSuite(
    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization,

    @ManyToOne
    @JoinColumn(name = "test_suite_id")
    var testSuite: TestSuite,

    @Enumerated(EnumType.STRING)
    var rights: Rights,
) : BaseEntityWithDto<LnkOrganizationTestSuiteDto>() {
    override fun toDto() = LnkOrganizationTestSuiteDto(
        organization.toDto(),
        testSuite.toDto(),
        rights
    )
}
