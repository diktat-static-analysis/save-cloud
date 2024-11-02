package com.saveourtool.common.filters

import com.saveourtool.common.demo.DemoStatus
import kotlinx.serialization.Serializable

/**
 * @property organizationName substring that should match saveourtool organization name
 * @property projectName substring that should match saveourtool project name
 * @property statuses
 */
@Serializable
data class DemoFilter(
    val organizationName: String,
    val projectName: String,
    val statuses: Set<DemoStatus>
) {
    companion object {
        /**
         * The filter which returns all the demos
         */
        val any = DemoFilter(
            organizationName = "",
            projectName = "",
            statuses = DemoStatus.values().filter { it != DemoStatus.NOT_CREATED }.toSet()
        )

        /**
         * The filter which returns all active demos
         */
        val running = DemoFilter(
            organizationName = "",
            projectName = "",
            statuses = setOf(DemoStatus.RUNNING)
        )
    }
}
