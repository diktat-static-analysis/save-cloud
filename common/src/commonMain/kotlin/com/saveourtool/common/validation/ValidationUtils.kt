/**
 * Module that implements different types of validation.
 */

package com.saveourtool.common.validation

/**
 * Default amount of characters allowed for names
 */
const val WEBSITE_ALLOWED_LENGTH = 64
const val NAMING_ALLOWED_LENGTH = 64
const val NAMING_MAX_LENGTH = 22
private val namingAllowedSpecialSymbols = setOf('-', '_', '.')

@Suppress("MagicNumber")
private val tagLengthRange = 2..15

/**
 * Check if name is valid.
 *
 * @param allowedLength maximum allowed number of characters, default [NAMING_ALLOWED_LENGTH]
 * @param allowedSpecialSymbols allowed set of special symbols
 * @return true if name is valid, false otherwise
 */
fun String.isValidName(
    allowedLength: Int = NAMING_ALLOWED_LENGTH,
    allowedSpecialSymbols: Set<Char> = namingAllowedSpecialSymbols
) = run {
    isNotBlank() && setOf(first(), last()).none { it in allowedSpecialSymbols } &&
            hasOnlyAlphaNumOrAllowedSpecialSymbols(allowedSpecialSymbols) && areAllLettersEnglish() && !containsForbiddenWords() && isLengthOk(allowedLength)
}

/**
 * Check if path is valid.
 *
 * @param isRelative if true, check is done for relative paths, otherwise checking absolute paths
 * @return true if path is valid, false otherwise
 */
fun String.isValidPath(isRelative: Boolean = true) = run {
    isNotBlank() && if (isRelative) {
        first() != '/' && ValidationRegularExpressions.RELATIVE_PATH_VALIDATOR.value.matches(this)
    } else {
        first() == '/' && ValidationRegularExpressions.ABSOLUTE_PATH_VALIDATOR.value.matches(this)
    }
}

/**
 * Check if url is valid.
 *
 * @return true if url is valid, false otherwise
 */
fun String?.isValidUrl() = this?.let { ValidationRegularExpressions.URL_VALIDATOR.value.matches(it) } ?: false

/**
 * Check if email is valid.
 *
 * @return true if email is valid, false otherwise
 */
fun String.isValidEmail() = ValidationRegularExpressions.EMAIL_VALIDATOR.value.matches(this)

/**
 * Check if length of name is valid.
 *
 * @return true if length name less than [NAMING_MAX_LENGTH], false otherwise
 */
fun String.isValidLengthName() = isLengthOk(NAMING_MAX_LENGTH)

/**
 * Check if length of website is valid.
 *
 * @return true if length website less than [WEBSITE_ALLOWED_LENGTH], false otherwise
 */
fun String.isValidLengthWebsite() = isLengthOk(WEBSITE_ALLOWED_LENGTH)

/**
 * Check that the field is less than [NAMING_ALLOWED_LENGTH] symbols
 *
 * @return false if the length is more than [NAMING_ALLOWED_LENGTH]
 */
fun String.isValidMaxAllowedLength() = isLengthOk(NAMING_ALLOWED_LENGTH)

/**
 * Check if tag length is valid and tag does not contain commas (`,`).
 *
 * @return true if [String.length] is in [tagLengthRange] and [String] does not contain commas (`,`), false otherwise
 */
fun String.isValidTag() = length in tagLengthRange && !contains(",") && isNotBlank()

/**
 * Check if each of letter in string is English
 *
 * @return true if all letters are English, false otherwise
 */
fun String.areAllLettersEnglish(): Boolean = this.filter { it.isLetter() }.all {
    (it.lowercaseChar() >= 'a') && (it.lowercaseChar() <= 'z')
}

private fun String.hasOnlyAlphaNumOrAllowedSpecialSymbols(
    allowedSpecialSymbols: Set<Char> = namingAllowedSpecialSymbols
) = all { it.isLetterOrDigit() || allowedSpecialSymbols.contains(it) }

private fun String.containsForbiddenWords() = (FrontendRoutes.getForbiddenWords() + FrontendCosvRoutes.getForbiddenWords() + BackendRoutes.getForbiddenWords())
    .any { this == it }

private fun String.isLengthOk(allowedLength: Int) = length <= allowedLength
