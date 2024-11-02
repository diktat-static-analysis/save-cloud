/**
 * Utilities for spring WebClient
 */

package com.saveourtool.common.spring.utils

import com.saveourtool.common.utils.debug
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.web.reactive.function.client.WebClient

private val logger = LoggerFactory.getLogger("com.saveourtool.common.spring.utils.WebClientUtils")

/**
 * Applies all [WebClientCustomizer]s from [customizers] to [this] [WebClient.Builder].
 *
 * @param customizers [WebClientCustomizer]s to be applied
 * @return the modified builder
 */
fun WebClient.Builder.applyAll(customizers: Iterable<WebClientCustomizer>): WebClient.Builder = apply { builder ->
    customizers.forEach { customizer ->
        logger.debug { "Applying a WebClientCustomizer of type ${customizer::class.qualifiedName}" }
        customizer.customize(builder)
    }
}
