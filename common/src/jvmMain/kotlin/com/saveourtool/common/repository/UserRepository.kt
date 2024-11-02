package com.saveourtool.common.repository

import com.saveourtool.common.entities.User
import com.saveourtool.common.info.UserStatus
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * Repository to access data about users
 */
@Repository
interface UserRepository : BaseEntityRepository<User>, ValidateRepository {
    /**
     * @param ids
     * @return users with [ids]
     */
    fun findAllByIdIn(ids: List<Long>): List<User>

    /**
     * @param username
     * @return user or null if no results have been found
     */
    fun findByName(username: String): User?

    /**
     * @param role
     * @return users with status
     */
    fun findByRole(role: String): List<User>

    /**
     * @param status
     * @return users with status
     */
    fun findByStatus(status: UserStatus): List<User>

    /**
     * @param status
     * @return count users
     */
    fun countByStatus(status: UserStatus): Int

    /**
     * @param username
     * @param ids set of id of people that should not be found
     * @return list of users with [username] except those whose ids are in [ids]
     */
    fun findByNameAndIdNotIn(username: String, ids: Set<Long>): List<User>

    /**
     * @param prefix
     * @param ids
     * @param page
     * @return [Page] of users with names that start with [prefix] and id not in [ids]
     */
    fun findByNameStartingWithAndIdNotIn(prefix: String, ids: Set<Long>, page: Pageable): Page<User>

    /**
     * @param prefix
     * @param names
     * @param page
     * @return [Page] of users with names that start with [prefix] and name not in [names]
     */
    fun findByNameStartingWithAndNameNotIn(prefix: String, names: Set<String>, page: Pageable): Page<User>

    /**
     * @param prefix
     * @return list of users with names that start with [prefix]
     */
    fun findByNameStartingWith(prefix: String): List<User>

    /**
     * @param prefix
     * @param page
     * @return [Page] of users with names that start with [prefix]
     */
    fun findByNameStartingWith(prefix: String, page: Pageable): Page<User>
}
