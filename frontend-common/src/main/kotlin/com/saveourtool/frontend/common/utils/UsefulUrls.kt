package com.saveourtool.frontend.common.utils

import com.saveourtool.common.validation.NAME_FRAGMENT_CLASS
import com.saveourtool.common.validation.ValidationRegularExpressions.URL_VALIDATOR

/**
 * Enum only for storing URLs to well-known website
 *
 * @property basicUrl [String] basic url of a website
 * @property regex [Regex] that is used during validation
 */
enum class UsefulUrls(val basicUrl: String, val regex: Regex) {
    GITEE("https://gitee.com/", """https?://(?:www\.)?gitee.com\b$NAME_FRAGMENT_CLASS*""".toRegex()),
    GITHUB("https://github.com/", """https?://(?:www\.)?github.com\b$NAME_FRAGMENT_CLASS*""".toRegex()),
    LINKEDIN("https://linkedin.com/", """https?://(?:www\.)?linkedin.com\b$NAME_FRAGMENT_CLASS*""".toRegex()),
    TWITTER("https://twitter.com/", """https?://(?:www\.)?twitter.com\b$NAME_FRAGMENT_CLASS*""".toRegex()),
    WEBSITE("https://", URL_VALIDATOR.value),
    XCOM("https://x.com/", """https?://(?:www\.)?x.com\b$NAME_FRAGMENT_CLASS*""".toRegex()),
    ;
}
