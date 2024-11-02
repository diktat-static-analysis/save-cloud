package com.saveourtool.common.storage

import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.utils.info
import com.saveourtool.common.utils.isNotNull
import io.ktor.client.utils.*

import org.slf4j.Logger
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import kotlin.reflect.KClass
import kotlinx.coroutines.*
import kotlinx.coroutines.reactor.asCoroutineDispatcher

/**
 * Initializer for [StorageProjectReactor]
 *
 * @param storageName
 */
class StorageInitializer(
    private val storageName: String,
) {
    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isInitStarted = AtomicBoolean(false)

    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isInitFinishedCount = AtomicInteger(2)

    /**
     * @param clazz [storageName] will be calculated from class name
     */
    constructor(clazz: KClass<*>) : this(clazz.simpleName ?: clazz.java.simpleName)

    /**
     * @return true if initializer is done already
     */
    fun isDone(): Boolean = isInitStarted.get() && isInitFinishedCount.get() == 0

    /**
     * Init method using method that returns [Mono] and suspend function together
     * Both function can be empty
     *
     * @param doInitReactively
     * @param doInitSuspendedly
     */
    fun init(
        doInitReactively: () -> Mono<Unit> = { Mono.empty() },
        doInitSuspendedly: suspend () -> Unit? = { null },
    ) {
        require(!isInitStarted.compareAndExchange(false, true)) {
            "Init method cannot be called more than 1 time, initialization is in progress"
        }
        doInitReactively()
            .map { true }  // doInit worked
            .defaultIfEmpty(false)  // doInit is emtpy
            .doOnNext { wasDoInitCalled ->
                require(isInitFinishedCount.decrementAndGet() >= 0) {
                    "Init method cannot be called more than 1 time. Initialization $storageName already finished by another run"
                }
                if (wasDoInitCalled) {
                    log.info {
                        "Initialization $storageName is done (reactive part)"
                    }
                }
            }
            .subscribeOn(initScheduler)
            .subscribe()

        CoroutineScope(initCoroutineDispatcher).launch {
            val initResult = doInitSuspendedly()
            require(isInitFinishedCount.decrementAndGet() >= 0) {
                "Init method cannot be called more than 1 time. Initialization $storageName already finished by another run"
            }
            if (initResult.isNotNull()) {
                log.info {
                    "Initialization $storageName is done (suspending part)"
                }
            }
        }
    }

    /**
     * Init method using method that returns [Mono]
     * It can be empty
     *
     * @param doInit
     */
    fun initReactively(doInit: () -> Mono<Unit>): Unit = init(doInitReactively = doInit)

    /**
     * Init method using suspend method that returns [Unit]
     * It can be empty
     *
     * @param doInit
     */
    fun initSuspendedly(doInit: suspend () -> Unit?): Unit = init(doInitSuspendedly = doInit)

    /**
     * @param action
     * @return result of [action] if initialization is finished, otherwise -- exception
     */
    fun <R> validateAndRun(action: () -> R): R {
        require(isInitFinishedCount.get() == 0) {
            "Any method of $storageName should be called after init method is finished"
        }
        return action()
    }

    /**
     * @param action
     * @return result of [action] if initialization is finished, otherwise -- exception
     */
    suspend fun <R> validateAndRunSuspend(action: suspend () -> R): R {
        require(isInitFinishedCount.get() == 0) {
            "Any method of $storageName should be called after init method is finished"
        }
        return action()
    }

    companion object {
        private val log: Logger = getLogger<StorageInitializer>()
        private val initScheduler: Scheduler = Schedulers.newBoundedElastic(5, Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE, "storage-init-")
        private val initCoroutineDispatcher: CoroutineDispatcher = initScheduler.asCoroutineDispatcher()
    }
}
