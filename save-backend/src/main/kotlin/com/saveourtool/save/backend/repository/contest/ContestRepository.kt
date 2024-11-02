package com.saveourtool.save.backend.repository.contest

import com.saveourtool.common.entities.Contest
import com.saveourtool.common.entities.contest.ContestStatus
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * The repository of contest entities
 */
@Repository
interface ContestRepository : BaseEntityRepository<Contest> {
    /**
     * @param name
     * @return contest by name
     */
    fun findByName(name: String): Contest?

    /**
     * @param contestIds
     * @return list of [Contest]s with ids from [contestIds]
     */
    fun findByIdIn(contestIds: Set<Long>): List<Contest>

    /**
     * @param currentTime
     * @param pageable
     * @param status
     * @return [Page] of contests with startTime greater than [currentTime]
     */
    fun findByEndTimeBeforeAndStatus(
        currentTime: LocalDateTime,
        status: ContestStatus,
        pageable: Pageable,
    ): Page<Contest>

    /**
     * @param currentTime
     * @param currentTimeAgain
     * @param pageable
     * @param status
     * @return [Page] of contests with startTime less than [currentTime] and endTime greater than [currentTime]
     */
    fun findByStartTimeBeforeAndEndTimeAfterAndStatus(
        currentTime: LocalDateTime,
        // need to pass current time twice due to spring features
        currentTimeAgain: LocalDateTime,
        status: ContestStatus,
        pageable: Pageable,
    ): Page<Contest>

    /**
     * @param currentTime
     * @param currentTimeAgain
     * @param status
     * @param contestIds
     * @param pageable
     * @return [Page] of active [Contest]s that ids are not in [contestIds]
     */
    fun findByStartTimeBeforeAndEndTimeAfterAndStatusAndIdNotIn(
        currentTime: LocalDateTime,
        currentTimeAgain: LocalDateTime,
        status: ContestStatus,
        contestIds: Set<Long>,
        pageable: Pageable,
    ): Page<Contest>

    /**
     * @param organizationName
     * @param pageable
     * @return [Page] of [Contest]s that belong to organization with name [organizationName]
     */
    fun findByOrganizationName(organizationName: String, pageable: Pageable): Page<Contest>

    /**
     * @param status
     * @param currentTime
     * @param pageable
     * @return List of [Contest]s
     */
    fun findByStatusNotAndEndTimeAfterOrderByCreationTimeDesc(status: ContestStatus, currentTime: LocalDateTime, pageable: Pageable): List<Contest>

    /**
     * Find ids of all contests marked as featured
     *
     * @return set filled with ids of featured contests
     */
    @Query("""select contest_id from save_cloud.featured_contests""", nativeQuery = true)
    fun findFeaturedContestIds(): Set<Long>

    /**
     * Method should be used to check whether a contest with given id is marked to be featured or not
     *
     * @param contestId
     * @return [contestId] if given [Contest] is featured, null otherwise
     */
    @Query("""select contest_id from save_cloud.featured_contests where contest_id = :id""", nativeQuery = true)
    fun findFeaturedContestById(@Param("id") contestId: Long): Long?

    /**
     * Mark [Contest] with [contestId] as featured
     *
     * @param contestId id of a [Contest]
     */
    @Modifying
    @Query("""insert into save_cloud.featured_contests values (null, :id)""", nativeQuery = true)
    fun saveFeaturedContest(@Param("id") contestId: Long)

    /**
     * Unmark [Contest] with [contestId] as featured
     *
     * @param contestId id of a [Contest]
     */
    @Modifying
    @Query("""delete from save_cloud.featured_contests where contest_id = :id""", nativeQuery = true)
    fun deleteFeaturedContestById(@Param("id") contestId: Long)
}
