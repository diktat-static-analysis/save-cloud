/**
 * If you would like to add filtering to tables and have proper routing on react level to filtered views
 * than you can add extension methods to URLSearchParams and pass filter to particular view
 */

package com.saveourtool.frontend.common.utils

import com.saveourtool.common.filters.VulnerabilityFilter
import org.w3c.dom.url.URLSearchParams

/**
 * @return VulnerabilityFilter that can be passed to a table
 */
fun URLSearchParams.toVulnerabilitiesFilter(): VulnerabilityFilter {
    val tags = this.get("tag")?.let { setOf(it) } ?: emptySet()

    return VulnerabilityFilter(tags = tags)
}
