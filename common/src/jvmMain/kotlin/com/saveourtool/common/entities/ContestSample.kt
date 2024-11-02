package com.saveourtool.common.entities

import com.saveourtool.common.entities.contest.ContestSampleDto
import com.saveourtool.common.spring.entity.BaseEntityWithDateAndDto
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property name name of contest sample
 * @property description description of contest sample
 * @property user creator contestSample
 */
@Entity
class ContestSample(

    var name: String,

    var description: String?,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

) : BaseEntityWithDateAndDto<ContestSampleDto>() {
    override fun toDto() = ContestSampleDto(
        id = requiredId(),
        name = name,
        description = description,
    )
}
