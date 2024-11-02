/**
 * Platform utils
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")
/**
 * Platform dependent utility methods
 */

@file:JvmName("PlatformUtilsJVM")

package com.saveourtool.common.utils

actual fun createAtomicLong(value: Long): AtomicLong = object : AtomicLong {
    private val holder = java.util.concurrent.atomic.AtomicLong(value)
    override fun get(): Long = holder.get()

    override fun set(newValue: Long) = holder.set(newValue)

    override fun addAndGet(delta: Long): Long = holder.addAndGet(delta)
}

actual fun <T> createGenericAtomicReference(valueToStore: T): GenericAtomicReference<T> = object : GenericAtomicReference<T> {
    private val holder: java.util.concurrent.atomic.AtomicReference<T> = java.util.concurrent.atomic.AtomicReference(valueToStore)
    override fun get(): T = holder.get()
    override fun set(newValue: T) {
        holder.set(newValue)
    }
}

actual fun getenv(envName: String): String? = System.getProperty(envName) ?: System.getenv(envName)
