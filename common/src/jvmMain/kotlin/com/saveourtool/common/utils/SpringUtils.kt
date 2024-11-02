/**
 * This class contains util methods for Spring
 */

package com.saveourtool.common.utils

import com.saveourtool.common.spring.entity.BaseEntity
import com.saveourtool.common.spring.repository.BaseEntityRepository
import com.saveourtool.common.storage.StorageProjectReactor
import io.ktor.http.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.Part
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

typealias ByteBufferFluxResponse = ResponseEntity<Flux<ByteBuffer>>

/**
 * upload [ByteArray] as content
 *
 * @param key a key for provided content
 * @param contentBytes
 * @return count of written bytes
 */
fun <K : Any> StorageProjectReactor<K>.uploadAndReturnContentSize(key: K, contentBytes: ByteArray): Mono<Long> =
        doUpload(key, contentBytes).map { it.second }

/**
 * upload [ByteArray] as content
 *
 * @param key a key for provided content
 * @param contentBytes
 * @return updated key [K]
 */
fun <K : Any> StorageProjectReactor<K>.upload(key: K, contentBytes: ByteArray): Mono<K> =
        doUpload(key, contentBytes).map { it.first }

/**
 * overwrite with [Part] as content
 *
 * @param key a key for provided content
 * @param content
 * @param contentLength
 * @return [Mono] with overwritten key [K]
 */
fun <K> StorageProjectReactor<K>.overwrite(key: K, content: Part, contentLength: Long): Mono<K> = content.content()
    .map { it.asByteBuffer() }
    .let { overwrite(key, contentLength, it) }

/**
 * overwrite [ByteArray] as content
 *
 * @param key a key for provided content
 * @param contentBytes
 * @return count of written bytes
 */
fun <K : Any> StorageProjectReactor<K>.overwrite(key: K, contentBytes: ByteArray): Mono<Long> = contentBytes.size.toLong()
    .let { contentLength ->
        overwrite(key, contentLength, Flux.just(ByteBuffer.wrap(contentBytes))).thenReturn(contentLength)
    }

/**
 * @receiver repository for [T]
 * @param id ID of [T]
 * @return [T] found by [id] in [R] or response exception with status [org.springframework.http.HttpStatus.NOT_FOUND]
 */
inline fun <reified T : BaseEntity, R : BaseEntityRepository<T>> R.getByIdOrNotFound(id: Long): T = findByIdOrNull(id).orNotFound {
    "Not found ${T::class.simpleName} by id = $id"
}

/**
 * Returns a [Page] of entities matching the given [Specification].
 *
 * @param spec can be null.
 * @param pageable must not be null.
 * @return never null.
 */
fun <T : Any> JpaSpecificationExecutor<T>.kFindAll(pageable: Pageable, spec: Specification<T>?): Page<T> = findAll(spec, pageable)

private fun <K : Any> StorageProjectReactor<K>.doUpload(key: K, contentBytes: ByteArray) = contentBytes.size.toLong()
    .let { contentLength ->
        upload(key, contentLength, Flux.just(ByteBuffer.wrap(contentBytes)))
            .map { it to contentLength }
    }

/**
 * @param statusCode [HttpStatusCode] that should be set to [StringResponse]
 * @param loggingMethod method that should be used for logging e.g. logger::info
 * @param lazyMessage callback that generates a message
 * @return [StringResponse] filled with [lazyMessage] and [statusCode]
 */
fun logAndRespond(
    statusCode: HttpStatusCode,
    loggingMethod: (String) -> Unit,
    lazyMessage: () -> String,
): StringResponse = lazyMessage().also {
    loggingMethod(it)
}.let {
    ResponseEntity.status(statusCode.value).body(it)
}
