package com.saveourtool.common.entitiescosv

import com.saveourtool.common.entities.cosv.CosvFileDto
import com.saveourtool.common.spring.entity.BaseEntity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * Entity for COSV repository
 * @property identifier
 * @property modified
 * @property prevCosvFile
 */
@Entity
@Table(name = "cosv_file")
class CosvFile(
    var identifier: String,
    var modified: LocalDateTime,
    @OneToOne
    @JoinColumn(name = "prev_cosv_file_id")
    var prevCosvFile: CosvFile? = null,
) : BaseEntity() {
    override fun toString(): String = "CosvFile(identifier=$identifier, modified=$modified)"

    /**
     * @return CosvFileDto
     */
    fun toDto() = CosvFileDto(
        id = requiredId(),
        identifier = identifier,
        modified = modified.toKotlinLocalDateTime(),
        prevCosvFileId = prevCosvFile?.requiredId()
    )
}
