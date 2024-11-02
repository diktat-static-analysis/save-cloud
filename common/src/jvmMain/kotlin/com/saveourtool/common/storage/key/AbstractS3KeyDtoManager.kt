package com.saveourtool.common.storage.key

import com.saveourtool.common.entities.DtoWithId
import com.saveourtool.common.spring.entity.BaseEntityWithDto
import com.saveourtool.common.spring.repository.BaseEntityRepository
import com.saveourtool.common.utils.BlockingBridge
import com.saveourtool.common.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull

/**
 * Implementation of [S3KeyManager] which uses DTO [K] (for entity [E]) as keys
 *
 * @param prefix a common prefix for all keys in S3 storage for this storage
 * @param repository repository for [E] which is entity for [K]
 * @param blockingBridge
 */
abstract class AbstractS3KeyDtoManager<K : DtoWithId, E : BaseEntityWithDto<K>, R : BaseEntityRepository<E>>(
    prefix: String,
    repository: R,
    blockingBridge: BlockingBridge,
) : AbstractS3KeyDatabaseManager<K, E, R>(prefix, repository, blockingBridge) {
    override fun E.toKey(): K = toDto()

    override fun K.toEntity(): E = createNewEntityFromDto(this)

    override fun findEntity(key: K): E? = key.id
        ?.let { id ->
            repository.findByIdOrNull(id)
                .orNotFound { "Failed to find entity for ${this.javaClass.simpleName} by id = $id" }
        }
        ?: findByDto(key)

    /**
     * @param dto
     * @return [E] entity found by [K] dto or null
     */
    protected abstract fun findByDto(dto: K): E?

    /**
     * @param dto
     * @return a new [E] entity is created from provided [K] dto
     */
    abstract fun createNewEntityFromDto(dto: K): E
}
