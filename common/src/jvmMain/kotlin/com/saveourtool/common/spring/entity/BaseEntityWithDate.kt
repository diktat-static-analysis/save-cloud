package com.saveourtool.common.spring.entity

import com.saveourtool.common.listeners.DateListener
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.EntityListeners
import javax.persistence.MappedSuperclass

/**
 * base class for all entities with [createDate] and [updateDate]
 */
@MappedSuperclass
@EntityListeners(DateListener::class)
@Suppress("UnnecessaryAbstractClass")
abstract class BaseEntityWithDate : BaseEntity(), IBaseEntityWithDate {
    /**
     * Create date of entity
     **/
    @Column(name = "create_date")
    override var createDate: LocalDateTime? = null

    /**
     * Update date of entity
     **/
    @Column(name = "update_date")
    override var updateDate: LocalDateTime? = null
}
