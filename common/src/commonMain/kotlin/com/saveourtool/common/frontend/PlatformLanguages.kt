package com.saveourtool.common.frontend

import io.ktor.http.*

/**
 * Enum that contains all supported saveourtool languages
 *
 * @property code language code
 * @property value language name
 * @property label language label
 */
enum class PlatformLanguages(val code: String, val value: String, val label: String) {
    /**
     * Chinese
     */
    CN("cn", "Chinese", "中文"),

    /**
     * English
     */
    EN("en", "English", "EN"),

    /**
     * Russian
     */
    RU("ru", "Russian", "РУ"),
    ;
    companion object {
        /**
         * Default platform language
         */
        val defaultLanguage = EN

        /**
         * @param code language code
         * @return [PlatformLanguages] enum entity corresponding to language [code] or [defaultLanguage]
         */
        fun getByCodeOrDefault(code: String) = values().find { it.code == code } ?: defaultLanguage
    }
}
