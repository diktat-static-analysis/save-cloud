@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.frontend.common.externals.cookie

import js.core.jso
import kotlinext.js.require

/**
 * Object that manages cookies
 */
val cookie: Cookie = require("js-cookie")

/**
 * Interface that encapsulates all cookies interactions
 */
external interface Cookie {
    /**
     * Get cookie by [key]
     *
     * @param key key to get cookie
     * @param cookieAttribute [CookieAttribute]
     * @return cookie as [String]
     */
    fun get(key: String, cookieAttribute: CookieAttribute = definedExternally): String

    /**
     * Get all cookies
     *
     * @return [Set] of cookies as [String]s
     */
    fun get(): Set<String>

    /**
     * Set cookie
     *
     * @param key key to set cookie
     * @param value cookie value as [String]
     * @param cookieAttribute [CookieAttribute]
     */
    fun set(key: String, value: String, cookieAttribute: CookieAttribute = definedExternally)

    /**
     * Remove cookie
     *
     * @param key cookie key
     * @param cookieAttribute [CookieAttribute]
     */
    fun remove(key: String, cookieAttribute: CookieAttribute = definedExternally)
}

/**
 * Cookie attributes that can be passed in [Cookie.remove], [Cookie.set] or [Cookie.get] methods
 *
 * @see <a href=https://github.com/js-cookie/js-cookie>Documentation on GitHub</a>
 */
external interface CookieAttribute {
    /**
     * A [String] indicating the path where the cookie is visible.
     */
    var path: String?

    /**
     * A [String] indicating a valid domain where the cookie should be visible.
     * The cookie will also be visible to all subdomains.
     */
    var domain: String?

    /**
     * Define when the cookie will be removed.
     * Value must be an [Int] which will be interpreted as days from time of creation.
     *
     * If omitted, the cookie becomes a session cookie.
     */
    var expires: Int?

    /**
     * Either true or false, indicating if the cookie transmission requires a secure protocol (https).
     */
    var secure: Boolean?
}

/**
 * Class that encapsulates the cookie information
 *
 * @property key [String] cookie name
 * @property expires amount of days before a cookie is considered to be expired, [DEFAULT_EXPIRES] by default
 */
sealed class CookieKeys(val key: String, val expires: Int = DEFAULT_EXPIRES) {
    /**
     * Cookie that indicates that user as accepted cookie policy
     */
    object IsCookieOk : CookieKeys("isCookieOk")

    /**
     * Cookie that stores preferred platform language
     */
    object PreferredLanguage : CookieKeys("language")
    companion object {
        /**
         * Default value for [CookieKeys.expires]
         */
        const val DEFAULT_EXPIRES = 365
    }
}

/**
 * @param key key as [CookieKeys]
 * @param value value to set
 * @see Cookie.set
 */
fun Cookie.set(key: CookieKeys, value: String) = set(key.key, value, jso { expires = key.expires })

/**
 * @param key key as [CookieKeys]
 * @return cookie as [String] by [CookieKeys.key] of [key]
 * @see Cookie.get
 */
fun Cookie.get(key: CookieKeys): String = get(key.key)

/**
 * @param key key as [CookieKeys]
 * @see Cookie.remove
 */
fun Cookie.remove(key: CookieKeys) = remove(key.key)

/**
 * Check if cookies are accepted by user
 *
 * @return true if cookies are accepted, false otherwise
 */
fun Cookie.isAccepted() = get(CookieKeys.IsCookieOk) == "true"

/**
 * Accept cookies
 *
 * @return [Unit]
 */
fun Cookie.acceptCookies() = set(CookieKeys.IsCookieOk, "true")

/**
 * Decline cookies and delete existed once
 *
 * @return [Unit]
 */
fun Cookie.declineCookies() = remove(CookieKeys.IsCookieOk).also {
    remove(CookieKeys.PreferredLanguage)
}

/**
 * Get preferred platform language code
 *
 * @return preferred platform language code as [String]
 */
fun Cookie.getLanguageCode() = get(CookieKeys.PreferredLanguage)

/**
 * Save preferred platform language code
 *
 * @param languageCode preferred platform language code as [String]
 * @return [Unit]
 */
fun Cookie.saveLanguageCode(languageCode: String) = set(CookieKeys.PreferredLanguage, languageCode)
