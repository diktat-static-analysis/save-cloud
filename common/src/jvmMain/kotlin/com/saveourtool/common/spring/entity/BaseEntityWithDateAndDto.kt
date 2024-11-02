package com.saveourtool.common.spring.entity

/**
 * base class for all entities with date and DTO with [D] type
 */
abstract class BaseEntityWithDateAndDto<D : Any> : BaseEntityWithDate() {
    /**
     * @return DTO [D] for current entity
     */
    abstract fun toDto(): D
}
