/**
 * Platform dependent utility methods
 */

package com.saveourtool.common.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

actual fun createAtomicLong(value: Long): AtomicLong = object : AtomicLong {
    private val kotlinAtomicLong = kotlin.concurrent.AtomicLong(value)

    override fun get(): Long = kotlinAtomicLong.value

    override fun set(newValue: Long) {
        kotlinAtomicLong.value = newValue
    }

    override fun addAndGet(delta: Long): Long = kotlinAtomicLong.addAndGet(delta)
}

actual fun <T> createGenericAtomicReference(valueToStore: T): GenericAtomicReference<T> = object : GenericAtomicReference<T> {
    private val holder: kotlin.concurrent.AtomicReference<T> = kotlin.concurrent.AtomicReference(valueToStore)
    override fun get(): T = holder.value
    override fun set(newValue: T) {
        holder.value = newValue
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun getenv(envName: String): String? = platform.posix.getenv(envName)?.toKString()
