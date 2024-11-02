package com.saveourtool.save.test.analysis.results

import com.saveourtool.common.test.analysis.results.supportAnalysisResult
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class AnalysisResultConfig {
    @Bean
    fun jackson2ObjectMapperBuilderCustomizer() = Jackson2ObjectMapperBuilderCustomizer { builder ->
        builder.supportAnalysisResult()
    }
}
