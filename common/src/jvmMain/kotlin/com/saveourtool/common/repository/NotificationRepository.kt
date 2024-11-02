package com.saveourtool.common.repository

import com.saveourtool.common.entities.Notification
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository to access data about user notification
 */
@Repository
interface NotificationRepository : BaseEntityRepository<Notification> {
    /**
     * @param name
     * @return list of notification
     */
    fun findByUserName(name: String): List<Notification>
}
