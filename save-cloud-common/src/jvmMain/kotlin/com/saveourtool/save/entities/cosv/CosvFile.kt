package com.saveourtool.save.entities.cosv

import com.saveourtool.save.spring.entity.BaseEntity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.OneToOne

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * Entity for COSV repository
 * @property identifier
 * @property modified
 * @property prevCosvFile
 */
@Entity
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
