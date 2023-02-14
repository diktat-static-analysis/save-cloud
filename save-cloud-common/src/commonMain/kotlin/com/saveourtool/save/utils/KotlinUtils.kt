/**
 * Utilities for Kotlin
 */

package com.saveourtool.save.utils

import kotlinx.coroutines.delay

typealias StringList = List<String>

/**
 * Run [action] several [times] with [timeMillis] milliseconds
 *
 * [T] is just a non-nullable type
 *
 * @param times number of times to retry [action]
 * @param timeMillis number of milliseconds to wait until next retry
 * @param action action that should be invoked
 * @return [T] if the result was fetched in [times] attempts, null otherwise
 */
suspend fun <T : Any> retry(
    times: Int,
    timeMillis: Long? = 10_000L,
    action: () -> T?,
): T? = action() ?: run {
    if (times > 0) {
        timeMillis?.let { delay(timeMillis) }
        retry(times - 1, timeMillis, action)
    } else {
        null
    }
}
