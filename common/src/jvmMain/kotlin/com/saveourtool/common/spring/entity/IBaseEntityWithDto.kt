@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.common.spring.entity

/**
 * base class for all entities with DTO with [D] type
 */
@Suppress("CLASS_NAME_INCORRECT")
interface IBaseEntityWithDto<D : Any> {
    /**
     * @return DTO [D] for current entity
     */
    fun toDto(): D
}
