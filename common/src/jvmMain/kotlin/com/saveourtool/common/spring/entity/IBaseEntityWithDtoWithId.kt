@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.common.spring.entity

import com.saveourtool.common.entities.DtoWithId

/**
 * base class for all entities with DTO with [D] type where DTO has ID
 */
@Suppress("CLASS_NAME_INCORRECT")
interface IBaseEntityWithDtoWithId<D : DtoWithId> : IBaseEntityWithDto<D>
