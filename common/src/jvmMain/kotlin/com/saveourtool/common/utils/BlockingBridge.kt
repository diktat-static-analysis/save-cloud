package com.saveourtool.common.utils

import org.jetbrains.annotations.NonBlocking
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A bridge for blocking (IO) operations
 *
 * @property ioScheduler [Scheduler] for IO operations for [Mono] and [Flux]
 * @property ioDispatcher [CoroutineDispatcher] for IO operations in suspend function
 */
@Component
class BlockingBridge(
    val ioScheduler: Scheduler = Schedulers.boundedElastic(),
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Taking from https://projectreactor.io/docs/core/release/reference/#faq.wrap-blocking
     *
     * @param supplier blocking operation like JDBC
     * @return [Mono] from result of blocking operation [T]
     * @see blockingToFlux
     */
    @NonBlocking
    fun <T : Any> blockingToMono(supplier: () -> T?): Mono<T> = supplier.toMono().subscribeOn(ioScheduler)

    /**
     * @param supplier blocking operation like JDBC
     * @return [Flux] from result of blocking operation [List] of [T]
     * @see blockingToMono
     */
    @NonBlocking
    fun <T> blockingToFlux(supplier: () -> Iterable<T>): Flux<T> = blockingToMono(supplier).flatMapIterable { it }

    /**
     * @param supplier blocking operation like JDBC
     * @return suspend result of blocking operation [T]
     * @see blockingToMono
     */
    @NonBlocking
    suspend fun <T> blockingToSuspend(supplier: () -> T): T = withContext(ioDispatcher) {
        supplier()
    }

    companion object {
        /**
         * A default instance of [BlockingBridge]
         */
        val default: BlockingBridge = BlockingBridge()
    }
}
