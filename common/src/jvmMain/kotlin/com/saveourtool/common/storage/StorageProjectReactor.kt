package com.saveourtool.common.storage

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.time.Instant

/**
 * Base interface for Storage using [Mono] and [Flux] from [Project Reactor](https://projectreactor.io/)
 *
 * @param K type of key
 */
interface StorageProjectReactor<K> {
    /**
     * @return true if init is done already
     */
    fun isInitDone(): Boolean = true

    /**
     * @return list of keys in storage
     */
    fun list(): Flux<K>

    /**
     * @param key a key to be checked
     * @return true if the key exists, otherwise false
     */
    fun doesExist(key: K): Mono<Boolean>

    /**
     * @param key a key to be checked
     * @return content size in bytes
     */
    fun contentLength(key: K): Mono<Long>

    /**
     * @param key a key to be checked
     * @return when a key was modified last time
     */
    fun lastModified(key: K): Mono<Instant>

    /**
     * @param key a key to be deleted
     * @return true if the object deleted, otherwise false
     */
    fun delete(key: K): Mono<Boolean>

    /**
     * @param keys keyes to be deleted
     * @return true if all objects deleted, otherwise false
     */
    fun deleteAll(keys: Collection<K>): Mono<Boolean>

    /**
     * @param key a key for provided content
     * @param content
     * @return [Mono] with uploaded key [K]
     */
    fun upload(key: K, content: Flux<ByteBuffer>): Mono<K>

    /**
     * @param key a key for provided content
     * @param contentLength a content length of content
     * @param content as [Flux] of [ByteBuffer]
     * @return [Mono] with uploaded key [K]
     */
    fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<K>

    /**
     * @param key a key for provided content
     * @param content
     * @return [Mono] with overwritten key [K]
     */
    fun overwrite(key: K, content: Flux<ByteBuffer>): Mono<K> = delete(key)
        .flatMap { upload(key, content) }

    /**
     * @param key a key for provided content
     * @param contentLength a content length of content
     * @param content
     * @return [Mono] with overwritten key [K]
     */
    fun overwrite(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<K> = delete(key)
        .flatMap { upload(key, contentLength, content) }

    /**
     * @param key a key to download content
     * @return downloaded content
     */
    fun download(key: K): Flux<ByteBuffer>

    /**
     * @param source a key of source
     * @param target a key of target
     * @return true if the [source] deleted, otherwise false
     */
    fun move(source: K, target: K): Mono<Boolean>
}
