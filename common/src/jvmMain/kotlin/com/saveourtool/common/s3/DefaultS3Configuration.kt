package com.saveourtool.common.s3

import org.springframework.context.annotation.Bean

/**
 * Configuration for S3
 */
open class DefaultS3Configuration(
    private val s3OperationsPropertiesProvider: S3OperationsProperties.Provider,
) {
    /**
     * @return [S3Operations] as a Spring's bean
     */
    @Bean
    open fun s3Operations(): S3Operations = DefaultS3Operations(s3OperationsPropertiesProvider.s3Storage)
}
