package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDateAndDto

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * @property message
 * @property user
 */
@Entity
@Table(schema = "save_cloud", name = "notification")
class Notification(
    var message: String,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,
) : BaseEntityWithDateAndDto<NotificationDto>() {
    override fun toDto() = NotificationDto(
        id = requiredId(),
        message = message,
        createDate = createDate?.toKotlinLocalDateTime(),
    )
}
