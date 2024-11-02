/**
 * Utility methods related to a Date and Time
 */

package com.saveourtool.common.utils

import io.ktor.util.*
import kotlin.time.Duration
import kotlinx.datetime.*

private const val MILLS_IN_NANOS = 1_000_000

/**
 * @return [Instant] from epoch time in mills
 */
fun Long.millisToInstant(): Instant = Instant.fromEpochMilliseconds(this)

/**
 * @return [Instant] from epoch time in seconds
 */
fun Long.secondsToInstant(): Instant = Instant.fromEpochSeconds(this)

/**
 * @param timeZone timezone to print the date time in
 * @return pretty string representation of [LocalDateTime]
 */
fun LocalDateTime.prettyPrint(timeZone: TimeZone = TimeZone.UTC) = toInstant(TimeZone.UTC).toLocalDateTime(timeZone)
    .toString()
    .replace("T", " ")
    .replace("Z", "")
    .replace("-", ".")

/**
 * @param timeZone timezone to print the date time in
 * @param isSeconds flag for added seconds
 * @return string representation of [LocalDateTime] in [Thursday, 1 Jan 1970 00:00] format
 */
fun LocalDateTime.toUnixCalendarFormat(timeZone: TimeZone = TimeZone.UTC, isSeconds: Boolean = false) = toInstant(TimeZone.UTC).toLocalDateTime(timeZone)
    .let {
        "${it.dayOfWeek.name.toLowerCaseWithFirstCharUpperCase()}, ${it.dayOfMonth} ${it.month.name.toLowerCaseWithFirstCharUpperCase()} ${it.year} ${it.hour.plusZero()}:${it
            .minute.plusZero()}${if (isSeconds)":${it.second.plusZero()}" else ""}"
    }

/**
 * @param duration
 */
operator fun LocalDateTime.plus(duration: Duration) = toInstant(TimeZone.UTC).plus(duration).toLocalDateTime(TimeZone.UTC)

/**
 * @param duration
 */
operator fun LocalDateTime.minus(duration: Duration) = toInstant(TimeZone.UTC).minus(duration).toLocalDateTime(TimeZone.UTC)

/**
 * @return [LocalDateTime] truncated to mills
 */
fun LocalDateTime.truncatedToMills(): LocalDateTime = LocalDateTime(
    year, month, dayOfMonth, hour, minute, second, nanosecond / MILLS_IN_NANOS * MILLS_IN_NANOS
)

@Suppress(
    "MAGIC_NUMBER",
    "MagicNumber",
)
private fun Int.plusZero(): String = this.let { if (it < 10) "0$it" else it }.toString()

private fun String.toLowerCaseWithFirstCharUpperCase() = this.toLowerCasePreservingASCIIRules().replaceFirstChar { char -> char.titlecase() }

/**
 * @return current local date-time in UTC timezone
 */
fun getCurrentLocalDateTime(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
