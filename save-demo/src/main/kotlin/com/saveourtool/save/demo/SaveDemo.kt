package com.saveourtool.save.demo

import com.saveourtool.common.s3.DefaultS3Configuration
import com.saveourtool.save.demo.config.ConfigProperties

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

/**
 * An entrypoint for spring boot for save-demo
 */
@EnableConfigurationProperties(ConfigProperties::class)
@Import(DefaultS3Configuration::class)
@SpringBootApplication
class SaveDemo

fun main(args: Array<String>) {
    SpringApplication.run(SaveDemo::class.java, *args)
}
