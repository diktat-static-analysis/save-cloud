/**
 * Module with defined constants
 */

@file:JsExport

package com.saveourtool.common.utils

import kotlin.js.JsExport

/**
 * Link to save-cloud on GitHub
 */
const val SAVE_CLOUD_GITHUB_URL = "https://github.com/saveourtool/save-cloud"

/**
 * Delimiter used in database
 */
const val DATABASE_DELIMITER = ","

/**
 * Delimiter used to combine a list to show to user
 */
const val PRETTY_DELIMITER = ", "

/**
 * Delimiter used in URL path
 */
const val URL_PATH_DELIMITER = "/"

/**
 * Period in ms for debounce on frontend
 */
const val DEFAULT_DEBOUNCE_PERIOD = 250

/**
 * Period in ms for ace editor debouncing
 */
const val DEBOUNCE_PERIOD_FOR_EDITORS = 250

/**
 * Number of characters of git commit hash that should be displayed
 */
const val GIT_HASH_PREFIX_LENGTH = 6

/**
 * Part name for multipart-data uploading a file
 */
const val FILE_PART_NAME = "file"

/**
 * A custom header for `Content-Length`
 */
const val CONTENT_LENGTH_CUSTOM = "Content-Length-Custom"

/**
 * `X-Authorization-Roles` used to specify SAVE user id
 */
const val AUTHORIZATION_ID = "X-Authorization-Id"

/**
 * `X-Authorization-Status` used to specify SAVE user status
 */
const val AUTHORIZATION_STATUS = "X-Authorization-Status"

/**
 * `X-Authorization-Roles` used to specify SAVE username
 */
const val AUTHORIZATION_NAME = "X-Authorization-Name"

/**
 * `X-Authorization-Roles` used to specify SAVE user roles
 */
const val AUTHORIZATION_ROLES = "X-Authorization-Roles"

/**
 * An attribute to store save's user id
 */
const val SAVE_USER_ID_ATTRIBUTE = "save-user-id"

/**
 * Default time to execute setup.sh
 */
@Suppress("NON_EXPORTABLE_TYPE")
const val DEFAULT_SETUP_SH_TIMEOUT_MILLIS: Long = 60_000L

/**
 * directory with the default avatars package
 */
const val AVATARS_PACKS_DIR: String = "/img/avatar_packs"

/**
 * `NO-BREAK SPACE` (U+00A0).
 */
@Suppress("NON_EXPORTABLE_TYPE")
const val NO_BREAK_SPACE = '\u00a0'

/**
 * Horizontal Ellipsis (U+2026).
 */
@Suppress("NON_EXPORTABLE_TYPE")
const val ELLIPSIS = '\u2026'
