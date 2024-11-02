@file:Suppress(
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION",
)

package com.saveourtool.common.configs

import com.saveourtool.common.domain.supportTestStatus
import com.saveourtool.common.test.analysis.metrics.supportTestMetrics
import com.saveourtool.common.test.analysis.results.supportAnalysisResult
import com.saveourtool.common.utils.supportKLocalDateTime

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
open class WebConfig {
    @Bean
    open fun jackson2ObjectMapperBuilderCustomizer() = Jackson2ObjectMapperBuilderCustomizer { builder: Jackson2ObjectMapperBuilder ->
        builder
            .supportTestStatus()
            .supportKLocalDateTime()
            .supportTestMetrics()
            .supportAnalysisResult()
    }

    @Bean
    open fun jackson2JsonEncoder(mapper: ObjectMapper) = Jackson2JsonEncoder(mapper)

    @Bean
    open fun jackson2JsonDecoder(mapper: ObjectMapper) = Jackson2JsonDecoder(mapper)

    @Bean
    open fun webFluxConfigurer(encoder: Jackson2JsonEncoder, decoder: Jackson2JsonDecoder) =
            object : WebFluxConfigurer {
                override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
                    configurer.defaultCodecs().jackson2JsonEncoder(encoder)
                    configurer.defaultCodecs().jackson2JsonDecoder(decoder)
                }
            }

    @Bean
    open fun jackson2WebClientCustomizer(jackson2JsonEncoder: Jackson2JsonEncoder, jackson2JsonDecoder: Jackson2JsonDecoder): WebClientCustomizer = WebClientCustomizer { builder ->
        builder.codecs {
            it.defaultCodecs().jackson2JsonEncoder(jackson2JsonEncoder)
            it.defaultCodecs().jackson2JsonDecoder(jackson2JsonDecoder)
        }
    }
}
