package com.saveourtool.save.demo.config

import com.saveourtool.common.utils.BlockingBridge
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Class for [KubernetesClient] bean initialization
 */
@Configuration
class Beans {
    /**
     * @param configProperties application configuration
     * @return configured [KubernetesClient]
     */
    @Bean(destroyMethod = "close")
    @Profile("kubernetes | minikube")
    fun kubernetesClient(configProperties: ConfigProperties): KubernetesClient {
        val kubernetesSettings = requireNotNull(configProperties.kubernetes) {
            "Kubernetes settings should be passed in order to use Kubernetes"
        }
        return KubernetesClientBuilder()
            .withConfig(ConfigBuilder()
                .withNamespace(kubernetesSettings.currentNamespace)
                .build())
            .build()
    }

    /**
     * @return [BlockingBridge]
     */
    @Bean
    fun blockingBridge(): BlockingBridge = BlockingBridge.default
}
