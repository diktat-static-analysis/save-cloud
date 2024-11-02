/**
 * File containing initialization of i18next
 */

package com.saveourtool.frontend.common.externals.i18next

/**
 * Function that encapsulates i18n initialization.
 *
 * @param isDebug flag to set debug mode
 * @param interpolationEscapeValue interpolation.escapeValue value
 */
@Suppress("UNUSED_PARAMETER")
fun initI18n(isDebug: Boolean = false, interpolationEscapeValue: Boolean = false) {
    js("""
        var i18n = require("i18next");
        var reactI18n = require("react-i18next");
        var Backend = require("i18next-http-backend");
        
        i18n
            .use(reactI18n.initReactI18next)
            .use(Backend.default)
            .init({
                load: 'languageOnly',
                initImmediate: false,
                partialBundledLanguages: true,
                ns: [
                    'cookies',
                    'topbar',
                    'organization',
                    'proposing',
                    'table-headers',
                    'thanks-for-registration',
                    'vulnerability-collection',
                    'vulnerability-upload',
                    'welcome',
                    'vulnerability',
                    'comments',
                    'dates',
                    'index',
                    'profile'
                ],
                backend: {
                    loadPath: '/locales/{{lng}}/{{ns}}.json'
                },
                lng: "en",
                fallbackLng: "en",
                debug: isDebug,
                interpolation: {
                    escapeValue: interpolationEscapeValue
                },
            }); 
    """)
}
