package com.saveourtool.common.storage.key

import com.saveourtool.common.spring.entity.BaseEntity
import com.saveourtool.common.spring.repository.BaseEntityRepository
import com.saveourtool.common.utils.BlockingBridge
import com.saveourtool.common.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull

/**
 * Implementation of [S3KeyManager] uses entity [E] as key
 *
 * @param prefix a common prefix for all keys in S3 storage for this storage
 * @param repository repository for [E]
 * @param blockingBridge
 */
abstract class AbstractS3KeyEntityManager<E : BaseEntity, R : BaseEntityRepository<E>>(
    prefix: String,
    repository: R,
    blockingBridge: BlockingBridge,
) : AbstractS3KeyDatabaseManager<E, E, R>(
    prefix,
    repository,
    blockingBridge,
) {
    override fun E.toKey(): E = this

    override fun E.toEntity(): E = this

    override fun findEntity(key: E): E? = key.id
        ?.let { id ->
            repository.findByIdOrNull(id)
                .orNotFound { "Failed to find entity for ${this.javaClass.simpleName} by id = $id" }
        }
        ?: findByContent(key)

    /**
     * @param key
     * @return [E] entity found in database by [E] or null
     */
    protected abstract fun findByContent(key: E): E?
}
