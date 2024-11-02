package com.saveourtool.save.backend.service

import com.saveourtool.common.entities.Notification
import com.saveourtool.common.repository.NotificationRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for notifications
 */
@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    /**
     * @param notification
     * @return saved notification
     */
    @Transactional
    fun save(notification: Notification): Notification = notificationRepository.save(notification)

    /**
     * @param notifications
     * @return saved notifications
     */
    @Transactional
    fun saveAll(notifications: List<Notification>): List<Notification> = notificationRepository.saveAll(notifications)

    /**
     * @param name name of user
     * @return list of notifications
     */
    fun getAllByUserName(name: String): List<Notification> = notificationRepository.findByUserName(name)

    /**
     * @param id of notification
     */
    @Transactional
    fun deleteById(id: Long) = notificationRepository.deleteById(id)

    /**
     * @param id of notification
     * @return notification by id
     */
    fun getById(id: Long) = notificationRepository.findByIdOrNull(id)
}
