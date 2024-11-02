package com.saveourtool.common.listeners

import com.saveourtool.common.spring.entity.BaseEntity
import com.saveourtool.common.spring.entity.IBaseEntityWithDate
import java.time.LocalDateTime
import javax.persistence.PrePersist
import javax.persistence.PreUpdate

/**
 * JPA listener which sets [IBaseEntityWithDate.createDate] and [IBaseEntityWithDate.updateDate]
 */
class DateListener {
    @PrePersist
    private fun beforeSave(entity: BaseEntity) {
        (entity as? IBaseEntityWithDate)?.apply {
            val date = LocalDateTime.now()
            createDate = date
            updateDate = date
        }
    }

    @PreUpdate
    private fun beforeUpdate(entity: BaseEntity) {
        (entity as? IBaseEntityWithDate)?.apply {
            updateDate = LocalDateTime.now()
        }
    }
}
