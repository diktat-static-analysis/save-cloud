/**
 * Platform dependent utility methods
 */

package com.saveourtool.common.utils

actual fun createAtomicLong(value: Long): AtomicLong = object : AtomicLong {
    override fun get(): Long = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)

    override fun set(newValue: Long) = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)

    override fun addAndGet(delta: Long): Long = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
}

actual fun <T> createGenericAtomicReference(valueToStore: T): GenericAtomicReference<T> = object : GenericAtomicReference<T> {
    override fun get(): T = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)

    override fun set(newValue: T) = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
}

actual fun getenv(envName: String): String? = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
