package com.saveourtool.common.entities

import kotlinx.serialization.Serializable

/**
 * base class for all DTOs with ID
 */
@Serializable
abstract class DtoWithId {
    /**
     * a unique id
     */
    abstract val id: Long?

    /**
     * @return non-nullable [id]
     */
    fun requiredId(): Long = requireNotNull(id) {
        "Entity is not saved yet: $this"
    }
}
