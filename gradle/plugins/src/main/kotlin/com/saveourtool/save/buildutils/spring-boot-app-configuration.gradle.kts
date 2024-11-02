/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import org.gradle.kotlin.dsl.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("com.saveourtool.save.buildutils.spring-boot-configuration")
    id("org.springframework.boot")
}

configure<SpringBootExtension> {
    // Same condition, as we use to determine whether images need to be published.
    // Essentially enables this setting for release builds only to spare time in dev builds.
    // Configures `bootBuildInfo` task, which creates META-INF/build-info.properties file,
    // which contains timestamps so its inputs change every single build.
    if (isRelease) {
        buildInfo()
    }
}

tasks.withType<BootRun>().configureEach {
    val profiles = buildString {
        append("dev")
        val os = DefaultNativePlatform.getCurrentOperatingSystem()
        when {
            os.isWindows -> append(",win")
            os.isMacOsX -> append(",mac")
        }
        if (listOf("save-backend", "cosv-backend").any { project.path.contains(it) }) {
            append(",secure")
        }
    }
    environment["SPRING_PROFILES_ACTIVE"] = profiles
}

tasks.named<BootBuildImage>("bootBuildImage") {
    commonConfigure()

    environment = mapOf(
        "BP_JVM_VERSION" to Versions.jdk,
        "BPE_DELIM_JAVA_TOOL_OPTIONS" to " ",
        "BPE_APPEND_JAVA_TOOL_OPTIONS" to
                // Workaround for https://github.com/reactor/reactor-netty/issues/564
                "-Dreactor.netty.pool.maxIdleTime=60000 -Dreactor.netty.pool.leasingStrategy=lifo " +
                        // Override default configuration. Intended to be used on a particular environment.
                        "-Dspring.config.additional-location=optional:file:/home/cnb/config/application.properties"
    )
}
