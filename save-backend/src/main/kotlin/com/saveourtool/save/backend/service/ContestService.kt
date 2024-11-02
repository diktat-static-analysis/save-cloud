package com.saveourtool.save.backend.service

import com.saveourtool.common.entities.Contest
import com.saveourtool.common.entities.Organization
import com.saveourtool.common.entities.contest.ContestStatus
import com.saveourtool.save.backend.repository.contest.ContestRepository

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDateTime
import java.util.*

/**
 * Service for contests
 *
 * @param contestRepository
 */
@OptIn(ExperimentalStdlibApi::class)
@Service
class ContestService(
    private val contestRepository: ContestRepository,
) {
    /**
     * @param contestId
     * @return contest by id
     */
    fun findById(contestId: Long): Optional<Contest> = contestRepository.findById(contestId)

    /**
     * @param contestIds
     * @return [Contest]s with ids from [contestIds]
     */
    fun findByIdIn(contestIds: Set<Long>): List<Contest> = contestRepository.findByIdIn(contestIds)

    /**
     * @param contestId
     * @return true if contest with [contestId] is marked as featured, false otherwise
     */
    fun isContestFeatured(contestId: Long) = contestRepository.findFeaturedContestById(contestId) != null

    /**
     * @param name name of contest
     * @return contest by name
     */
    fun findByName(name: String) = contestRepository.findByName(name)

    /**
     * @param pageSize amount of contests that should be taken
     * @return list of active contests
     */
    fun findContestsInProgress(pageSize: Int): List<Contest> = LocalDateTime.now().let {
        contestRepository.findByStartTimeBeforeAndEndTimeAfterAndStatus(
            it,
            it,
            ContestStatus.CREATED,
            Pageable.ofSize(pageSize),
        )
    }.content

    /**
     * @param pageSize amount of contests that should be taken
     * @return list of active contests
     */
    fun findFinishedContests(pageSize: Int): List<Contest> = LocalDateTime.now().let {
        contestRepository.findByEndTimeBeforeAndStatus(
            it,
            ContestStatus.CREATED,
            Pageable.ofSize(pageSize),
        )
    }.content

    /**
     * @param contestIds set of ids of a [Contest]s
     * @param numberOfRecords amount of records that should be taken from database
     * @return list of active [Contest]s which ids are not from [contestIds]
     */
    fun getAllActiveContestsNotFrom(contestIds: Set<Long>, numberOfRecords: Int): List<Contest> = LocalDateTime.now().let {
        contestRepository.findByStartTimeBeforeAndEndTimeAfterAndStatusAndIdNotIn(
            it,
            it,
            ContestStatus.CREATED,
            contestIds,
            Pageable.ofSize(numberOfRecords),
        ).content
    }

    /**
     * @param organizationName
     * @param pageable
     * @return page of contests
     */
    fun findPageOfContestsByOrganizationName(organizationName: String, pageable: Pageable) = contestRepository.findAll({ root, _, cb ->
        cb.and(
            cb.equal(root.get<Organization>("organization").get<String>("name"), organizationName),
            cb.notEqual(root.get<String>("status"), ContestStatus.DELETED)
        )
    },
        pageable
    )

    /**
     * @param newContest
     * @return [Contest]
     */
    fun updateContest(newContest: Contest) = contestRepository.save(newContest)

    /**
     * @param newContest
     * @return true if contest was saved, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    @Transactional
    fun createContestIfNotPresent(newContest: Contest): Boolean =
            contestRepository.findByName(newContest.name)?.let {
                false
            } ?: run {
                contestRepository.save(newContest.apply { creationTime = LocalDateTime.now() })
                true
            }

    /**
     * Change `feature` state of [Contest]: if contest is not marked to be featured, it will be marked so,
     * if contest is already featured, it will be unmarked
     *
     * @param contest
     * @return id of a [contest] if it is marked to be featured, null if [contest] is unmarked to be featured
     */
    @Transactional
    @Suppress("AVOID_NULL_CHECKS")
    fun addOrDeleteFeaturedContest(contest: Contest): Long? = contest.requiredId().let { contestId ->
        if (contestRepository.findFeaturedContestById(contestId) == null) {
            contestRepository.saveFeaturedContest(contestId)
            contestId
        } else {
            contestRepository.deleteFeaturedContestById(contestId)
            null
        }
    }

    /**
     * @return list of featured [Contest]s
     */
    fun getFeaturedContests(): List<Contest> = findByIdIn(contestRepository.findFeaturedContestIds())

    /**
     * @param pageSize amount of records that will be fetched
     * @return list of [pageSize] newest [Contest]s
     */
    fun getNewestContests(pageSize: Int): List<Contest> = contestRepository.findByStatusNotAndEndTimeAfterOrderByCreationTimeDesc(
        ContestStatus.DELETED,
        LocalDateTime.now(),
        PageRequest.ofSize(pageSize),
    )
}
