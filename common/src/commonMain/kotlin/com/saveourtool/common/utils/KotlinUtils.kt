/**
 * Utilities for Kotlin
 */

package com.saveourtool.common.utils

import kotlinx.coroutines.delay

private const val BYTES_COEFFICIENT = 1024

typealias StringList = List<String>

/**
 * @return true if [this] is not null
 */
fun <T : Any> T?.isNotNull(): Boolean = this != null

/**
 * @return converts bytes to kilobytes
 */
fun Long.toKilobytes(): Long = div(BYTES_COEFFICIENT)

/**
 * @return converts bytes to megabytes
 */
fun Double.toMegabytes(): Double = div(BYTES_COEFFICIENT * BYTES_COEFFICIENT)

/**
 * Run [action] several [times] with [delayMillis] milliseconds
 * Catches all the exceptions and retries [action] if [times] is not null
 *
 * [T] is just a non-nullable type
 *
 * @param times number of times to retry [action]
 * @param delayMillis number of milliseconds to wait until next retry
 * @param action action that should be invoked
 * @return Pair where first element is [T] if the result was fetched in [times] attempts, null otherwise,
 *  second element is list of [Throwable] caught in [retry]
 */
@Suppress("TooGenericExceptionCaught")
suspend fun <T : Any> retry(
    times: Int,
    delayMillis: Long = 10_000L,
    action: suspend (Int) -> T?,
): Pair<T?, List<Throwable>> {
    val caughtExceptions: MutableList<Throwable> = mutableListOf()
    times.downTo(1).map { iteration ->
        try {
            action(iteration)?.let { result ->
                return result to caughtExceptions
            }
            delay(delayMillis)
        } catch (e: Throwable) {
            caughtExceptions.add(e)
            delay(delayMillis)
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
