/**
 * Utility methods related to a Date and Time in JVM
 */

package com.saveourtool.common.utils

import java.time.LocalDateTime
import java.time.ZoneOffset

import kotlinx.datetime.toJavaInstant

/**
 * Function to convert long number of seconds to LocalDateTime
 *
 * @return an instance of [LocalDateTime]
 */
@Suppress("FUNCTION_NAME_INCORRECT_CASE")
fun Long.secondsToJLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(secondsToInstant().toJavaInstant(), ZoneOffset.UTC)
