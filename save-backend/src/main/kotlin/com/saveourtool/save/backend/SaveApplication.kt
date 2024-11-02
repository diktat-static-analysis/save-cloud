package com.saveourtool.save.backend

import com.saveourtool.common.s3.DefaultS3Configuration
import com.saveourtool.save.backend.configs.ConfigProperties

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

/**
 * An entrypoint for spring for save-backend
 */
@SpringBootApplication(scanBasePackages = [
    "com.saveourtool.common.configs",
    "com.saveourtool.save.backend",
    "com.saveourtool.common.service",
    "com.saveourtool.common.storage",
    "com.saveourtool.common.security",
    "com.saveourtool.common.utils",
    "com.saveourtool.common.repository",
])
@EnableConfigurationProperties(ConfigProperties::class)
@Import(
    DefaultS3Configuration::class,
)
class SaveApplication

fun main(args: Array<String>) {
    SpringApplication.run(SaveApplication::class.java, *args)
}
