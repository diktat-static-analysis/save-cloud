/**
 * Names that are used as endpoints in the frontend.
 * If you create a new view with new URL - add it here.
 */

package com.saveourtool.common.validation

import com.saveourtool.common.utils.URL_PATH_DELIMITER
import kotlin.js.JsExport

/**
 * @property path substring of url that defines given route
 */
@JsExport
enum class FrontendCosvRoutes(val path: String) {
    ABOUT_US("about"),
    BAN("ban"),
    COOKIE("cookie"),
    CREATE_ORGANIZATION("create-organization"),
    ERROR_404("404"),
    MANAGE_ORGANIZATIONS("organizations"),
    NOT_FOUND("not-found"),
    PROFILE("profile"),
    REGISTRATION("registration"),
    SETTINGS_DELETE("$SETTINGS/delete"),
    SETTINGS_EMAIL("$SETTINGS/email"),
    SETTINGS_ORGANIZATIONS("$SETTINGS/organizations"),
    SETTINGS_PROFILE("$SETTINGS/profile"),
    SETTINGS_TOKEN("$SETTINGS/token"),
    TERMS_OF_USE("terms-of-use"),
    THANKS_FOR_REGISTRATION("thanks-for-registration"),
    VULN("vuln"),
    VULNERABILITIES("$VULN/list"),
    VULNERABILITY_SINGLE("$VULN/collection"),
    VULN_COSV_SCHEMA("$VULN/schema"),
    VULN_CREATE("$VULN/create-vulnerability"),
    VULN_TOP_RATING("$VULN/top-rating"),
    VULN_UPLOAD("$VULN/upload-vulnerability"),
    ;

    override fun toString(): String = path

    companion object {
        /**
         * List of views on which topbar should not be rendered
         */
        val noTopBarViewList = arrayOf(
            REGISTRATION,
            ERROR_404,
            TERMS_OF_USE,
            THANKS_FOR_REGISTRATION,
        )

        /**
         * Get forbidden words from [FrontendCosvRoutes].
         *
         * @return list of forbidden words
         */
        fun getForbiddenWords() = FrontendCosvRoutes.values()
            .map { it.path.split(URL_PATH_DELIMITER) }
            .flatten()
            .toTypedArray()
    }
}
