package com.saveourtool.common.service

/**
 * @property url url to loki service
 * @property labels labels to query loki service
 */
data class LokiConfig(
    val url: String,
    val labels: LabelsConfig,
) {
    /**
     * @property agentContainerName label to get logs by agent container name
     * @property applicationName label to get logs by application name
     */
    data class LabelsConfig(
        val agentContainerName: String,
        val applicationName: String? = null,
    )
}
