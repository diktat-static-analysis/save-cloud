package com.saveourtool.cosv.backend

import com.saveourtool.common.s3.DefaultS3Configuration
import com.saveourtool.cosv.backend.configs.ConfigProperties

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

/**
 * An entrypoint for spring for cosv-backend
 */
@SpringBootApplication(scanBasePackages = [
    "com.saveourtool.common",
    "com.saveourtool.save",
    "com.saveourtool.cosv",
])
@EnableConfigurationProperties(ConfigProperties::class)
@Import(
    DefaultS3Configuration::class,
)
@EntityScan(basePackages = ["com.saveourtool.common.entities", "com.saveourtool.common.entitiescosv"])
class CosvApplication

fun main(args: Array<String>) {
    SpringApplication.run(CosvApplication::class.java, *args)
}
