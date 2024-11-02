package com.saveourtool.common.spring.entity

/**
 * base class for all entities with DTO with [D] type
 */
abstract class BaseEntityWithDto<D : Any> : BaseEntity(), IBaseEntityWithDto<D>
