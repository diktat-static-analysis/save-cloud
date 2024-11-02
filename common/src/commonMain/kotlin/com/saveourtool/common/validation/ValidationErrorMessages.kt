/**
 * Module that contains different error messages.
 */

@file:JsExport

package com.saveourtool.common.validation

import kotlin.js.JsExport

/**
 * Error message that is shown when email input is invalid.
 */
const val EMAIL_ERROR_MESSAGE = "Please input a valid email address"

/**
 * Error message that is shown when url input is invalid.
 */
const val URL_ERROR_MESSAGE = "Please input a valid URL that starts either with [http://] or [https://], " +
        "ends with a top-level domain name with more than one character."

/**
 * Error message that is shown when name input is invalid.
 */
const val NAME_ERROR_MESSAGE = "Please input a name (not longer than $NAMING_MAX_LENGTH symbols) that contains " +
        "only English letters, numbers; dots and hyphens (but not the first and the last characters)"

/**
 * Error message for commit hash
 */
const val COMMIT_HASH_ERROR_MESSAGE = "Please input a valid commit hash"

/**
 * Error message that is shown when date range input is invalid.
 */
const val DATE_RANGE_ERROR_MESSAGE = "Please input a valid date range"

/**
 * Error message that is shown when CVE identifier is invalid.
 */
const val CVE_NAME_ERROR_MESSAGE = "CVE identifier is invalid"

/**
 * Error message that is shown when tag is invalid.
 */
const val TAG_ERROR_MESSAGE = "Tag length should be in [2, 15] range, no commas are allowed."

/**
 * Error message that is shown when severity score vector is invalid.
 */
const val SEVERITY_VECTOR_ERROR_MESSAGE = "Severity score vector is invalid"
