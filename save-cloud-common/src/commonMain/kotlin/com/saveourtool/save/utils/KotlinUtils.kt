/**
 * Utilities for Kotlin
 */

package com.saveourtool.save.utils

import kotlinx.coroutines.delay

typealias StringList = List<String>

/**
 * Run [action] several [times] with [delayMillis] milliseconds
 * Catches all the exceptions and retries [action] if [times] is not null
 *
 * [T] is just a non-nullable type
 *
 * @param times number of times to retry [action]
 * @param delayMillis number of milliseconds to wait until next retry
 * @param action action that should be invoked
 * @return [T] if the result was fetched in [times] attempts, null otherwise
 */
@Suppress("TooGenericExceptionCaught")
suspend fun <T : Any> retry(
    times: Int,
    delayMillis: Long = 10_000L,
    action: suspend (Int) -> T?,
): Pair<T?, List<Throwable>> {
    val caughtExceptions: MutableList<Throwable> = mutableListOf()
    times.downTo(0).map { iteration ->
        try {
            delay(delayMillis)
            action(iteration)?.let { result ->
                return result to caughtExceptions
            }
        } catch (e: Throwable) {
            caughtExceptions.add(e)
        }
    }
    return null to caughtExceptions
}

/**
 * Run [action] several [times] with [delayMillis] milliseconds **ignoring** error logs
 * Catches all the exceptions and retries [action] if [times] is not null
 *
 * [T] is just a non-nullable type
 *
 * @see retry
 *
 * @param times number of times to retry [action]
 * @param delayMillis number of milliseconds to wait until next retry
 * @param action action that should be invoked
 * @return [T] if the result was fetched in [times] attempts, null otherwise
 */
suspend fun <T : Any> retrySilently(
    times: Int,
    delayMillis: Long = 10_000L,
    action: suspend (Int) -> T?,
): T? = retry(times, delayMillis, action).first
