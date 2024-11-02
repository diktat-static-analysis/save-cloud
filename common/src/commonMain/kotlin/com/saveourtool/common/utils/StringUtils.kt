@file:JvmName("StringUtils")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.common.utils

import kotlin.jvm.JvmName

const val LOGIN_MAX_LENGTH = 14
const val REAL_NAME_PART_MAX_LENGTH = 20
const val URL_MAX_LENGTH = 28

/**
 * @return short [String] for list values
 */
fun List<String>?.listToShortString(): String = this?.run {
    if (size <= 2) {
        this.joinToString(PRETTY_DELIMITER)
    } else {
        "${first()} ... ${last()}"
    }
}.orEmpty()

/**
 * If necessary, shortens the receiver to have the maximum length of
 * [maxLength], replacing the last character with [ELLIPSIS].
 *
 * @receiver the string to truncate.
 * @param maxLength the maximum length the truncated string should have.
 * @return the truncated string.
 * @see ELLIPSIS
 */
fun String.shorten(maxLength: Int): String {
    require(maxLength >= 1) {
        "maxLength should be >= 1: $maxLength"
    }

    return when {
        length <= maxLength -> this
        else -> asSequence().joinToString(
            separator = "",
            limit = maxLength - 1,
            truncated = ELLIPSIS.toString(),
        )
    }
}

/**
 * @return short [String] for login
 */
fun String.shortenLogin(): String = shorten(LOGIN_MAX_LENGTH)

/**
 * @return short [String] for real name
 */
fun String.shortenRealName(): String = shorten(REAL_NAME_PART_MAX_LENGTH)

/**
 * @return short [String] for urls
 */
fun String.shortenUrl(): String = shorten(URL_MAX_LENGTH)
