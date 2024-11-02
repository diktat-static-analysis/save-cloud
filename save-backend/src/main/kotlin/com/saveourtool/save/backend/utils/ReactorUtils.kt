/**
 * Utility methods for working with Reactor publishers
 */

package com.saveourtool.save.backend.utils

import com.saveourtool.common.utils.collectToInputStream
import com.saveourtool.common.utils.switchIfEmptyToNotFound

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import java.nio.ByteBuffer
import java.util.Optional

/**
 * Same as [Flux.filter], but calls [onExclude] for every value not matching [predicate]
 *
 * @param onExclude
 * @param predicate
 * @return same as [Flux.filter]
 */
inline fun <T> Flux<T>.filterAndInvoke(crossinline onExclude: (T) -> Unit, crossinline predicate: (T) -> Boolean): Flux<T> = filter { value ->
    predicate(value).also {
        if (!it) {
            onExclude(value)
        }
    }
}

/**
 * Same as [Flux.filterWhen], but calls [onExclude] for every value not matching async [predicate]
 *
 * @param onExclude
 * @param predicate
 * @return Same as [Flux.filterWhen]
 */
inline fun <T> Flux<T>.filterWhenAndInvoke(crossinline onExclude: (T) -> Unit, crossinline predicate: (T) -> Mono<Boolean>): Flux<T> = filterWhen { value ->
    predicate(value).doOnNext {
        if (!it) {
            onExclude(value)
        }
    }
}

/**
 * @param objectMapper
 * @return convert [Flux] of [ByteBuffer] to object of [T] from Json string using [ObjectMapper]
 */
inline fun <reified T> Flux<ByteBuffer>.collectAsJsonTo(objectMapper: ObjectMapper): Mono<T> = this
    .collectToInputStream()
    .map { objectMapper.readValue(it, T::class.java) }

/**
 * @return [Mono] with original value or with [ResponseEntity] with [HttpStatus.FORBIDDEN]
 */
fun <T> Mono<ResponseEntity<T>>.forbiddenIfEmpty() = defaultIfEmpty(ResponseEntity.status(HttpStatus.FORBIDDEN).build())

/**
 * @param message
 * @return [Mono] containing current object or [Mono.error] with 404 status otherwise
 */
fun <T : Any> T?.toMonoOrNotFound(message: String? = null) = toMono<T>().switchIfEmptyToNotFound {
    message
}

/**
 * @param data
 * @param message
 * @return [Mono] containing [data] or [Mono.error] with 404 status otherwise
 */
fun <T> justOrNotFound(data: Optional<T>, message: String? = null) = Mono.justOrEmpty(data).switchIfEmptyToNotFound {
    message
}
