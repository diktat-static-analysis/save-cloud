package com.saveourtool.common.entities

import com.saveourtool.common.entities.contest.ContestDto
import com.saveourtool.common.entities.contest.ContestStatus
import com.saveourtool.common.spring.entity.BaseEntity
import com.saveourtool.common.validation.isValidName

import com.fasterxml.jackson.annotation.JsonIgnore

import java.time.LocalDateTime
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

import kotlinx.datetime.*

/**
 * @property name organization
 * @property status
 * @property startTime the time contest starts
 * @property endTime the time contest ends
 * @property organization organization that created this contest
 * @property description
 * @property creationTime
 * @property testSuiteLinks
 */
@Entity
@Suppress("LongParameterList")
class Contest(
    var name: String,
    @Enumerated(EnumType.STRING)
    var status: ContestStatus,
    var startTime: LocalDateTime?,
    var endTime: LocalDateTime?,
    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization,
    var description: String? = null,
    var creationTime: LocalDateTime?,
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "contest",
        targetEntity = LnkContestTestSuite::class,
    )
    @JsonIgnore
    var testSuiteLinks: List<LnkContestTestSuite>,
) : BaseEntity() {
    /**
     * Create Data Transfer Object in order to pass entity to frontend
     *
     * @return [ContestDto]
     */
    @Suppress("UnsafeCallOnNullableType")
    fun toDto() = ContestDto(
        name,
        status,
        startTime?.toKotlinLocalDateTime()!!,
        endTime?.toKotlinLocalDateTime()!!,
        description,
        organization.name,
        testSuiteLinks.map { it.testSuite.toVersioned(it.testSuite.sourceSnapshot.commitId) },
        creationTime?.toKotlinLocalDateTime(),
    )

    /**
     * @return Test Suites that are attached to the contest
     */
    fun testSuites() = testSuiteLinks.map {
        it.testSuite
    }

    private fun validateDateRange() = startTime != null && endTime != null && (startTime as LocalDateTime) < endTime

    /**
     * Validate contest data
     *
     * @return true if contest data is valid, false otherwise
     */
    override fun validate() = name.isValidName() && validateDateRange()

    companion object {
        /**
         * Create a stub for testing.
         *
         * @param id id of created
         * @param status
         * @return an organization
         */
        fun stub(
            id: Long?,
            status: ContestStatus = ContestStatus.CREATED
        ) = Contest(
            name = "stub",
            status = status,
            startTime = null,
            endTime = null,
            organization = Organization.stub(1),
            creationTime = LocalDateTime.now(),
            testSuiteLinks = emptyList(),
        ).apply {
            this.id = id
        }

        /**
         * Create [Contest] from [ContestDto]
         *
         * @param organization that created contest
         * @param creationTime specified time when contest was created
         * @param testSuiteLinks
         * @return [Contest] entity
         */
        fun ContestDto.toContest(
            organization: Organization,
            testSuiteLinks: List<LnkContestTestSuite>,
            creationTime: LocalDateTime? = null,
        ) = Contest(
            name,
            status,
            startTime?.toJavaLocalDateTime(),
            endTime?.toJavaLocalDateTime(),
            organization,
            description,
            creationTime ?: this.creationTime?.toJavaLocalDateTime(),
            testSuiteLinks,
        )
    }
}
