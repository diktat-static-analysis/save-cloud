package com.saveourtool.common.spring.entity

import com.saveourtool.common.entities.DtoWithId

/**
 * base class for all entities with DTO with [D] type where DTO has ID
 */
abstract class BaseEntityWithDtoWithId<D : DtoWithId> : BaseEntityWithDto<D>(), IBaseEntityWithDtoWithId<D>
