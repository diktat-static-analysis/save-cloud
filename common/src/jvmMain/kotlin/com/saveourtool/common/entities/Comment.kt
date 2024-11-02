package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDateAndDto

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * @property message
 * @property section
 * @property user
 */
@Entity
@Table(schema = "save_cloud", name = "comments")
class Comment(

    var message: String,

    var section: String,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

) : BaseEntityWithDateAndDto<CommentDto>() {
    override fun toDto() = CommentDto(
        message = message,
        userName = user.name,
        userRating = user.rating,
        userAvatar = user.avatar,
        createDate = createDate?.toKotlinLocalDateTime(),
        section = section,
    )
}
