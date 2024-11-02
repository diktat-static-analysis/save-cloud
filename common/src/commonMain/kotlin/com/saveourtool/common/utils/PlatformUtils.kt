/**
 * Platform dependent utility methods
 */

package com.saveourtool.common.utils

import com.saveourtool.save.core.logging.logDebug

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlinx.datetime.Clock

/**
 * Atomic values
 */
interface AtomicLong {
    /**
     * @return value
     */
    fun get(): Long

    /**
     * @param newValue
     */
    fun set(newValue: Long)

    /**
     * @param delta increments the value_ by delta
     * @return the new value
     */
    fun addAndGet(delta: Long): Long
}

/**
 * Class that holds value and shares atomic reference to the value
 */
interface GenericAtomicReference<T> {
    /**
     * @return stored value
     */
    fun get(): T

    /**
     * @param newValue new value to store
     */
    fun set(newValue: T)
}

/**
 * A wrapper around a value of type [T] that caches it for [expirationTimeSeconds] and then recalculates
 * using [valueGetter]
 *
 * @param expirationTime value expiration time
 * @param valueGetter a function to calculate the value of type [T]
 */
class ExpiringValueWrapper<T : Any>(
    expirationTime: Duration,
    private val valueGetter: () -> T,
) {
    private val expirationTimeSeconds = expirationTime.toLong(DurationUnit.SECONDS)
    private val lastUpdateTimeSeconds = createAtomicLong(0)
    private val value: GenericAtomicReference<T> = createGenericAtomicReference(valueGetter())

    /**
     * @return cached value or refreshes the value and returns it
     */
    fun getValue(): T {
        val current = Clock.System.now().epochSeconds
        if (current - lastUpdateTimeSeconds.get() > expirationTimeSeconds) {
            value.set(valueGetter())
            lastUpdateTimeSeconds.set(current)
        }
        return value.get()
    }
}

/**
 * @param value
 * @return [AtomicLong] with initial value [value]
 */
expect fun createAtomicLong(value: Long): AtomicLong

/**
 * @param valueToStore
 * @return create [GenericAtomicReference] with initial value [valueToStore]
 */
expect fun <T> createGenericAtomicReference(valueToStore: T): GenericAtomicReference<T>

/**
 * @param envName
 * @return env variable name
 */
expect fun getenv(envName: String): String?

/**
 * Get value of environment variable [envName] or throw if it is not set.
 *
 * @param envName name of the environment variable
 * @return value of the environment variable
 */
fun requiredEnv(envName: String): String = requireNotNull(getenv(envName)) {
    "Environment variable $envName is not set but is required"
}.toString()

/**
 * Get value of environment variable [envName] or null.
 *
 * @param envName name of the optional environment variable
 * @return value of the optional environment variable or null
 */
fun optionalEnv(envName: String): String? = getenv(envName)
    .also {
        it ?: logDebug("Optional environment variable $envName is not provided")
    }
    ?.toString()
