package com.saveourtool.common.entities

import com.saveourtool.common.entities.contest.ContestSampleFieldDto
import com.saveourtool.common.entities.contest.ContestSampleFieldType
import com.saveourtool.common.spring.entity.BaseEntityWithDateAndDto
import javax.persistence.*

/**
 * @property contestSample contest sample
 * @property name name of field
 * @property type type of field
 * @property userId creator contestSampleField
 */
@Entity
class ContestSampleField(

    @ManyToOne
    @JoinColumn(name = "contest_sample_id")
    var contestSample: ContestSample,

    var name: String,

    @Enumerated(EnumType.STRING)
    var type: ContestSampleFieldType,

    var userId: Long,

) : BaseEntityWithDateAndDto<ContestSampleFieldDto>() {
    override fun toDto() = ContestSampleFieldDto(
        name = name,
        type = type,
    )
}
