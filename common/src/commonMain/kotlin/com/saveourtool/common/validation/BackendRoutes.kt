/**
 * Names that are used as endpoints in the frontend.
 * If you create a new view with new URL - add it here.
 */

package com.saveourtool.common.validation

import com.saveourtool.common.utils.URL_PATH_DELIMITER
import kotlin.js.JsExport

/**
 * It is required to forbid creating organizations and users with such names
 *
 * @property path substring of url that defines given route
 */
@JsExport
@Suppress("unused", "WRONG_DECLARATIONS_ORDER")
enum class BackendRoutes(val path: String) {
    API("api"),
    INTERNAL("internal"),
    ACTUATOR("actuator"),
    GRAFANA("grafana"),
    ERROR("error"),
    LOGIN("login"),
    LOGOUT("logout"),
    SEC("sec"),
    NEO4J("neo4j"),
    ;

    override fun toString(): String = path

    companion object {
        /**
         * Get forbidden words from [BackendRoutes].
         *
         * @return list of forbidden words
         */
        fun getForbiddenWords() = BackendRoutes.values()
            .map { it.path.split(URL_PATH_DELIMITER) }
            .flatten()
            .toTypedArray()
    }
}
