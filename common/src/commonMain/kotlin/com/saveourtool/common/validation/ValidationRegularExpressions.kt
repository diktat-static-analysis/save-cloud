/**
 * Module that implements Validation with regular expression
 */

package com.saveourtool.common.validation

/**
 * URL name fragment class.
 */
const val NAME_FRAGMENT_CLASS = """[-a-zA-Z\d@:%._\\+~#?&/=]"""

/**
 * @property value [Regex] that is used during validation
 */
enum class ValidationRegularExpressions(val value: Regex) {
    ABSOLUTE_PATH_VALIDATOR("(/[^/ \\n]+/?)*[^/ \\n]*".toRegex()),

    EMAIL_VALIDATOR("^\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*\$".toRegex()),

    RELATIVE_PATH_VALIDATOR("([^/ \\n]+/?)*[^/ \\n]+".toRegex()),

    URL_VALIDATOR("""https?://(?:www\.)?$NAME_FRAGMENT_CLASS{2,256}\.[a-z]{2,6}\b$NAME_FRAGMENT_CLASS*""".toRegex()),
    ;
}
