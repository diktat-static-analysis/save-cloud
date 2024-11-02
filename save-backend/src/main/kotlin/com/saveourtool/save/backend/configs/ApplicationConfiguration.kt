package com.saveourtool.save.backend.configs

import com.saveourtool.common.service.LogService
import com.saveourtool.common.service.LokiLogService
import org.springframework.boot.actuate.autoconfigure.metrics.orm.jpa.HibernateMetricsAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.web.reactive.config.EnableWebFlux

@Configuration
@EnableWebFlux
@EnableJpaRepositories(basePackages = ["com.saveourtool.save.backend.repository", "com.saveourtool.common.repository"])
@EntityScan("com.saveourtool.common.entities")
@ImportAutoConfiguration(HibernateMetricsAutoConfiguration::class)
@Suppress("MISSING_KDOC_TOP_LEVEL")
class ApplicationConfiguration {
    /**
     * @param configProperties
     * @return [LokiLogService] if [ConfigProperties.loki] is provided, otherwise [LogService.stub]
     */
    @Bean
    fun logService(configProperties: ConfigProperties): LogService = LokiLogService.createOrStub(configProperties.loki)
}
