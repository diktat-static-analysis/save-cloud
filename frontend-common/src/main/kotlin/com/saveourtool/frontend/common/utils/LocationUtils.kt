@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.frontend.common.utils

import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.common.validation.SETTINGS
import remix.run.router.Location

/**
 * @param url url for comparison
 * @return true if [Location.pathname] is not [url], false otherwise
 */
fun Location<*>.not(url: String) = pathname != url

/**
 * @param urls list of urls
 * @return true of [Location] is not in [urls]
 */
fun Location<*>.notIn(urls: List<String>) = urls.all { not(it) }

/**
 * @param routes array of [FrontendRoutes]
 * @return true of [Location] is not in [FrontendRoutes.path] of [routes]
 */
fun Location<*>.notIn(routes: Array<FrontendRoutes>) = notIn(routes.map { "/$it" })

/**
 * @return true if [Location.pathname] starts with `/vuln`, false otherwise
 */
fun Location<*>.isVuln() = this.pathname.startsWith("/vuln")

/**
 * @return true if [Location.pathname] starts with `/settings`, false otherwise
 */
fun Location<*>.isSettings() = this.pathname.startsWith("/$SETTINGS")

/**
 * @return true if we are on a main (index) page
 */
fun Location<*>.isIndex() = pathname == "/"
