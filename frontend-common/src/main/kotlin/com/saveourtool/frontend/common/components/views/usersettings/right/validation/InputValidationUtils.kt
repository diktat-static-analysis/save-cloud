/**
 * Utility file with validation functions that should be passed to inputForm
 */

package com.saveourtool.frontend.common.components.views.usersettings.right.validation

import com.saveourtool.common.validation.*
import com.saveourtool.frontend.common.utils.UsefulUrls

private val namingAllowedSymbols = setOf('-', '_', '.', ' ')
private val extendedNamingAllowedSymbols = namingAllowedSymbols + setOf(',', '\'')

/**
 * @return validation in inputField
 */
fun String.validateLogin(): String = if (isValidName(NAMING_MAX_LENGTH)) "" else NAME_ERROR_MESSAGE

/**
 * @return validation in inputField
 */
fun String.validateUserEmail(): String = if (isValidEmail()) "" else EMAIL_ERROR_MESSAGE

/**
 * @return validation in inputField
 */
fun String.validateRealName(): String =
        if (!isValidName(NAMING_ALLOWED_LENGTH, namingAllowedSymbols)) {
            "Name should contain only English letters and be less than $NAMING_ALLOWED_LENGTH symbols"
        } else {
            ""
        }

/**
 * @return validation in inputField
 */
fun String.validateCompany(): String =
        if (!isValidName(NAMING_ALLOWED_LENGTH, extendedNamingAllowedSymbols)) {
            "Affiliation should contain only English letters and be less than $NAMING_ALLOWED_LENGTH symbols"
        } else {
            ""
        }

/**
 * @return validation in inputField
 */
fun String.validateLocation(): String =
        if (!isValidName(NAMING_ALLOWED_LENGTH, extendedNamingAllowedSymbols)) {
            "Location should contain only English letters and be less than $NAMING_ALLOWED_LENGTH symbols"
        } else {
            ""
        }

/**
 * @return validation in inputField
 */
fun String.validateWebsite(): String =
        when {
            this == "" -> ""
            this.matches(UsefulUrls.WEBSITE.regex) && isValidLengthWebsite() -> ""
            else -> "Url should start with ${UsefulUrls.WEBSITE.basicUrl} and be less than $WEBSITE_ALLOWED_LENGTH symbols"
        }

/**
 * @return validation in inputField
 */
fun String.validateLinkedIn(): String =
        when {
            this == "" -> ""
            this.matches(UsefulUrls.LINKEDIN.regex) -> ""
            else -> "Url should start with ${UsefulUrls.LINKEDIN.basicUrl}"
        }

/**
 * @return validation in inputField
 */
fun String.validateGithub(): String =
        when {
            this == "" -> ""
            this.matches(UsefulUrls.GITHUB.regex) || this.matches(UsefulUrls.GITEE.regex) -> ""
            else -> "Url should start with ${UsefulUrls.GITEE.basicUrl} or ${UsefulUrls.GITHUB.basicUrl}"
        }

/**
 * @return validation in inputField
 */
fun String.validateTwitter(): String =
        when {
            this == "" -> ""
            this.matches(UsefulUrls.XCOM.regex) || this.matches(UsefulUrls.TWITTER.regex) -> ""
            else -> "Url should start with ${UsefulUrls.XCOM.basicUrl} or ${UsefulUrls.TWITTER.basicUrl}"
        }
