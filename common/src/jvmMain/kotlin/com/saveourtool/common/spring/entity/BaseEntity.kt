package com.saveourtool.common.spring.entity

import com.saveourtool.common.listeners.DateListener
import com.saveourtool.common.validation.Validatable
import javax.persistence.*

/**
 * base class for all entities
 */
@MappedSuperclass
@EntityListeners(DateListener::class)
@Suppress("CLASS_SHOULD_NOT_BE_ABSTRACT", "UnnecessaryAbstractClass")
abstract class BaseEntity : Validatable {
    /**
     * generate a unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    /**
     * @return [id] as not null with validating
     * @throws IllegalArgumentException when [id] is not set that means entity is not saved yet
     */
    open fun requiredId(): Long = requireNotNull(id) {
        "Entity is not saved yet: $this"
    }
}
