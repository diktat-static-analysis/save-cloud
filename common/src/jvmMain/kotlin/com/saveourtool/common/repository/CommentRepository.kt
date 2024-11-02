package com.saveourtool.common.repository

import com.saveourtool.common.entities.Comment
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository to access data about user comments
 */
@Repository
interface CommentRepository : BaseEntityRepository<Comment> {
    /**
     * @param section section of Save
     * @return list of user comments
     */
    fun getAllBySection(section: String): List<Comment>

    /**
     * @param section section of Save
     * @return count comments
     */
    fun countAllBySection(section: String): Int

    /**
     * @param userName comment author username
     * @param section [Comment.section]
     * @param creationDate [Comment.createDate]
     * @return [Comment] if found, null otherwise
     */
    fun findByUserNameAndSectionAndCreateDate(
        userName: String,
        section: String,
        creationDate: LocalDateTime?,
    ): Comment?
}
