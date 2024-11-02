/**
 * save-demo configuration
 */

package com.saveourtool.save.demo.config

import com.saveourtool.common.s3.S3OperationsProperties
import io.fabric8.kubernetes.api.model.Quantity
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * @property backendUrl URL of backend
 * @property agentConfig configuration of save-demo-agents that are run by save-demo
 * @property s3Storage configuration of S3 storage
 * @property kubernetes kubernetes configuration
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "demo")
data class ConfigProperties(
    val backendUrl: String,
    override val s3Storage: S3OperationsProperties,
    val kubernetes: KubernetesConfig?,
    val agentConfig: AgentConfig? = null,
) : S3OperationsProperties.Provider {
    /**
     * @property demoUrl url of save-demo
     */
    data class AgentConfig(
        val demoUrl: String
    )
}

/**
 * @property apiServerUrl URL of Kubernetes API Server. See [docs on accessing API from within a pod](https://kubernetes.io/docs/tasks/run-application/access-api-from-pod/)
 * @property serviceAccount Name of [ServiceAccount](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/) that will be used
 * to authenticate save-demo to the API server
 * @property currentNamespace namespace that demo works in
 * @property useGvisor if true, will try to use gVisor's runsc runtime for starting agents
 * @property agentSubdomainName name of service that is created in order to access agents
 * @property agentPort port of agent that should be used to access it
 * @property agentNamespace namespace that demo-agents should work in, [currentNamespace] by default
 * @property agentCpuLimitations configures CPU [Limitations] for demo-agent pods
 * @property agentMemoryLimitations configures memory [Limitations] for demo-agent pods
 * @property agentEphemeralStorageLimitations configures ephemeral storage [Limitations] for demo-agent pods
 * @property podBackoffLimit number of attempts to restart the pod before it is considered to be failed, [DEFAULT_BACKOFF_LIMIT] by default
 */
data class KubernetesConfig(
    val apiServerUrl: String,
    val serviceAccount: String,
    val currentNamespace: String,
    val useGvisor: Boolean,
    val agentSubdomainName: String,
    val agentPort: Int,
    val agentNamespace: String = currentNamespace,
    val agentCpuLimitations: Limitations? = defaultAgentCpuLimitations,
    val agentMemoryLimitations: Limitations? = defaultAgentMemoryLimitations,
    val agentEphemeralStorageLimitations: Limitations? = defaultEphemeralStorageLimitations,
    val podBackoffLimit: Int = DEFAULT_BACKOFF_LIMIT,
) {
    /**
     * Data class that configures demo-agent limitations:
     *
     * `m` stands for milli, `M` stands for Mega.
     *
     * By default, `M` and `m` are powers of 10.
     * To be more accurate and use `M` as 1024 instead of 1000, `i` should be provided: `Mi`
     * [reference](https://kubernetes.io/docs/reference/kubernetes-api/common-definitions/quantity/)
     *
     * * CPU is measured in units. This means that `500m` of CPU equals to half of a unit.
     * * Ephemeral storage and memory is measured in bytes.
     *
     * @property requests configures the amount of resources that must be assigned to pod (`resources.requests.<RESOURCE_NAME>`)
     * @property limits configures the maximum amount of resources that might be assigned to pod (`resources.limits.<RESOURCE_NAME>`)
     */
    data class Limitations(
        val requests: String,
        val limits: String,
    ) {
        /**
         * @return [requests] as [Quantity]
         */
        fun requestsQuantity() = Quantity(requests)

        /**
         * @return [limits] as [Quantity]
         */
        fun limitsQuantity() = Quantity(limits)
    }
    companion object {
        private const val DEFAULT_BACKOFF_LIMIT = 6
        private val defaultAgentCpuLimitations = Limitations("200m", "2")
        private val defaultAgentMemoryLimitations = Limitations("300Mi", "500Mi")
        private val defaultEphemeralStorageLimitations = Limitations("100Mi", "500Mi")
    }
}
