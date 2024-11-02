package com.saveourtool.common.storage.key

import com.saveourtool.common.spring.entity.BaseEntity
import com.saveourtool.common.spring.repository.BaseEntityRepository
import com.saveourtool.common.utils.BlockingBridge
import com.saveourtool.common.utils.orNotFound

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional

/**
 * Implementation of [S3KeyManager] which stores keys in database
 *
 * @param prefix a common prefix for all keys in S3 storage for this storage
 * @property repository repository for [E]
 * @property blockingBridge
 */
abstract class AbstractS3KeyDatabaseManager<K : Any, E : BaseEntity, R : BaseEntityRepository<E>>(
    prefix: String,
    protected val repository: R,
    open val blockingBridge: BlockingBridge,
) : S3KeyManager<K> {
    /**
     * [S3KeyManager] with [Long] as key (it's [ID][com.saveourtool.save.spring.entity.BaseEntity.requiredId])
     */
    private val entityIdKeyManager: S3KeyManager<Long> = object : AbstractS3KeyManager<Long>(prefix) {
        override fun buildKeyFromSuffix(s3KeySuffix: String): Long = s3KeySuffix.toLong()
        override fun buildS3KeySuffix(key: Long): String = key.toString()
    }
    override val commonPrefix: String = entityIdKeyManager.commonPrefix

    /**
     * @return a key [K] created from receiver entity [E]
     */
    protected abstract fun E.toKey(): K

    /**
     * @return an entity [E] created from receiver key [K]
     */
    protected abstract fun K.toEntity(): E

    /**
     * @param key
     * @return [E] entity found by [K] key or null
     */
    protected abstract fun findEntity(key: K): E?

    @Transactional
    override fun delete(key: K) {
        findEntity(key)?.let { entity ->
            beforeDelete(entity)
            repository.delete(entity)
        }
    }

    @Transactional
    override fun deleteAll(keys: Collection<K>) {
        keys.mapNotNull { findEntity(it) }
            .onEach { beforeDelete(it) }
            .let { repository.deleteAll(it) }
    }

    /**
     * @receiver [E] entity which needs to be processed before deletion
     * @param entity
     */
    protected open fun beforeDelete(entity: E): Unit = Unit

    @Transactional
    override fun createNewS3Key(key: K): String {
        val entity = repository.save(key.toEntity())
        return entityIdKeyManager.createNewS3Key(entity.requiredId())
    }

    override fun findExistedS3Key(key: K): String? = findEntity(key)?.let { entity -> entityIdKeyManager.findExistedS3Key(entity.requiredId()) }

    override fun findKey(s3Key: String): K? {
        val entityId = entityIdKeyManager.findKey(s3Key).orNotFound {
            "Cannot extract entity id from s3 key: $s3Key"
        }
        return repository.findByIdOrNull(entityId)
            .orNotFound {
                "Not found entity by id $entityId"
            }
            .toKey()
    }

    /**
     * @param id
     * @return [K] for [E] with provided [id]
     */
    open fun findKeyByEntityId(
        id: Long,
    ): K? = repository.findByIdOrNull(id)?.toKey()
}
