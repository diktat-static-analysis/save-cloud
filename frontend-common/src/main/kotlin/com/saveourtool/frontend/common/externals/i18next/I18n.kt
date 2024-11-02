package com.saveourtool.frontend.common.externals.i18next

import com.saveourtool.common.frontend.PlatformLanguages

/**
 * Class that represents i18n object
 */
external class I18n {
    /**
     * Current language
     */
    val language: String

    /**
     * Set language by language code
     *
     * @param language language code
     *
     * @see I18n.changeLanguage
     */
    fun changeLanguage(language: String)
}

/**
 * Get current [PlatformLanguages]
 *
 * @return current language as [PlatformLanguages] or [PlatformLanguages.defaultLanguage]
 */
fun I18n.language(): PlatformLanguages = PlatformLanguages.getByCodeOrDefault(language)
