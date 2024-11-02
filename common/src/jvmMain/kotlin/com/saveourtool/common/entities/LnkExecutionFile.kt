package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDto
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property execution execution that is connected to [file]
 * @property file manageable file
 */
@Entity
class LnkExecutionFile(
    @ManyToOne
    @JoinColumn(name = "execution_id")
    var execution: Execution,

    @ManyToOne
    @JoinColumn(name = "file_id")
    var file: File,
) : BaseEntityWithDto<LnkExecutionFileDto>() {
    override fun toDto() = LnkExecutionFileDto(
        execution.toDto(),
        file.toDto(),
    )
}
