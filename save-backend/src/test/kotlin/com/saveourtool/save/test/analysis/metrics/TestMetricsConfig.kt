package com.saveourtool.save.test.analysis.metrics

import com.saveourtool.common.test.analysis.metrics.supportTestMetrics
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestMetricsConfig {
    @Bean
    fun jackson2ObjectMapperBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
            Jackson2ObjectMapperBuilderCustomizer { builder ->
                builder.supportTestMetrics()
            }
}
